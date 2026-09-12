# Arquitectura y convenciones Kotlin

Se carga al trabajar con cualquier archivo Kotlin.

## Estructura

Un solo módulo Gradle, `:app`, con paquetes por característica.

```
app/src/main/java/bo/saludencasa/
├── core/
│   ├── error/            Tipos de falla comunes
│   ├── network/          Configuración del cliente de Supabase
│   └── util/             Utilidades transversales — el único lugar para ellas
├── di/                   Módulos de Koin, uno por característica
├── navigation/           Grafo de navegación y rutas tipadas
├── ui/
│   ├── theme/            Color, tipografía, formas
│   ├── components/       Componentes reutilizables
│   └── animations/       Transiciones compartidas
└── features/<feature>/
    ├── data/
    │   ├── datasource/   Supabase<Entity>DataSource
    │   ├── model/        <Entity>Dto — solo transporte
    │   ├── mapper/       Dto <-> modelo de dominio
    │   └── repository/   <Entity>Repository — implementación
    ├── domain/
    │   ├── model/        Entidades puras y <Entity>Result
    │   ├── repository/   I<Entity>Repository — interfaz
    │   ├── usecase/      <Verb><Noun>UseCase
    │   └── vo/           Objetos de valor
    └── presentation/     Pantallas Compose y modelos de vista
```

Las pruebas replican esta estructura bajo `app/src/test/java/` y
`app/src/androidTest/java/`.

## Nomenclatura

| Elemento | Convención | Ejemplo |
|---|---|---|
| Interfaz de repositorio | `I<Entity>Repository` en `domain/repository/` | `IRequestRepository` |
| Implementación | `<Entity>Repository` en `data/repository/` | `RequestRepository` |
| Fuente de datos remota | `Supabase<Entity>DataSource` | `SupabaseRequestDataSource` |
| Objeto de transporte | `<Entity>Dto` | `ServiceRequestDto` |
| Caso de uso | `<Verb><Noun>UseCase` | `CreateServiceRequestUseCase` |
| Resultado | `<Entity>Result`, jerarquía sellada | `RequestResult` |
| Tipo de error | `<Entity>Error`, jerarquía sellada | `RequestError` |
| Modelo de vista | `<Screen>ViewModel` | `OfferNegotiationViewModel` |
| Estado de interfaz | `<Screen>UiState` | `RequestListUiState` |
| Objeto de valor | Nombre del concepto | `AmountBob`, `Coordinate` |
| Módulo de inyección | `<feature>Module` | `requestModule` |

## Regla de dependencia

**No hay compilador que la haga cumplir.** El proyecto es un solo módulo, así que
técnicamente cualquier clase puede importar cualquier otra. La separación se
sostiene por convención, por revisión y por análisis estático.

Las reglas, en orden de importancia:

1. **`domain` no importa nada de plataforma.** Ningún archivo bajo `domain/`
   importa `androidx.*`, `android.*` ni `io.github.jan.supabase.*`. Es código
   Kotlin puro, y debe poder probarse sin emulador.
2. **`presentation` no importa `data`.** Consume casos de uso, nunca repositorios
   ni fuentes de datos.
3. **Una característica no importa la capa `data` de otra.** Si necesita algo, lo
   pide por el caso de uso o la interfaz de repositorio de esa característica.
4. **El modelo de vista no inyecta repositorios.** Solo casos de uso.

Al abrir un archivo bajo `domain/`, revisar sus importaciones antes de agregar
nada. Es el punto donde la arquitectura se degrada primero.

## Caso de uso

Una operación del negocio por clase. Valida sus precondiciones, ejecuta y
devuelve un resultado explícito.

```kotlin
class AcceptOfferUseCase(
    private val offerRepository: IOfferRepository,
    private val requestRepository: IRequestRepository,
) {
    suspend operator fun invoke(offerId: String): OfferResult {
        // 1. validar precondiciones
        // 2. consultar el estado necesario
        // 3. verificar reglas de negocio
        // 4. delegar la persistencia al repositorio
    }
}
```

- Firma siempre `suspend operator fun invoke(...)`.
- Si devuelve un flujo, es `operator fun invoke(...): Flow<T>` sin `suspend`.
- Sin dependencias de Android. Debe probarse con JUnit, sin emulador.
- La regla de negocio vive aquí, nunca en el modelo de vista ni en la pantalla.

## Resultados y errores

Condiciones esperadas como jerarquía sellada, no como excepción.

```kotlin
sealed interface RequestResult {
    data class Success(val request: ServiceRequest) : RequestResult
    data class SuccessList(val requests: List<ServiceRequest>) : RequestResult
    data class Failure(val error: RequestError) : RequestResult
}

sealed interface RequestError {
    data object PatientHasNoPrimaryAddress : RequestError
    data object ProfessionalNotVerified : RequestError
    data class AmountOutOfRange(val min: Int, val max: Int) : RequestError
}
```

**El error transporta un tipo, nunca una frase.** La capa de presentación traduce
ese tipo a una clave de recurso. Un texto en la capa de dominio queda atrapado
ahí, y la misma condición puede necesitar presentarse distinto en cada pantalla.

Las excepciones se reservan para fallos verdaderamente inesperados. El compilador
exige cubrir todas las variantes en cada `when` exhaustivo.

## Objetos de valor

Todo dato con una regla de validación se modela así. Un dato inválido no puede
existir como instancia del tipo.

```kotlin
@JvmInline
value class AmountBob private constructor(val value: BigDecimal) {
    companion object {
        fun create(raw: BigDecimal): Result<AmountBob> = when {
            raw <= BigDecimal.ZERO -> Result.failure(
                IllegalArgumentException("amount_must_be_positive")
            )
            raw.scale() > 2 -> Result.failure(
                IllegalArgumentException("amount_max_two_decimals")
            )
            else -> Result.success(AmountBob(raw))
        }
    }
}
```

El mensaje de la excepción es una **clave**, no una frase para el usuario.

Objetos de valor previstos: `Email`, `PhoneNumber`, `PersonName`, `AmountBob`,
`Rating`, `Coordinate`, `CoverageRadiusKm`.

Cada uno lleva su prueba unitaria con los casos límite.

## Estados

Modelar con jerarquía sellada, no con enumerado más campos nulos. Así el
compilador obliga a cubrir cada caso y las transiciones inválidas se vuelven
errores de compilación.

```kotlin
sealed interface RequestState {
    data object Published : RequestState
    data class Negotiating(val offers: List<Offer>) : RequestState
    data class Accepted(val offer: Offer, val serviceId: String) : RequestState
    data class InProgress(val startedAt: Instant) : RequestState
    data class Completed(val finishedAt: Instant) : RequestState
    data class Cancelled(val reason: String) : RequestState
    data object Expired : RequestState
}
```

## Inyección de dependencias

Un módulo de Koin por característica, en `di/`.

| Componente | Alcance | Motivo |
|---|---|---|
| Fuente de datos | `single` | Mantiene conexión |
| Repositorio | `single` | Mantiene estado compartido |
| Caso de uso | `factory` | Sin estado |
| Modelo de vista | `viewModel` | Ligado al ciclo de vida |

La declaración expresa la inversión de dependencias de forma literal:

```kotlin
single<IRequestRepository> { RequestRepository(get()) }
```

Inyección por constructor únicamente. Nunca inyección en campos.

## Asincronía

- `suspend` para operaciones puntuales.
- `Flow` para secuencias que cambian en el tiempo, como las suscripciones de
  tiempo real.
- Nunca bloquear. Nunca `runBlocking` fuera de pruebas.
- El repositorio expone `Flow`; el modelo de vista lo convierte en `StateFlow`
  con `stateIn` y un alcance de suscripción acotado.

## Persistencia local

**No hay base de datos local en esta fase.** Ningún requisito exige operación sin
conexión. `DataStore` guarda la sesión y las preferencias del usuario; nada más.

Si una historia futura requiere caché o funcionamiento sin conexión, primero se
agrega el requisito a `docs/requirements.md` y luego se implementa. No al revés.

## Lo que no se hace

- Agregar una biblioteca sin un requisito que la exija.
- Abstracción especulativa. Sin interfaces con una sola implementación que nadie
  va a reemplazar.
- Programación defensiva para casos imposibles.
- Funcionalidad que nadie pidió.
- Dependencias en versión `alpha`, `beta`, `rc` o `SNAPSHOT`.
- Comentarios que repiten lo que dice el código. Solo se comenta el porqué de una
  decisión no evidente.
- Emojis en ninguna parte.

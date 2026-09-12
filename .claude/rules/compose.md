# Convenciones de interfaz

Se carga al trabajar con pantallas y componentes Compose.

## Estructura de una pantalla

Cada pantalla se divide en dos funciones. La primera se conecta al modelo de
vista; la segunda recibe estado y devuelve eventos, y es la que se previsualiza y
se prueba.

```kotlin
@Composable
fun RequestListScreen(
    viewModel: RequestListViewModel = koinViewModel(),
    onRequestClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RequestListContent(
        uiState = uiState,
        onRequestClick = onRequestClick,
        onRefresh = viewModel::refresh,
    )
}

@Composable
private fun RequestListContent(
    uiState: RequestListUiState,
    onRequestClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) { /* ... */ }
```

Reglas:

- La función de contenido **no** conoce el modelo de vista.
- El parámetro `modifier` va siempre, con valor por omisión, y en la primera
  posición de los parámetros opcionales.
- Los eventos suben como lambdas. El estado baja como parámetro.

## Estado de interfaz

Una jerarquía sellada por pantalla, no un objeto con banderas nulas.

```kotlin
sealed interface RequestListUiState {
    data object Loading : RequestListUiState
    data object Empty : RequestListUiState
    data class Content(val requests: List<RequestItem>) : RequestListUiState
    data class Error(val message: String) : RequestListUiState
}
```

Toda pantalla que carga datos contempla los cuatro estados. El estado vacío no es
opcional: es lo que ve el usuario la primera vez que entra, y una pantalla en
blanco no comunica nada.

## Modelo de vista

- Expone `StateFlow`, nunca `MutableStateFlow` público.
- Consume casos de uso, **nunca** repositorios ni fuentes de datos.
- No contiene reglas de negocio. Transforma el resultado del caso de uso en
  estado de interfaz.
- Usa `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), inicial)`
  para los flujos, de modo que la suscripción se cierre al dejar la pantalla.

## Navegación

- Rutas tipadas, nunca cadenas concatenadas a mano.
- El grafo vive en `navigation/`. Las pantallas no conocen el grafo: reciben
  lambdas de navegación.
- Los argumentos son identificadores, nunca objetos serializados.

## Sistema de diseño

La especificación completa —paleta, tipografía, formas, espaciado y componentes—
está en `docs/design-system.md`, derivada de las referencias visuales de
`docs/design/references/`. Consúltala antes de diseñar una pantalla nueva.

Lo esencial que no se negocia:

- Todo color proviene del esquema del tema. **Ningún valor de color escrito
  directamente en una pantalla.**
- Toda tipografía proviene de la escala del tema. Tamaño mínimo 12 sp.
- Todo espaciado sale de la escala de 4 dp.
- Ambos esquemas, claro y oscuro, deben verse correctos.
- Todo elemento tocable mide al menos 48 dp.
- La interfaz responde al ajuste de tamaño de fuente del sistema sin recortar
  texto. Verificar al 200 %. Parte de los usuarios son adultos mayores.

Dos restricciones de contraste verificadas que deben respetarse:

- **El ámbar `#FFA600` nunca lleva texto ni icono pequeño sobre fondo claro**
  (contraste 1,96:1). Es color de relleno, con texto oscuro encima.
- **El verde de estado con texto encima es `#15782B`**, no `#1A8F33`, que no
  alcanza el contraste requerido para texto de tamaño normal.

## Accesibilidad

- Todo icono e imagen con significado lleva descripción de contenido, **desde
  recursos de cadenas** con el prefijo `cd_`. Los decorativos la llevan nula de
  forma explícita.
- El foco de teclado tiene estado visible.
- Se respeta la preferencia de movimiento reducido del sistema.

## Listas

- Toda lista usa `LazyColumn` o `LazyRow` con `key` estable.
- Toda lista es paginada. Ninguna consulta devuelve una colección sin límite.
- Al final de la lista, un indicador de carga de la página siguiente.

## Textos

**Ningún texto visible escrito en el código.** Todo proviene de recursos de
cadenas, con claves en inglés. Las reglas completas de nomenclatura, parámetros,
plurales y formatos regionales están en `.claude/rules/i18n.md`.

- Los mensajes de error explican qué pasó y qué hacer. Sin disculpas, sin
  vaguedades.
- Los controles dicen exactamente qué ocurre al pulsarlos.
- Nunca concatenar cadenas para formar una frase: el orden de las palabras cambia
  entre idiomas.
- La moneda, las fechas y los números se formatean con las utilidades de la
  plataforma según la configuración regional, nunca componiendo texto a mano.

## Mapas

- El mapa **nunca** se instancia con identificador de estilo en la nube: eso
  reclasifica cada carga a una categoría facturable. El estilo se aplica desde el
  cliente.
- Los marcadores se agrupan cuando hay muchos.
- La posición de la cámara se conserva entre recomposiciones.

## Imágenes

- Se cargan con Coil, con marcador de posición y estado de error.
- Las provenientes del almacenamiento privado se resuelven con URL firmada.
- Las miniaturas de lista se solicitan en tamaño reducido, no la imagen completa:
  el consumo de datos de salida es lo primero que crece con el uso.

## Rendimiento

- Nada de trabajo pesado dentro de una función componible. Va en el modelo de
  vista o más abajo.
- Estado que se lee dentro de un `lambda` diferido, no capturado en la
  recomposición.
- Previsualizaciones para cada estado de la pantalla, incluido el vacío y el de error.

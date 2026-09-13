# CLAUDE.md — Salud en Casa

## Proyecto

Aplicación móvil Android que vincula pacientes con profesionales de salud para
atención domiciliaria en Bolivia. Búsqueda por cercanía geográfica, solicitud
inmediata o agendada, negociación de tarifas, mensajería, confirmación mutua de
pagos y calificación bidireccional.

Requisitos completos: `docs/requirements.md`. Léelo antes de planificar cualquier sprint.
Estado actual del trabajo: `plan.md`. Léelo al inicio de cada sesión.
Decisiones técnicas tomadas: `docs/decisions.md`.

Proyecto académico de grado. El desarrollo es incremental e iterativo bajo SCRUM:
cada sprint entrega un incremento demostrable, nunca una capa horizontal.

Comandos de instalación y ejecución: ver `README.md`.

## Alcance de plataforma

**El producto es exclusivamente Android.** No hay módulo multiplataforma, no hay
objetivo de iOS, no hay código compartido entre plataformas.

Esto es deliberado y está registrado en `docs/decisions.md`: el alcance del
proyecto declara Android, y toda complejidad que no sirva a ese alcance es una
carga, no una ventaja. La proyección a iOS figura como trabajo futuro en
`docs/requirements.md`, sección «Fuera de alcance», y se abordará cuando sea
alcance real.

Si una tarea sugiere agregar soporte multiplataforma, un objetivo de compilación
adicional o una biblioteca elegida por ser portable, **no se hace**. Se anota como
posible trabajo futuro y se sigue con lo que el alcance pide.

## Stack y comandos

- **Lenguaje:** Kotlin, versión estable vigente
- **Proyecto:** un solo módulo Gradle, `:app`
- **Paquete base y `namespace`:** `bo.saludencasa`
- **`applicationId`:** `bo.saludencasa.app`
- **Versión mínima:** API 26
- **Interfaz:** Jetpack Compose + Material 3
- **Backend:** Supabase (PostgreSQL 17 + PostGIS, Auth, Realtime, Storage, Edge Functions)
- **Inyección de dependencias:** Koin
- **Preferencias y sesión local:** DataStore
- **Compilar:** `./gradlew assembleDebug`
- **Pruebas unitarias:** `./gradlew test`
- **Pruebas instrumentadas:** `./gradlew connectedAndroidTest`
- **Análisis estático:** `./gradlew ktlintCheck`
- **Verificación completa:** `./gradlew build`
- **Migraciones:** `supabase migration new <nombre>` · `supabase db push` · `supabase db reset`

**`namespace` y `applicationId` difieren de forma deliberada.** El `namespace`
define la raíz de los paquetes y la clase de recursos; el `applicationId`
identifica la aplicación ante la tienda, Firebase y los identificadores OAuth, y
está atado a las huellas SHA-1 ya registradas. **El `applicationId` no se cambia
nunca.** Registrado en `docs/decisions.md`.

**No se usa base de datos local en esta fase.** El almacenamiento sin conexión no
figura en los requisitos. DataStore cubre la sesión y las preferencias; nada más.
Si aparece la necesidad, se agrega con su requisito y su historia, no por
adelantado.

## Regla de idioma

**Todo artefacto de código se escribe en inglés**: nombres de clases, métodos,
variables y paquetes; tablas y columnas (`snake_case`); valores de enumerados
(`SCREAMING_SNAKE_CASE`); campos JSON (`camelCase`); comentarios; mensajes de
commit; mensajes de registro; nombres de pruebas; nombres de archivos de migración.

Nunca transliterar términos del dominio en español. No usar `Paciente`,
`Solicitud`, `Calificacion`. El mapeo completo español-inglés está en
`.claude/rules/glosario.md`, que se carga automáticamente al trabajar con
archivos Kotlin o SQL.

**Excepciones.** Los documentos del proyecto (`plan.md`, `docs/requirements.md`,
`docs/decisions.md`, `README.md`) se escriben en español, porque son evidencia
académica que revisa el tribunal.

## Control de versiones — no ejecutar operaciones de git

**Nunca ejecutar un comando de git.** Ni `git add`, ni `git commit`, ni `git push`,
ni `git pull`, ni `git checkout`, ni `git branch`, ni `git merge`, ni `git stash`,
ni `git reset`, ni `git rebase`, ni ninguna otra operación sobre el repositorio.

El autor gestiona el control de versiones de forma manual. Esto incluye no crear
ramas, no proponer ejecutar comandos de git y no dar por supuesto que un cambio
quedó versionado.

Al terminar una tarea, **informar qué archivos se crearon, modificaron o
eliminaron**, para que el autor decida qué versiona y cómo.

Editar el archivo `.gitignore` sí está permitido: es configuración del proyecto,
no una operación sobre el historial.

## Internacionalización — ningún texto escrito en el código

**Ningún texto visible al usuario se escribe directamente en el código.** Todo
proviene de recursos de cadenas, identificados por claves en inglés.

Esto aplica a todo: etiquetas de pantalla, textos de botón, mensajes de error,
descripciones de contenido para accesibilidad, textos de estado vacío, títulos de
notificación y opciones de menú. Sin excepciones.

El idioma inicial es el español, en `values-es/`, y `values/` contiene el idioma
de reserva. La aplicación se construye desde el inicio con esta estructura para
que agregar un idioma sea traducir un archivo, no recorrer el código.

Las reglas completas de formato, pluralización, parámetros y mensajes de error
están en `.claude/rules/i18n.md`.

## Invariantes del dominio — nunca violar

1. `profiles` es la identidad única de toda persona. Su `id` coincide con el
   identificador que emite el proveedor de autenticación. `patients` y
   `professionals` comparten identidad con él; nunca duplican la persona.
2. Toda tabla tiene seguridad a nivel de fila habilitada y al menos una política,
   desde la migración que la crea. Ninguna tabla existe sin política.
3. `service_requests.location` es una instantánea de dónde se pidió el servicio.
   Nunca se recalcula desde `addresses` al leer.
4. `services.final_amount_bob` congela el monto acordado. Nunca se recalcula
   desde la oferta ni desde la tarifa vigente del profesional.
5. `services` y `payments` no se editan para corregir. Un servicio se cancela;
   un pago se disputa. Nunca se sobrescribe el histórico.
6. `request_offers` es de solo agregar. Una contraoferta es una fila nueva que
   apunta a la anterior mediante `parent_offer_id`. Nunca se edita una oferta emitida.
7. Solo un profesional con `verification_status = APPROVED` y `profiles.active`
   aparece en resultados de búsqueda o puede emitir ofertas.
8. La búsqueda por cercanía se resuelve siempre con PostGIS dentro de la base de
   datos. Nunca con un servicio externo de búsqueda por proximidad, y nunca
   filtrando distancias en el cliente.
9. Un pago alcanza `BOTH_CONFIRMED` solo cuando paciente y profesional confirman
   por separado. Ninguna confirmación unilateral cierra un pago.
10. `payments.total_amount_bob` siempre iguala la suma de `platform_fee_bob` más
    `professional_amount_bob`. Es una restricción del motor, no una validación de
    la aplicación.
11. Una calificación es única por servicio y autor, con valor entre 1 y 5, y su
    autor nunca coincide con su destinatario.
12. El rol `ADMIN` no accede a `messages`. La conversación de la solicitud
    contiene el detalle de la atención, que es información sensible.
13. Un usuario nunca lee filas de otro usuario. Esto lo garantiza la política de
    la base de datos, no una condición en el código del cliente.
14. La clave de servicio de Supabase jamás sale del entorno de servidor. El
    cliente móvil usa únicamente la clave anónima.

## Límites entre capas

El proyecto es un solo módulo con paquetes por característica. **La regla de
dependencia se sostiene por convención y por revisión, no por el compilador.**
Esto obliga a ser estricto, porque nada impide técnicamente romperla.

Dentro de cada característica, la dependencia apunta siempre hacia el dominio:

- `presentation` depende de `domain`. **Nunca de `data`.**
- `data` implementa las interfaces que declara `domain`.
- `domain` no depende de nada: ni de Compose, ni de Android, ni de Supabase.

Reglas adicionales que el análisis estático debe verificar:

- Ningún archivo de `domain/` importa `androidx.*`, `android.*` ni
  `io.github.jan.supabase.*`.
- Ningún archivo de `presentation/` importa nada de `data/`.
- Una característica **nunca** importa la capa `data` de otra característica. Si
  necesita algo suyo, lo pide por su caso de uso o su interfaz de repositorio.

Un modelo de vista **nunca** inyecta un repositorio ni una fuente de datos
directamente. Consume casos de uso.

## Reglas por tema — se cargan automáticamente

Viven en `.claude/rules/` y se cargan solo al trabajar con archivos que coinciden,
de modo que no consumen contexto en trabajo no relacionado. No hace falta
invocarlas. Si estás *planificando* algo antes de tocar un archivo que coincida,
conviene mencionar la relevante para tenerla en contexto desde el inicio.

| Tema | Archivo | Se carga al trabajar con |
|---|---|---|
| Términos español → inglés | `glosario.md` | cualquier archivo Kotlin o SQL |
| Arquitectura y convenciones Kotlin | `arquitectura.md` | cualquier archivo Kotlin |
| Convenciones de interfaz y diseño | `compose.md` | pantallas y componentes Compose |
| Internacionalización | `i18n.md` | recursos de cadenas y cualquier texto visible |
| Supabase, migraciones y RLS | `supabase.md` | migraciones SQL y fuentes de datos |
| Casos de prueba obligatorios | `testing.md` | archivos de prueba |

El sistema de diseño, con la paleta, la tipografía y las medidas derivadas de las
referencias visuales, está en `docs/design-system.md`. Las imágenes de referencia
viven en `docs/design/references/`. Los diagramas de arquitectura viven en
`docs/architecture/`, escritos en Mermaid dentro de archivos Markdown.

## Flujo de trabajo — mantener los documentos vivos

1. Lee `plan.md` primero. Trabaja únicamente el sprint activo. No comiences el siguiente.
2. Dentro del sprint, trabaja una historia de usuario a la vez, en el orden listado.
3. Cada historia es un incremento vertical: migración si corresponde, dominio,
   datos, presentación y pruebas. Deja algo demostrable al usuario final.
4. Al terminar una historia: marca sus criterios de aceptación en `plan.md`.
5. Al terminar un sprint: marca el sprint, registra en `docs/decisions.md` toda
   decisión técnica no evidente con su fecha y su razonamiento, y anota en la
   retrospectiva del sprint qué se aprendió.
6. Actualiza `README.md` solo cuando cambien la instalación, los comandos o las
   variables de entorno.
7. Una historia está terminada cuando cumple la Definición de Terminado que fija
   `plan.md`.

## Pruebas — la suite completa pasa en cada iteración

**Ninguna historia se cierra con una prueba en rojo.** No existe la prueba
«pendiente de arreglar», ni la prueba deshabilitada temporalmente, ni la prueba
ignorada con una anotación. Si una prueba falla, se corrige el código o se corrige
la prueba, y se explica cuál de las dos estaba equivocada.

**Las pruebas verifican comportamiento, no cobertura.** Una prueba que solo
confirma que un método devuelve lo que se le pidió devolver no aporta nada. Una
prueba vale cuando puede fallar por una razón real: una regla de negocio violada,
un caso límite mal resuelto, una condición de carrera, una política de seguridad
que no filtra.

Al escribir una prueba, la pregunta es **qué error del mundo real atraparía**. Si
no hay respuesta, la prueba sobra.

Cada característica tiene pruebas obligatorias enumeradas en
`.claude/rules/testing.md`. Una historia con su prueba obligatoria ausente no está
terminada, aunque funcione en el dispositivo.

## Disciplina de alcance

Antes de agregar una biblioteca, un módulo, una capa o una funcionalidad, la
pregunta es: **¿qué requisito de `docs/requirements.md` lo exige?**

Si no hay respuesta, no se agrega. Se anota como posible trabajo futuro en la
sección «Fuera de alcance» de ese documento y se sigue.

Esta regla existe porque el proyecto ya pagó el costo de ignorarla una vez, al
adoptar una arquitectura multiplataforma que el alcance no pedía. Está registrado
en `docs/decisions.md`.

## Comentarios y documentación en el código
 
**No se escriben bloques de documentación KDoc.** Ni en clases, ni en funciones,
ni en propiedades. Este proyecto no publica una biblioteca ni genera documentación
de API: nadie consume esos bloques, y describen lo que el nombre y la firma ya
dicen.
 
Un comentario `//` se escribe **únicamente** cuando explica algo que el código no
puede expresar:
 
- Una restricción externa. Por ejemplo, que la construcción del punto de PostGIS
  recibe longitud antes que latitud, o que el identificador de cliente que espera
  Credential Manager es el **Web** y no el de Android.
- Un rodeo impuesto por una biblioteca o por una versión concreta.
- El motivo de una decisión no evidente. En ese caso el comentario **cita la
  fecha y el tema de la entrada** en `docs/decisions.md`, en una sola línea, y
  nada más. Nunca repite el razonamiento, ni siquiera resumido: si el
  razonamiento no está todavía en `decisions.md`, se escribe ahí primero — el
  comentario no es el lugar donde una decisión se explica por primera vez.

Si un comentario describe *qué* hace el código, sobra: el problema es el nombre.
 
El «por qué» del sistema vive en `docs/decisions.md` y en `plan.md`. Repartirlo en
bloques de comentario lo duplica, y la copia del código queda obsoleta sin que
nadie lo note.
 
Esta regla la verifica la revisión, no el análisis estático.

## Estándares de código

- Mantenerlo simple. Nunca sobre-diseñar. Sin abstracción especulativa, sin
  programación defensiva para casos que no pueden ocurrir, sin funcionalidad que
  nadie pidió.
- Kotlin idiomático: `data class` para modelos, `sealed interface` para estados y
  resultados, funciones de extensión para transformaciones, `Flow` para flujos
  reactivos.
- Los resultados de operación se modelan como jerarquías selladas por
  característica, nunca lanzando excepciones para condiciones esperadas.
- Los datos que deben cumplir una regla se modelan como objetos de valor con
  constructor privado y método de fábrica que devuelve `Result`.
- Inyección por constructor únicamente.
- Sin lógica de negocio en pantallas ni en modelos de vista. Ambos delegan en
  casos de uso.
- Ningún texto visible escrito en el código. Todo en recursos de cadenas.
- Ningún color, medida ni tipografía escrita en una pantalla. Todo desde el tema.
- **Todas las dependencias en versión estable.** Ninguna versión `alpha`, `beta`,
  `rc` ni `SNAPSHOT`. El proyecto se defiende dentro de varios meses y una API en
  desarrollo puede cambiar entre sprints.
- Todas las versiones en `gradle/libs.versions.toml`. Ninguna versión escrita
  directamente en un archivo de compilación.
- Sin emojis en ninguna parte: código, comentarios, commits, documentos o registros.
- Mantener el `README.md` mínimo.

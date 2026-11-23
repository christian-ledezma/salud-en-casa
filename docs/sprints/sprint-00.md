# Sprint 0 — Fundación técnica

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-22 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.


**Estado:** `[x]` cerrado el **2026-10-15**, con sus doce historias técnicas
terminadas: HT-01 a HT-09, y HT-19, HT-20 y HT-21. HT-04 cerró su último criterio
el 2026-10-09, en HU-11, al medir el plan de ejecución con quinientos
profesionales cargados. Sin puntos: un sprint de fundación no estima historias de
usuario, de modo que no entra en la medición de velocidad.

**Objetivo del sprint.** Infraestructura operativa y estructura del proyecto
definitiva, verificadas por una prueba de humo de autenticación.

**Objetivo específico que atiende.** 1 · Diseñar la arquitectura de software.

Este sprint no contiene historias de usuario porque no entrega valor visible al
usuario final. Contiene **historias técnicas habilitadoras**, que SCRUM admite
explícitamente para el trabajo de fundación.

## Historias técnicas

### HT-01 · Repositorio, proyecto y estructura `[x]`

Repositorio con control de versiones iniciado y archivo de exclusiones completo.
Proyecto Android de un solo módulo `:app` con el paquete base correcto y la
estructura de paquetes por característica de `.claude/rules/arquitectura.md`.

**Paquete base: se corrigió el proyecto, no los documentos.** Se hizo con cuatro
archivos fuente en el árbol; cuando existan `core/`, `di/`, `navigation/`,
`features/` y sus pruebas, mover el paquete habría sido un refactor de cientos de
archivos.

| Propiedad | Valor | Por qué |
|---|---|---|
| `applicationId` | `bo.saludencasa.app` | **No se toca.** Las huellas SHA-1, los identificadores OAuth y `google-services.json` están atados a él |
| `namespace` | `bo.saludencasa` | Se corrigió. No lo usa ningún servicio externo |
| Raíz de fuentes | `app/src/main/java/bo/saludencasa/` | Movida desde `.../bo/saludencasa/app/` |

Que ambos difieran es válido y habitual: el `namespace` define la raíz de los
paquetes y la clase de recursos; el `applicationId` identifica la aplicación ante
la tienda y los servicios. Registrado en `docs/decisions.md`.

- [x] `namespace = bo.saludencasa` en el archivo de compilación del módulo
- [x] Fuentes movidas a `app/src/main/java/bo/saludencasa/`
- [x] `applicationId = bo.saludencasa.app` sin cambios
- [x] `minSdk = 26`, `compileSdk` y `targetSdk` en la versión estable vigente
- [x] `compileOptions` y `jvmTarget` fijados de forma deliberada en **17**, no
      heredados de la plantilla
- [x] El proceso que ejecuta Gradle también corre sobre **17**, declarado en
      `gradle/gradle-daemon-jvm.properties`. Un solo número de Java en el
      proyecto: compilación, código intermedio, `README.md` e integración continua
- [x] Estructura de paquetes creada: `core/`, `di/`, `navigation/`, `ui/`,
      `features/`
- [x] Repositorio git iniciado **por el autor**
- [x] Archivo de exclusiones ampliado con lo que la plantilla no cubre:
      `*.jks`, `*.keystore`, `google-services.json`, `local.properties`,
      `.env`, `*.log`
- [x] `google-services.json` movido de la raíz a `app/`
- [x] `README.md` creado en la raíz con las instrucciones de puesta en marcha
- [x] `./gradlew assembleDebug` concluye sin error
- [x] Ninguna credencial figura en el historial del repositorio

**Verificado en la compilación.** El manifiesto fusionado declara
`package="bo.saludencasa.app"` —el `applicationId`, intacto— y
`android:name="bo.saludencasa.MainActivity"`, resuelto desde el `namespace` nuevo.
La clase de recursos se genera en `bo/saludencasa/R.class`. Las clases compiladas
llevan versión mayor 61 de código intermedio, que corresponde a Java 17, y
`./gradlew --version` confirma el proceso de Gradle sobre Java 17. `./gradlew
build` completo, con análisis de lint y pruebas, concluye sin error.

**Requisitos:** RNF-06.

> **Sobre `google-services.json`.** Contiene la clave de API de Firebase. Se
> **excluye** del control de versiones y el `README.md` documenta cómo obtenerlo.
> El proyecto tampoco compila sin `local.properties`, así que excluirlo no
> introduce una barrera nueva. Decisión registrada en `docs/decisions.md`.

> **Sobre el repositorio.** El agente nunca ejecuta comandos de git. Este criterio
> lo cumple el autor de forma manual. Hasta entonces, RNF-06 —que se verifica
> «revisión del historial del repositorio»— no puede darse por cumplido.

> El proyecto se recreó desde la plantilla estándar de Android tras abandonar la
> estructura multiplataforma. La configuración de servicios externos del HT-02 no
> se rehace: sigue atada al `applicationId` y a las huellas SHA-1, que no cambian.

### HT-02 · Servicios externos configurados `[x]`

Proyecto Supabase con PostGIS, proyecto de Google Cloud con los tres
identificadores OAuth y ambas huellas de firma, API de Mapas restringida y con
presupuesto, Firebase con Cloud Messaging y App Distribution.

- [x] Proveedor de Google habilitado en Supabase con el identificador Web primero
- [x] Ambas huellas SHA-1 registradas
- [x] Places API **no** habilitada
- [x] Solo Cloud Messaging y App Distribution habilitados en Firebase
- [x] **RNF-05 verificado por configuración del proveedor:** Supabase cifra en
      tránsito con TLS y cifra los discos en reposo. Se deja constancia aquí
      porque es el único requisito no funcional que no se verifica con código

**Requisitos:** RF-01.1, RNF-05, RNF-10.

### HT-03 · Catálogo de versiones `[x]`

Todas las dependencias y sus versiones centralizadas en
`gradle/libs.versions.toml`, siguiendo la convención de agrupaciones de whosinApp.

**Estado real.** El catálogo era el de la plantilla: cumplía la regla de versiones
estables, pero le faltaban casi todas las dependencias del proyecto y varias de las
que tenía eran de una generación anterior a la del complemento de compilación.

**Dependencias que las reglas ya dan por existentes y hoy no están**

| Dependencia | Quién la exige |
|---|---|
| `androidx.lifecycle:lifecycle-runtime-compose` | `.claude/rules/compose.md`, patrón obligatorio de pantalla con `collectAsStateWithLifecycle` |
| Complemento de **ktlint** | `CLAUDE.md` publica `./gradlew ktlintCheck`; hoy ese comando falla y la Definición de Terminado depende de él |
| **Koin** (`koin-android`, `koin-androidx-compose`) | `.claude/rules/arquitectura.md` |
| **supabase-kt** (`postgrest`, `auth`, `realtime`, `storage`, `functions`) | `.claude/rules/supabase.md` |
| **kotlinx-serialization** | La exige el cliente de Supabase |
| **DataStore** | HT-05, sesión y preferencias |
| **Navigation Compose** | `.claude/rules/compose.md` |
| **Coil 3** | `.claude/rules/compose.md` |
| **Maps Compose** y `play-services-location` | HU-05, HU-11 |
| **Credential Manager** y `googleid` | HU-01 |
| **Turbine** | `.claude/rules/testing.md` |

- [x] Ninguna versión de dependencia escrita directamente en un archivo de
      compilación. La del motor de ktlint también sale del catálogo. Única
      excepción, inevitable: el complemento resolvedor de cadenas de herramientas
      en `settings.gradle.kts`, porque el catálogo se declara en ese mismo archivo
      y no está disponible para su propio bloque de complementos
- [x] **Todas las versiones son estables.** Verificado sobre el grafo resuelto:
      111 artefactos, ninguno `alpha`, `beta`, `rc` ni `SNAPSHOT`
- [x] Las versiones rezagadas de la plantilla se actualizan a la estable vigente:
      `espresso-core`, `androidx-junit`, `lifecycle-runtime-ktx`, `activity-compose`
- [x] Las once dependencias de la tabla anterior están declaradas
- [x] Cinco agrupaciones declaradas: `supabase`, `compose`, `maps`, `auth`, `test`
- [x] `./gradlew ktlintCheck` se ejecuta sin error de comando no encontrado
- [x] El proyecto sincroniza sin advertencias de versión

**Actualizaciones aplicadas**

| Dependencia | Plantilla | Ahora |
|---|---|---|
| Kotlin | 2.2.10 | 2.4.20 |
| Compose, declaración de versiones agrupadas | 2026.02.01 | 2026.09.00 |
| `lifecycle-runtime-ktx` | 2.6.1 | 2.11.0 |
| `activity-compose` | 1.8.0 | 1.13.0 |
| `androidx-junit` | 1.1.5 | 1.3.0 |
| `espresso-core` | 3.5.1 | 3.7.0 |

**La subida de Kotlin no fue cosmética.** `maps-compose` arrastra
`kotlin-stdlib:2.4.10`, que un compilador 2.2 no puede leer. Se descubrió cableando
temporalmente las once dependencias y compilando; el cableado se revirtió después.
Registrado en `docs/decisions.md`.

**Qué queda declarado pero sin cablear.** Las agrupaciones `supabase`, `maps` y
`auth`, más Koin, DataStore y Coil, existen en el catálogo y su resolución está
verificada, pero no figuran en el archivo de compilación: cada historia declara lo
que necesita. `supabase`, Koin y DataStore entran en HT-05; `auth` en HU-01;
`maps` en HU-05.

**`.editorconfig`.** El análisis estático necesitaba configuración: la regla de
nomenclatura de funciones de ktlint marca toda función componible por usar
PascalCase. La excepción `ktlint_function_naming_ignore_when_annotated_with =
Composable` vive ahí, junto al estilo de código y la longitud máxima de línea.

**Requisitos:** RNF-09.

> Las plantillas de Android Studio proponen versiones de vista previa de Compose y
> de Material 3, y dejan artefactos de prueba de una generación anterior. Ambas
> cosas se corrigen aquí: una API en desarrollo puede cambiar entre sprints y el
> proyecto se defiende dentro de varios meses.

### HT-04 · Esquema de base de datos `[x]` — último criterio cerrado el 2026-10-09

Ocho migraciones versionadas, en este orden. Cada una crea sus tablas **junto con
sus políticas de seguridad**.

| # | Migración | Contenido |
|---|---|---|
| 1 | `extensions_and_types` | PostGIS y los doce tipos enumerados |
| 2 | `identity` | `profiles`, `patients`, `professionals`, `verification_documents`, `addresses` |
| 3 | `catalog` | `service_types`, `professional_services`, `availability_slots` |
| 4 | `requests` | `service_requests`, `request_offers`, `messages` |
| 5 | `delivery` | `services`, `payments`, `reviews` |
| 6 | `support` | `device_tokens` |
| 7 | `functions` | Búsqueda por cercanía, creación de perfil, recálculo de reputación |
| 8 | `seed_data` | Catálogo inicial de tipos de servicio |

- [x] Las quince tablas existen con seguridad a nivel de fila habilitada y al
      menos una política cada una
- [x] Los índices espaciales aparecen en el plan de ejecución de la consulta de
      cercanía — **solo verificable con datos.** Con las tablas vacías el
      planificador elige recorrido secuencial por ser más barato, así que el
      plan no prueba nada. **Se cumplió en HU-11, el 2026-10-09**, con el
      experimento 6: `Index Scan using idx_addresses_location` con
      `Index Cond: location && _st_expand(...)`, ni un recorrido secuencial en el
      plan, y 64 ms sobre 500 profesionales sintéticos. No hizo falta
      `enable_seqscan = off`
- [x] Las ocho migraciones se aplican sobre el proyecto remoto con
      `npx supabase db push`, previo `--dry-run`
- [x] `supabase db reset` reconstruye la base completa sin errores. **Estuvo
      aplazado desde el Sprint 0 y se cumplió el 2026-10-08**, con el entorno local
      de HT-18: las 26 migraciones se aplican desde cero sin error, el catálogo del
      esquema reconstruido coincide con el del remoto y `db diff --linked` no
      encuentra deriva. Registrado en `docs/decisions.md`, 2026-10-08
- [x] El rol administrador no accede a `messages`: ninguna de sus seis políticas
      invoca `is_admin()`, ni la invocará ninguna migración posterior
- [x] `docs/architecture/data-model.md` contiene el diagrama entidad-relación en
      Mermaid, derivado del esquema efectivamente aplicado

**Verificación contra el proyecto remoto**, tras aplicar las ocho migraciones el
2026-09-12 sobre `salud-en-casa` (`sa-east-1`, PostgreSQL 17.6):

| Qué se comprobó | Cómo | Resultado |
|---|---|---|
| Las quince tablas existen | Tipos generados desde el remoto | 15, sin ninguna de más |
| Los doce enumerados existen | Tipos generados desde el remoto | 12, sin ninguno de más |
| La vista y las funciones existen | Tipos generados desde el remoto | `professional_directory`, `is_admin`, `professional_covers`, `shares_service_with`, `search_nearby_professionals` |
| Los índices se crearon | Estadísticas de índices del remoto | 52, incluidos los dos GIST `idx_addresses_location` e `idx_service_requests_location` |
| El catálogo inicial se insertó | Estadísticas de tablas del remoto | `service_types` con 12 filas; las otras catorce vacías |
| **La seguridad a nivel de fila deniega de verdad** | Lectura anónima de las quince tablas por la API REST con la clave anónima | Las quince devuelven cero filas. `professional_directory` responde `permission denied` |

Esa última fila es la que vale: no comprueba que las políticas estén escritas,
sino que un tercero sin sesión no obtiene ni una fila de ninguna tabla (INV-02,
INV-13).

**Dónde vive cada invariante.** La tabla de correspondencia entre los catorce
invariantes y el mecanismo que los hace cumplir está en
`docs/architecture/data-model.md`. Es el lugar a revisar si alguna vez se
sospecha que uno dejó de cumplirse.

**Requisitos:** INV-02, INV-08, INV-12, RNF-04.

### HT-05 · Cliente de Supabase, secretos e inyección de dependencias `[x]`

Cliente de Supabase con persistencia de sesión, secretos fuera del código fuente,
estructura base de módulos de Koin, y `DataStore` para sesión y preferencias.

- [x] Los secretos se leen de `local.properties` en desarrollo y de los secretos
      del repositorio en integración continua. Llegan al código como campos de
      `BuildConfig`: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`
- [x] Ningún valor de clave figura en el código fuente. Verificado buscando
      `supabase.co`, tokens `eyJ...` y `apps.googleusercontent.com` en `app/src/`
- [x] La sesión persiste entre ejecuciones y el token se renueva de forma
      automática. Verificado cerrando el proceso por completo y volviendo a
      abrir: la pantalla muestra el mismo identificador sin volver a ingresar
- [x] La inyección de dependencias arranca en la clase de aplicación
      `SaludEnCasaApplication`, declarada en el manifiesto
- [x] **No se configura base de datos local.** DataStore cubre sesión y
      preferencias; ningún requisito exige operación sin conexión

**Prueba de humo de autenticación.** Es el incremento del sprint y esta historia
es su dueña. Pantalla desechable con un botón que recorre la cadena completa:

- [x] Al pulsar el botón aparece el selector de cuentas nativo de Android
- [x] Al elegir una cuenta, Supabase valida el token y crea la sesión
- [x] El disparador crea la fila en `profiles` con el rol sin asignar
- [x] La pantalla muestra el identificador obtenido
- [x] La fila es visible en el panel de la base de datos

Si los cinco ocurren, toda la cadena de configuración es correcta. El código se
elimina al implementar HU-01, **y así ocurrió**: `AuthSmokeTestScreen`,
`AuthSmokeTestViewModel` y `GoogleAuthClient` ya no existen. El botón de fallo
provocado sobrevivió más tiempo, porque HT-08 todavía lo necesitaba: vivió en la
pantalla «Mi cuenta», solo en compilaciones de depuración, hasta que el autor
verificó Crashlytics con él y se retiró (ver HT-08, 2026-09-15).

**Los cinco verificados sobre el emulador el 2026-09-12**, recorriendo el flujo
completo: selector de cuentas, pantalla de consentimiento, sesión creada y fila
`08ddb28f-…` en `profiles` con `role` nulo, `active` verdadero y reputación en
cero. El identificador que muestra la pantalla coincide con el de la base.

> **Defecto encontrado al probarlo, y corregido.** La primera versión consultaba
> la sesión una sola vez y de forma síncrona al construir el modelo de vista. El
> cliente restaura la sesión guardada de forma asíncrona, así que la consulta
> siempre llegaba antes y respondía que no había nadie: la sesión parecía
> perderse en cada reinicio aunque estuviera correctamente guardada en DataStore.
> Ahora la sesión se **observa** como flujo. Es un defecto que la compilación no
> podía detectar y que solo apareció al cerrar y reabrir la aplicación.

**Qué compone la cadena, para saber dónde mirar si falla**

| Pieza | Dónde vive |
|---|---|
| Selector de cuentas | `GoogleAuthClient`, mediante Credential Manager |
| Nonce | `core/util/Nonce.kt`: Google firma sobre el resumen, Supabase verifica el valor crudo |
| Intercambio del token | `supabase.auth.signInWith(IDToken)` con el proveedor Google |
| Creación del perfil | Disparador `on_auth_user_created` en la base, ya aplicado en HT-04 |
| Persistencia | `DataStoreSessionManager` |

El README explica en qué orden revisar cuando el ingreso falla.

**Deuda reconocida, saldada en HU-01.** El modelo de vista consumía un cliente de
infraestructura en lugar de un caso de uso, que no es la forma que pide la
arquitectura. Se aceptó solo porque esa pantalla era andamiaje sin dominio
detrás. HU-01 la reemplazó por el flujo real: ningún modelo de vista del
proyecto inyecta ya otra cosa que casos de uso.

**Comentarios revisados contra la sección nueva de `CLAUDE.md`.** Los nueve
archivos Kotlin de esta historia se depuraron: sin bloques KDoc, y cada
comentario `//` restante explica algo que el código no puede expresar —una
restricción externa, un rodeo de biblioteca, o el motivo de una decisión,
referenciando `docs/decisions.md` o este mismo archivo en vez de repetirlos.
Las migraciones ya aplicadas al proyecto remoto —las ocho de HT-04 y la de
corrección de seguridad que se agregó después— quedaron **fuera** de esta
limpieza a propósito: una migración aplicada no se edita nunca, tampoco para esto.

**Revisión del pull request #1.** El revisor automático de GitHub señaló 24
observaciones sobre la rama. Cada una se verificó contra el esquema real antes de
aceptarla: la más grave se probó con un experimento propio contra la base de
datos remota, no solo leyendo el código. Las 24 eran reales; una de
ellas —la lectura de `pg_trigger_depth()` dentro de una cláusula `WHEN`— resultó
más grave de lo que el revisor describía, porque ya estaba aplicada y rompía en
producción el registro de una calificación o la finalización de un servicio, no
solo dejaba una puerta de seguridad abierta. Veintiuna se corrigieron; tres
quedaron registradas como deuda deliberada, detalladas más abajo.

Los cambios entran en dos grupos:

- **Código Kotlin**, corregido directamente: `GoogleAuthClient` ahora valida las
  tres variables de configuración, no solo la del cliente de Google, y ya no
  atrapa `CancellationException` como si fuera un fallo de autenticación.
- **Base de datos**, corregida con una migración nueva,
  `20260912110000_close_lifecycle_and_visibility_gaps`, catorce puntos: el error
  de profundidad de disparador ya descrito; el rol no verificado al crear
  `patients`/`professionals`; la aceptación de solicitudes y ofertas por
  escritura directa sin pasar por la operación atómica de RF-08.5, que todavía no
  existe; la falta del filtro `professional_covers()` al emitir una oferta; la
  contraoferta del paciente sin hilo válido; la llegada y finalización de un
  servicio registrables por cualquiera de las dos partes en vez de solo el
  profesional; la confirmación de un pago antes de que el servicio esté
  completado; la disputa de un pago por escritura directa; las calificaciones
  del paciente visibles públicamente cuando RF-12.4 solo hace pública la
  reputación del profesional; la fila completa de `patients` —incluidos
  `medical_notes` y `emergency_contact`— expuesta a la contraparte de un
  servicio; el catálogo y los servicios desactivados igual visibles; la
  conversación de un paciente legible antes de que exista un servicio; el límite
  negativo de la búsqueda por cercanía llegando sin filtrar hasta Postgres; el
  valor `IOS` en un dominio que este proyecto declara exclusivamente Android; y
  la cadena de baja en cascada que podía borrar servicios y pagos históricos.
  También se corrigieron dos observaciones fuera del esquema: un comentario en
  español en `app/build.gradle.kts` y la referencia a un `seed.sql` inexistente
  en `supabase/config.toml`.

Tres observaciones se registraron como deuda en vez de corregirse: la validación
de la franja de disponibilidad al crear una solicitud agendada (RF-07.3), el
flujo de integración continua que mapee los secretos del repositorio a
`local.properties` (ninguno existe todavía en este proyecto), y la operación
atómica de aceptación de oferta que RF-08.5 exige — la migración de esta revisión
cierra el camino directo hacia `ACCEPTED` sin abrir todavía el correcto, porque
construirlo es alcance de la historia de negociación, no de una corrección de
revisión. Las tres quedan anotadas en `docs/decisions.md`, 2026-09-12.

**Requisitos:** RF-01.1, RF-01.2, RF-01.3, RF-01.6, RNF-06.

### HT-06 · Sistema de diseño `[x]`

Implementación del tema a partir de `docs/design-system.md`: paleta con ambos
esquemas, tipografía Inter con la escala definida, formas, espaciado y colores
semánticos de estado como extensión del tema.

- [x] Ambos esquemas, claro y oscuro, definidos y verificados
- [x] El contraste de cada combinación de texto cumple los criterios de accesibilidad
- [x] El ámbar no se usa como color de texto sobre fondo claro
- [x] Componentes base disponibles: tarjeta de profesional, chip de especialidad,
      distintivo de calificación, selector de fecha, control segmentado, campo de
      formulario, botón principal y barra de navegación flotante
- [x] Cada componente tiene previsualización en ambos esquemas

**Restos de la plantilla que contradicen el sistema de diseño**

- [x] **`dynamicColor` en `false`, o el parámetro eliminado.** Se eliminó el
      parámetro: `SaludEnCasaTheme` ya no lo acepta, así que no puede reactivarse
      por descuido
- [x] El tema deja de heredar de `Theme.Material.Light.NoActionBar`, que fija el
      esquema claro
- [x] Existe `res/values-night/` con el esquema oscuro
- [x] La paleta de la plantilla queda reemplazada por la de `docs/design-system.md`
- [x] La tipografía pasa del único estilo con la familia por omisión a los diez
      estilos con Inter de `docs/design-system.md`

**Requisitos:** RNF-07.

> El prototipado en Figma **no forma parte de esta historia**. Se prototipan las
> pantallas de cada sprint al planificarlo, no las seis del producto por
> adelantado. Ver «Diseño previo a cada sprint».

**Cómo se resolvió lo que el documento no fijaba en un valor exacto.**
`docs/design-system.md` especifica cada color y cada tamaño de tipografía, pero no
asigna un rol de Material 3 a cada uno. `SaludEnCasaShapes` mapea los cinco radios
del documento a los cinco huecos de `Shapes`; la tarjeta destacada de 24 dp, la
píldora y el avatar circular no tienen hueco en ese objeto, así que viven en
`ExtraShapes`, expuesto igual que la tipografía y los colores de estado, desde
`SaludEnCasaTheme`.

`SaludEnCasaTheme` pasó de función a `object` con `operator fun invoke`, exactamente
como `MaterialTheme` lo hace en Compose: permite seguir escribiendo
`SaludEnCasaTheme { ... }` para envolver la pantalla y, además,
`SaludEnCasaTheme.spacing`, `SaludEnCasaTheme.statusColors` y
`SaludEnCasaTheme.extraShapes` para lo que Material 3 no tiene rol propio.

**Deuda reconocida.** El apartado 6 del documento pide Material Symbols; los ocho
componentes usan el conjunto núcleo de Material Icons (`material-icons-core`, no
`-extended`, por RNF-03) porque son la prueba de que el tema funciona, no pantallas
reales todavía — ninguna historia ha empezado a consumirlos con contenido real. El
cambio de icono queda para cuando una pantalla real los necesite.
`ProfessionalCard` incorpora Coil para la fotografía, ya requerido por
`.claude/rules/compose.md`, pero sin motor de red (`coil-network-ktor3`): nada en
el proyecto hace todavía una petición HTTP de imagen real, así que Coil cae a su
estado de marcador de posición hasta que algo la necesite. Ambas quedan en
`docs/decisions.md`, 2026-09-12.

### HT-07 · Estructura de internacionalización `[x]`

Recursos de cadenas con `values/` como reserva y `values-es/` como idioma inicial.
Convención de claves aplicada. Regla de análisis estático que detecte texto escrito
en el código.

- [x] Existen `app/src/main/res/values/strings.xml` y `values-es/strings.xml`
- [x] La regla de análisis estático señala cualquier texto visible escrito en el código
- [x] Los tipos de error del dominio transportan tipos, nunca frases
- [x] La prueba `everyStringKeyUsedInCodeExistsInAllLocales` existe y pasa

**Restos de la plantilla que contradicen la regla**

- [x] El componible `Greeting` y su texto escrito en el código quedan eliminados.
      La regla de análisis estático lo marcaría el primer día
- [x] Las pruebas de ejemplo de la plantilla quedan eliminadas. Son exactamente el
      tipo que `.claude/rules/testing.md` describe como «no aporta», y la suite
      debe arrancar limpia

**Requisitos:** RNF-07.

**Estado real.** `values/strings.xml` y `values-es/strings.xml` ya existían desde
HT-05, con la convención de claves de `.claude/rules/i18n.md` ya aplicada
(`common_`, `auth_`, `error_`), y `SignInError` ya modelaba sus errores como tipo
sellado sin frases. Los restos de la plantilla (`Greeting`, pruebas de ejemplo)
tampoco existían: ya se habían retirado en una historia previa. El trabajo de
esta historia fue exclusivamente el análisis estático que faltaba: tres pruebas
nuevas bajo `bo.saludencasa.i18n`, en `app/src/test`, que corren con
`./gradlew test` igual que cualquier otra prueba unitaria.

### HT-08 · Integración continua, análisis estático y monitoreo `[x]`

Flujo automático que se ejecuta en cada envío de código y en cada solicitud de
fusión, más el monitoreo de errores en producción.

Etapas del flujo, en `.github/workflows/ci.yml`:

| Etapa | Acción | Paso del flujo |
|---|---|---|
| Preparación | Descarga del código, JDK 17, caché de dependencias | `Check out the repository`, `Set up JDK 17`, `Set up Gradle with dependency caching` |
| Secretos | Generación de `local.properties` desde los secretos del repositorio | `Write the credentials the build needs` |
| Análisis estático | ktlint y las reglas propias del proyecto | `Static analysis` → `./gradlew staticAnalysis` |
| Pruebas | `./gradlew test` | `Unit tests` |
| Compilación | `./gradlew assembleDebug` | `Debug build` |

- [x] Las cinco etapas concluyen correctamente en un envío de prueba. El flujo se
      escribió y se verificó comando por comando en local; la primera ejecución
      real llegó con HT-19 el 2026-10-13 (ejecución 54) y desde entonces corre en
      cada envío sobre `master`: ejecuciones 59 (HT-20) y 62 (HT-21), las tres con
      análisis estático, `./gradlew test` y `assembleDebug` en verde. El
      procedimiento para cargar los cuatro secretos está en el `README.md`
- [x] El análisis estático incluye las reglas de arquitectura: `domain/` sin
      importaciones de plataforma, `presentation/` sin importaciones de `data/`
- [x] El análisis estático incluye la regla de texto escrito en el código
- [x] La prueba `domainLayerHasNoPlatformImports` existe y pasa
- [x] Un error provocado deliberadamente aparece en el panel de monitoreo —
      **verificado por el autor.** El botón «Provocar un fallo de prueba» vivía en
      la pantalla «Mi cuenta», solo en compilaciones de depuración; el autor lo
      pulsó, cerró y volvió a abrir la aplicación, y el reporte llegó al panel de
      Crashlytics en el arranque siguiente, como se esperaba. El botón ya cumplió
      su propósito y se retiró de `AccountScreen.kt` el 2026-09-15, junto con el
      recurso `debug_force_crash` que ya no usaba nadie

**Requisitos:** RNF-06, RNF-09.

> Estas reglas del análisis estático sustituyen la garantía que antes daba el
> compilador al separar módulos. Sin ellas, la regla de dependencia queda
> únicamente en la disciplina de quien programa.

**Las reglas propias no son reglas de ktlint, son pruebas.** La decisión se tomó
en HT-07 y está registrada en `docs/decisions.md`: escribir una regla propia de
ktlint o de Detekt exige un artefacto Kotlin separado del que dependa `:app`, y
el proyecto es de un solo módulo por decisión. La tarea `staticAnalysis` agrupa
las dos herramientas —`ktlintCheck` para el estilo y `testDebugUnitTest` para las
reglas propias— de modo que la etapa del flujo que dice «análisis estático»
ejecute de verdad todo lo que el criterio enumera. La etapa siguiente, la de
pruebas, reutiliza ese mismo resultado y por eso aparece como ya actualizada.

**Las tres reglas de arquitectura se verificaron provocando su fallo.** Hoy no
existe ningún archivo bajo `domain/` ni bajo `data/`, así que dos de las tres
pasarían igual aunque estuvieran mal escritas. Para no dejar una red de seguridad
que no sostiene nada, se crearon cuatro archivos sonda con violaciones reales
—un `domain/` que importa `android.util.Log`, un `presentation/` que importa la
capa `data` de su propia característica y una característica que importa la capa
`data` de otra—, se comprobó que las tres reglas fallaban, y se eliminaron. La
tercera **no falló en el primer intento**: su expresión regular anclaba la ruta
en `^features/` cuando la ruta relativa empieza en `bo/saludencasa/`. Sin el
experimento habría quedado permanentemente en verde sin mirar nada.

**Monitoreo: Firebase Crashlytics, no Sentry.** Ningún documento del proyecto
nombraba una herramienta. Se eligió Crashlytics porque Firebase ya está en el
proyecto desde HT-02 y el Sprint 10 distribuye por App Distribution, del mismo
proveedor. El razonamiento completo y su costo están en `docs/decisions.md`.
HT-02 declaraba «solo Cloud Messaging y App Distribution habilitados en
Firebase»: Crashlytics se suma a esa lista.

### HT-09 · Documentación de arquitectura `[x]`

Diagramas en `docs/architecture/`, escritos en **Mermaid dentro de archivos
Markdown**, no como imágenes sueltas. Así se versionan, se comparan entre
revisiones y se renderizan en el repositorio.

| Archivo | Contenido |
|---|---|
| `docs/architecture/components.md` | Aplicación, servicios externos y sus relaciones |
| `docs/architecture/packages.md` | Estructura de paquetes y la regla de dependencia |
| `docs/architecture/auth-sequence.md` | Secuencia completa de autenticación |
| `docs/architecture/deployment.md` | Dónde se ejecuta cada componente |

- [x] Los cuatro diagramas están escritos y renderizan correctamente
- [x] `docs/decisions.md` está iniciado con las decisiones ya tomadas
- [x] El archivo `README.md` permite a una persona ajena levantar el proyecto desde cero

**Cómo se verificó que renderizan.** Este entorno no tiene forma de abrir GitHub
para comprobarlo a ojo, así que los seis diagramas —uno en `components.md`, dos
en `packages.md`, uno en `auth-sequence.md`, uno en `deployment.md`— se
extrajeron de los archivos finales y se renderizaron con `@mermaid-js/mermaid-cli`
a SVG y PNG. Los cinco de `components.md`, `auth-sequence.md` y `deployment.md`
se revisaron además visualmente. Es la misma herramienta que usa la vista previa
de Mermaid de GitHub por debajo, así que un diagrama que renderiza aquí renderiza
ahí.

### HT-19 · Identidad de marca: ícono y arranque `[x]` — 2026-10-13

> Como **persona que abre la aplicación**, quiero **reconocer la marca desde el ícono
> hasta la primera pantalla**, para **saber que estoy en Salud en Casa y no en una
> plantilla**.

**Origen.** Pedido del autor como Product Owner el 2026-10-13. No responde a un
requisito funcional: retira el último resto de la plantilla, el robot verde del ícono,
como HT-06 retiró su paleta. RNF-02 condiciona la duración del arranque. El
razonamiento está en `docs/decisions.md`, en las dos entradas del 2026-10-13.

**Diseño aprobado.** El autor entregó el logo como imagen. Se redibujó en vectores y se
le presentaron dos variantes con la animación. Eligió la **B**, armonizada con el tema:
casa y cruz en `primary`, corazón en verde menta. Aprobó también la duración del trazo y
el latido del logo. Colores, medidas y tiempos están en `docs/design-system.md`,
sección 9.

- [x] El ícono de la plantilla se reemplaza por el dibujo del logo, sin el nombre ni la
      leyenda: ícono adaptativo sobre blanco, más la versión monocroma para los íconos
      temáticos de Android 13
- [x] Se eliminan los diez `.webp` de la plantilla: con `minSdk = 26`, el ícono
      adaptativo cubre todas las versiones
- [x] Al tocar el ícono, la ventana muestra el logo centrado sobre el fondo de la
      aplicación, en claro y en oscuro. En Android 12 o superior lo muestra el splash
      del sistema; en Android 8 a 11, el fondo de la ventana
- [x] La pantalla de arranque recoge el logo donde lo dejó el sistema, lo asienta y
      muestra «Salud en Casa». La línea cardio se traza de izquierda a derecha y el logo
      late cuando la línea cruza el pico
- [x] Al abrir la aplicación, la navegación espera ese primer trazo (1,75 s). Al volver
      del ingreso no se repite
- [x] Con «quitar animaciones» activado se muestra el cuadro final y no se espera
- [x] Si la carga dura más, la línea sigue barriendo como un monitor, y el error
      conserva el botón para reintentar
- [x] Ninguna leyenda ni texto visible escrito en el código: el nombre es una imagen con
      `app_name` como descripción
- [x] La integración continua pasó el 2026-10-13 (ejecución 54) con Java 17: análisis
      estático, `./gradlew test` con las ocho pruebas nuevas y `assembleDebug`
- [x] `./gradlew build` completo. Suma el análisis de Android lint, que la integración
      continua no corre. Verificado por el autor el 2026-10-13
- [x] Las tres previsualizaciones del arranque —claro, oscuro y al 200 %— se abrieron y
      se ven correctas. Verificado por el autor el 2026-10-13
- [x] Recorrido en emulador con Android 12 o superior y con uno de 8 a 11, hecho por el
      autor el 2026-10-13:
  - el logo no salta entre el splash y la animación;
  - la aplicación llega a la pantalla que corresponde;
  - al girar el teléfono en cualquier pantalla no asoma el logo

**Por qué los tres últimos los cerró el autor.** La sesión del 2026-10-13 no pudo compilar.
La política de red del entorno rechaza `dl.google.com`, donde vive el complemento de
Android, y Maven Central respondió con límite de peticiones. Además, el entorno solo tiene
Java 21 y el proceso de Gradle está fijado en 17. Para intentarlo, se cambió
temporalmente `gradle/gradle-daemon-jvm.properties` a 21 y se agregó un
`app/google-services.json` de relleno. **Los dos se restauraron**: el primero quedó igual
byte a byte, y el segundo se eliminó. La primera compilación real fue la de la
integración continua, que no corre Android lint ni puede abrir una previsualización. El
autor verificó los tres puntos y los dio por cumplidos el 2026-10-13. El recorrido era
obligatorio porque el arranque es parte del primer ingreso de HU-01, que figura como
crítica en «Cuándo hace falta un dispositivo».

**Pruebas.**

| Prueba | Qué error atrapa |
|---|---|
| `HeartbeatMotionTest.everyPhaseHasSettledWhenTheIntroEnds` | Una fase que dura más que la animación queda cortada en el último cuadro: el logo a mitad del latido o el nombre a medio aparecer |
| `HeartbeatMotionTest.theLogoBeatsAsTheTraceCrossesItsTallestPeak` | Mover el pico de la línea sin mover el latido, o al revés |
| `HeartbeatMotionTest.theTraceHasNoJumpAtAnyPointItIsDrawnThrough` | El defecto de la primera versión: el pico R contado dos veces, dibujado al doble de su altura |
| `StartupContentTest.aLaunchLeavesOnlyOnceTheHeartbeatHasBeenDrawn` | Salir a mitad del trazo, o no salir nunca |
| `StartupContentTest.aReturnFromSignInDoesNotWaitForTheIntro` | Volver a cobrar la espera después del ingreso |
| `StartupContentTest.withAnimationsRemovedALaunchDoesNotWait` | Hacer esperar un trazo que no se dibuja |
| `StartupScreenLargeFontTest.theNameAndTheHeartbeatFitTheNarrowestScreen` | El nombre o la línea fuera de los 320 dp |
| `StartupScreenLargeFontTest.theWayOutOfAFailedStartStaysReachable` | El botón para reintentar fuera de alcance al 200 % |

**Archivos.**

| Capa | Archivos |
|---|---|
| Ícono | `res/drawable/ic_launcher_{foreground, background, monochrome}.xml` · `res/mipmap-anydpi/ic_launcher{,_round}.xml` (modificados o nuevos) · `res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher{,_round}.webp` (eliminados, con sus carpetas) |
| Marca | `res/drawable/ic_brand_{mark, wordmark}.xml` · `res/drawable-night/ic_brand_{mark, wordmark}.xml` · `res/drawable/startup_window.xml` · `docs/design/brand/salud-en-casa-mark.svg` (nuevos) |
| Tema y ventana | `res/values/{colors, themes}.xml` · `res/values-night/colors.xml` · `res/values-v31/themes.xml` (nuevos o modificados) · `res/values-night/themes.xml` (eliminado: el color de la ventana pasó a `values-night/colors.xml`) · `AndroidManifest.xml` · `MainActivity.kt` |
| Interfaz | `ui/animations/{HeartbeatMotion, HeartbeatTrace}.kt` (nuevos) · `ui/theme/{Color, SaludEnCasaTheme, Spacing}.kt` · `features/auth/presentation/StartupScreen.kt` · `navigation/{Routes, SaludEnCasaNavHost}.kt` (modificados) |
| Pruebas | `test/.../ui/animations/HeartbeatMotionTest.kt` · `test/.../auth/presentation/{StartupContentTest, StartupScreenLargeFontTest}.kt` (nuevos) |
| Documentos | `docs/decisions.md` · `docs/design-system.md` (sección 9) · `.claude/rules/glosario.md` · `plan.md` (esta sección) |

**El diagrama de autenticación se corrigió durante la propia verificación.** La
primera versión mostraba a Supabase rechazando el token sin que Google
respondiera nunca la verificación dentro de esa misma rama del diagrama —una
secuencia que no podía ocurrir así—. Se corrigió moviendo la respuesta de
verificación antes de la bifurcación entre aceptar y rechazar, para que ambas
ramas partan del mismo paso.

**`packages.md` documenta la regla, y hoy casi ninguna característica existe
para violarla.** Ver la nota de «Estado real» en ese archivo: solo
`features/auth/presentation/` existe, así que dos de las tres reglas de
dependencia no tenían todavía ninguna violación real que las pusiera a prueba.
Se verificaron de todos modos con los archivos sonda de HT-08 antes de dar esta
historia por cerrada, no dando por sentado que documentar la regla bastaba para
demostrar que la prueba automática la sostiene.

### HT-20 · La bienvenida con la marca `[x]` — 2026-10-14

> Como **persona que abre la aplicación por primera vez**, quiero **una bienvenida
> que continúe la marca del arranque y me diga qué ofrece la aplicación**, para
> **decidir con confianza si ingreso**.

**Origen.** Pedido del autor como Product Owner el 2026-10-14, sobre la marca de
HT-19. Rehace la pantalla de HU-01 sin cambiar su comportamiento: el ingreso con
Google es el mismo. El razonamiento está en `docs/decisions.md`, 2026-10-14.

**Diseño aprobado.** Se presentaron tres propuestas y el autor eligió la **A,
continuidad con el arranque**. Pidió cuatro ajustes:
- el botón de Google con el mismo color en los dos modos;
- el círculo de la G simétrico;
- botones de Términos y condiciones y de Política de privacidad;
- nada anclado al pie.

Aprobó los textos y el retiro de las cifras fijas.

- [x] La marca del arranque —logo, nombre dibujado y línea cardio— reemplaza al
      ícono provisional y al nombre repetido
- [x] El eslogan acompaña a la marca, sin texto justificado en ningún lugar de la
      pantalla
- [x] Se retiran «4.9 Valoración» y «40+ profesionales»; tres puntos de valor que
      la aplicación ya cumple ocupan su lugar
- [x] El botón de Google usa el turquesa de la marca en claro y en oscuro, y el
      círculo de la G mantiene su forma con cualquier tamaño de fuente
- [x] Términos y condiciones y Política de privacidad son botones, todavía sin
      destino
- [x] Nada queda anclado: el ingreso se desplaza con el resto del contenido
- [x] La marca entra desde donde la dejó el arranque, sin fundido entre las dos
      pantallas; con «quitar animaciones» aparece quieta
- [x] La pantalla pinta su propio fondo, así que la vista previa oscura se ve como
      en el teléfono
- [x] La integración continua pasó el 2026-10-15 (ejecución 59, sobre `master`):
      análisis estático, `./gradlew test` y `assembleDebug`
- [x] Las previsualizaciones de la bienvenida —claro, oscuro y al 200 %— se
      abrieron y se ven correctas
- [x] Recorrido en emulador: del arranque a la bienvenida la marca no salta, y el
      ingreso con Google sigue llevando a la pantalla que corresponde

**Por qué los dos últimos los cerró el autor.** La sesión del 2026-10-14 no pudo
compilar: la política de red del entorno rechaza `dl.google.com`, así que la
primera compilación fue la de la integración continua, que no abre una
previsualización. El recorrido era obligatorio porque la bienvenida es el primer
ingreso de HU-01, que figura como crítica en «Cuándo hace falta un dispositivo»;
el autor abrió las tres previsualizaciones y recorrió el emulador, y dio los dos
puntos por cumplidos.

**Pruebas.**

| Prueba | Qué error atrapa |
|---|---|
| `StartupBrandGeometryTest.theWelcomeEntranceStartsWhereTheStartupLeavesTheBrand` | La marca del arranque se mueve y la entrada de la bienvenida sigue partiendo del lugar viejo: la marca da un salto |
| `WelcomeScreenLargeFontTest.theWayInStaysReachable` | Sin el pie anclado, el botón de ingreso queda fuera de alcance al 200 % |
| `WelcomeScreenLargeFontTest.theTermsAndThePrivacyPolicyStayReachable` | Los botones legales quedan fuera de la pantalla al 200 % |
| `WelcomeScreenLargeFontTest.theSignInButtonGrowsWithItsLabelInsteadOfCuttingIt` | `PrimaryButton` vuelve a un alto fijo y corta la segunda línea de su etiqueta |

**Archivos.**

| Capa | Archivos |
|---|---|
| Interfaz | `features/auth/presentation/{WelcomeScreen, StartupScreen}.kt` · `navigation/SaludEnCasaNavHost.kt` · `ui/components/PrimaryButton.kt` · `ui/theme/Color.kt` (modificados) · `ui/components/PulsingHeartIcon.kt` (eliminado) |
| Recursos | `res/drawable/{ic_verified_user, ic_group}.xml` (nuevos) · `res/values/strings.xml` · `res/values-es/strings.xml` (claves nuevas de la bienvenida; se retiran las de las insignias, los puntos anteriores y el pie legal) |
| Pruebas | `test/.../auth/presentation/{StartupBrandGeometryTest, WelcomeScreenLargeFontTest}.kt` (nuevos) · `test/.../ui/ScreenCatalog.kt` (las vistas previas con «Dark» en el nombre se dibujan con los recursos nocturnos, que es donde viven los colores oscuros de la marca) |
| Documentos | `docs/decisions.md` · `docs/design-system.md` (secciones 5 y 9) · `.claude/rules/glosario.md` · `plan.md` (esta sección) |

### HT-21 · La búsqueda y la ficha del profesional `[x]` — 2026-10-15

> Como **paciente que busca atención**, quiero **comparar a los profesionales
> cercanos de un vistazo y leer su ficha sin esfuerzo**, para **elegir a quién
> llamar**.

**Origen.** Pedido del autor como Product Owner el 2026-10-15, después de HT-20.
Rehace las pantallas de HU-11 y HU-12 sin cambiar su comportamiento: la búsqueda,
sus criterios y lo que la ficha publica son los mismos. El razonamiento está en
`docs/decisions.md`, 2026-10-15.

**Diseño aplicado.** Se presentaron dos propuestas por pantalla y una corrección
común al tema. El autor pidió aplicar las mejoras directamente sobre `master`, y se
aplicaron las recomendadas: la **A** de la búsqueda y la **A** de la ficha. La B de
cada una queda disponible si el autor la prefiere.

- [x] El tema define todo rol que leen los componentes, en claro y en oscuro: ningún
      lila de la paleta de base de Material 3
- [x] Todo monto se muestra con el símbolo «Bs» y sin decimales cuando es entero,
      sea cual sea el idioma del teléfono; un monto con centavos los conserva
- [x] La tarjeta de profesional se toca entera, con avatar de iniciales a la
      izquierda, chevrón a la derecha y la disponibilidad dentro de la tarjeta
- [x] La ficha de radio seleccionada lleva la marca de comprobación, la de
      disponibilidad el punto verde, y el filtro de tipo plegado se dibuja como un
      campo
- [x] La ficha se titula «Perfil del profesional» y abre con una cabecera: avatar,
      nombre, profesión y especialidad, distintivo de verificado y tres cifras
- [x] Sin calificaciones, la ficha no muestra un cero: muestra que todavía no las
      tiene
- [x] Disponibilidad y tarifa base van en píldoras; la biografía lleva título; cada
      servicio es una tarjeta con su precio y su duración en horas o minutos
- [x] Con la fuente al 150 % o más, las cifras y el precio de cada servicio se
      apilan
- [x] La integración continua pasó el 2026-10-15 (ejecución 62): análisis
      estático, `./gradlew test` y `assembleDebug`
- [x] Las previsualizaciones de las dos pantallas —claro, oscuro y al 200 %— se
      abrieron y se ven correctas

**La primera ejecución, la 61, falló en ktlint.** Una cadena de cuatro llamadas en
una sola línea, `Locale.Builder()...build()`, en `MoneyFormat.kt` y en su prueba. El
código ya compilaba. Se reemplazó por `Locale.forLanguageTag("es-BO")`, como en el
resto del proyecto.

**Por qué el último lo cerró el autor.** La sesión del 2026-10-15 no pudo
compilar: la política de red del entorno rechaza `dl.google.com`, y la integración
continua no abre una previsualización. Ninguna de las dos pantallas figura como
crítica en «Cuándo hace falta un dispositivo», así que no exigen recorrido; las
previsualizaciones sí, porque el recorte dentro de una píldora o de una columna de
cifras no lo ve ninguna prueba, y el autor las abrió y las dio por correctas.

**Pruebas.**

| Prueba | Qué error atrapa |
|---|---|
| `MoneyFormatTest.theSymbolIsTheBolivianOneWhateverTheDeviceLocale` | Un teléfono en inglés o en español de otro país vuelve a mostrar «BOB» |
| `MoneyFormatTest.aWholeAmountShowsNoDecimals` | Los «,00» vuelven a cada tarifa |
| `MoneyFormatTest.anAmountWithCentsKeepsBothDecimals` | Quitar los decimales a todo monto, y mostrar otro precio |
| `PublicProfileScreenLargeFontTest.aProfessionalNobodyHasRatedShowsNoScoreInsteadOfAZero` | La cifra de la ficha dibuja el distintivo con el promedio cero de quien no tiene calificaciones |

Dos pruebas existentes se ajustaron a la forma nueva, sin cambiar lo que afirman:

- `ProfessionalSearchScreenLargeFontTest.theDistanceOfAnUnratedProfessionalIsNotSqueezedOutOfItsRow`
  busca los textos en el árbol sin fusionar. La tarjeta entera es ahora un botón, y
  en el árbol fusionado un texto responde con los límites de su tarjeta.
- `PublicProfileScreenLargeFontTest.theNameAndTheRateFitTheScreen` mueve la lista
  hasta las píldoras antes de buscar la tarifa. La cabecera creció, y al 200 % la
  fila de la tarifa puede quedar por debajo del pliegue, donde la lista perezosa no
  la compone.

**Archivos.**

| Capa | Archivos |
|---|---|
| Tema | `ui/theme/{SaludEnCasaTheme, Color, Spacing}.kt` (modificados) |
| Utilidades | `core/util/MoneyFormat.kt` (modificado) |
| Componentes | `ui/components/{ProfessionalCard, ProfileAvatar}.kt` (modificados) · `ui/components/AvailabilityPill.kt` (nuevo) |
| Interfaz | `features/search/presentation/ProfessionalSearchScreen.kt` · `features/profile/presentation/PublicProfileScreen.kt` (modificados) |
| Recursos | `res/values/strings.xml` · `res/values-es/strings.xml` (claves nuevas de la ficha; se retiran tres pluralizaciones que ya no se usan) |
| Pruebas | `test/.../core/util/MoneyFormatTest.kt` (nuevo) · `test/.../search/presentation/ProfessionalSearchScreenLargeFontTest.kt` · `test/.../profile/presentation/PublicProfileScreenLargeFontTest.kt` (modificados) |
| Documentos | `docs/decisions.md` · `docs/design-system.md` (secciones 1, 4 y 5) · `.claude/rules/glosario.md` · `plan.md` (esta sección) |

### Rediseño de las dos pantallas de revisión, 2026-10-08

Pedido del autor después del cierre: la cola «se hace muy larga» porque listaba una
tarjeta por documento, y el detalle no permitía decidir varios documentos de una vez.
No es una historia nueva: es la misma HU-09 con sus dos pantallas rehechas, y los cinco
criterios de aceptación siguen cumpliéndose.

**Qué cambió.**

1. **La cola lista personas, no documentos.** Una tarjeta por usuario con nombre, correo
   y cantidad de documentos pendientes, y nada más. La agrupación, el conteo y el orden
   los hace el motor en la vista nueva `document_review_subject_queue`; la vista
   `document_review_queue` se elimina porque ya nadie la consume.
2. **Búsqueda, filtro y orden.** Búsqueda por nombre o correo con espera de 300 ms antes
   de consultar, filtro por rol (todos, pacientes, profesionales) y orden por antigüedad
   en los dos sentidos. Los tres viven en el motor, no en el cliente.
3. **Veredicto en lote.** Casillero de selección por documento y dos botones arriba de la
   lista, «Aprobar seleccionados» y «Rechazar seleccionados», más un «Seleccionar todos».
   Toda la selección viaja en **una sola sentencia**, así que la decide PostgreSQL de
   forma atómica. Los botones de veredicto por documento, debajo del visor, desaparecen.
4. **Un motivo de rechazo compartido** por toda la selección, decisión del autor.
5. **Botón de volver** en las dos pantallas, con el componente nuevo `ScreenHeader`.
   Era la primera pantalla del proyecto con una vuelta explícita.
6. **El documento se abre a pantalla completa**, el 2026-10-09. Pulsar la
   previsualización abre la imagen sobre fondo oscuro con pinza para acercar, arrastre
   para recorrer, doble toque que alterna entre ajustada y 2,5x, y botón de cerrar.
   Sin biblioteca nueva. En el recuadro 4:3, sobre una imagen comprimida a 1600 px, un
   número de matrícula no se lee: el criterio «veo la imagen» se cumplía de forma
   aparente.
7. **La cola es de trabajo pendiente, no de documentos pendientes**, el 2026-10-09.
   Incluye al profesional con todos sus requeridos aprobados que todavía no está
   verificado, y la tarjeta dice qué se le pide: «Listo para verificar», la cantidad de
   documentos pendientes, o las dos cosas. Cierra un hueco real que encontró el autor
   recorriendo el circuito: **el botón «Verificar profesional» vive solo en el
   expediente, y la cola no listaba a nadie cuyos documentos estuvieran todos
   aprobados**, de modo que el acto que cierra la historia quedaba sin puerta en cuanto
   se aprobaba el último documento. Ver «El hueco del 09/10» más abajo.

Las decisiones están razonadas en `docs/decisions.md`, cuatro entradas del 2026-10-08 y
dos del 2026-10-09, con los motivos de lo descartado: agrupar en el cliente, el `or` de
dos columnas para la búsqueda, un motivo por documento, el control segmentado para los
chips, una biblioteca de zoom, la promoción automática al aprobar el último documento y
una pantalla de búsqueda de personas aparte.

**Verificaciones del 2026-10-08.**

- **`supabase db reset` en local aplica las 27 migraciones** desde cero sin un error.
- **Experimento en local sobre la vista nueva:** con un profesional de 5 documentos
  pendientes, una paciente de 2 y un profesional ya aprobado, la cola devuelve **dos
  filas** con los contadores 5 y 2, el aprobado no aparece, los dos órdenes funcionan,
  el filtro por rol discrimina (`held_roles @> '{PROFESSIONAL}'` → solo el profesional)
  y la búsqueda encuentra por apellido y por un fragmento de correo con punto.
- **Seguridad a nivel de fila de la vista (INV-13):** el profesional y la paciente ven
  **solo su propia fila**; el administrador ve las dos. Es lo que da `security_invoker`.
- **Veredicto múltiple:** un solo `update` de tres documentos los deja los tres
  `APPROVED` y **sellados con el uuid del administrador**, el contador de la cola baja
  de 5 a 2, y un profesional que intenta el mismo `update` de varias filas sobre su
  propio expediente recibe `document_review_is_written_by_an_administrator`.
- **`supabase db diff --linked --schema public` → «No schema changes found»** tras
  aplicar la migración al remoto: cero deriva.
- **Las pruebas nuevas se comprobaron por mutación**, cinco mutaciones y cinco fallos
  en la prueba que les corresponde y en ninguna otra:
  pasar el grupo de chips de `FlowRow` a `Row` deja «Más recientes primero» fuera de la
  pantalla y falla `everyQueueCriterionStaysOnScreen`; meter los dos botones de veredicto
  en un `Row` falla `bothSelectionActionsStayOnScreen`; pedir la página siguiente con los
  criterios por omisión falla `theNextPageCarriesTheActiveQuery`; dejar que «Seleccionar
  todos» marque un casillero vacío falla `markingAllSelectsOnlyTheDocumentsThatExist`; y
  quitar la cancelación de la consulta en vuelo falla
  `aLateReplyToTheFormerCriteriaNeverLandsOnTheList`.
- **La aritmética del zoom se comprobó por mutación**, dos mutaciones: quitar el recorte
  del desplazamiento deja arrastrar el documento al vacío y falla
  `theDragStopsAtTheEdgeOfTheEnlargedImage`; quitar los topes de zoom y la proporción del
  desplazamiento rompe cuatro pruebas de `ImageTransformTest`.
- **63 suites, 363 pruebas, 0 fallos, 0 omitidas.** `ktlintCheck`, `staticAnalysis` y
  `assembleDebug` concluyen sin error.
- **Previsualizaciones: miradas y correctas.** Son **veinte**: ocho en la cola, ocho en el
  detalle, dos del componente `ScreenHeader` y dos del visor. Las tres obligatorias
  —claro, oscuro y `fontScale = 2f`— están en las dos pantallas, y hay tres nuevas que no
  existían: la cola sin coincidencias, el detalle con tres documentos marcados y el visor.
  El cierre del 06/10 dio por cumplido ese punto antes de que nadie las abriera; el autor
  las abrió el 2026-10-08 y las dio por correctas, y lo confirmó el 2026-10-15. **El zoom y
  el arrastre solo se comprueban de verdad con los dedos:** la aritmética tiene pruebas, el
  gesto que la alimenta no.

### El hueco del 09/10, y por qué ninguna prueba lo vio

El autor recorrió el circuito completo: subió los cinco documentos con su cuenta de rol
doble y los aprobó con la cuenta administradora. **El profesional no quedó verificado**, su
ficha seguía diciendo que no está publicada, y el panel ya no mostraba nada sobre esa
persona.

Que aprobar documentos no verifique al profesional es a propósito: la promoción es un acto
explícito del administrador, decisión del 2026-10-06, y el mensaje de la ficha es correcto
porque INV-07 solo publica a un profesional `APPROVED` y activo. Lo que faltaba era la
puerta: el botón vive en el expediente, y la cola no listaba a nadie con todos sus
documentos aprobados.

**Lo que esto enseña sobre las pruebas.**
`approvingTheLastRequiredDocumentOffersTheProfessionalApproval` estaba en verde todo este
tiempo, y sigue estándolo. Cubre que la pantalla ofrezca el botón, **no que exista un
camino hasta esa pantalla**: la prueba nunca se va de la pantalla y el administrador sí.
Una prueba de pantalla no es una prueba de recorrido. Es el mismo patrón que la prueba de
interfaz que no ve el texto recortado dentro de su contenedor, y el mismo remedio: alguien
tiene que recorrer el circuito en el dispositivo. Esta vez lo recorrió el autor, y por eso
apareció.

**Verificado en local el 2026-10-09**, sobre cinco personas construidas para el caso:

| Caso | Aparece | Qué dice la tarjeta |
|---|---|---|
| Todos los requeridos aprobados, sin verificar | Sí | Listo para verificar |
| Todos aprobados y el adjunto opcional pendiente | Sí, **una sola tarjeta** | Listo para verificar y 1 pendiente |
| Le falta un documento por aprobar | Sí | 1 pendiente |
| Todos aprobados pero sin tarifa declarada | **No** | Espera su propio perfil, no una revisión |
| Ya verificada | **No** | Nada que hacer |

Y la seguridad de la función `definer`: la profesional lista para verificar, leyendo la
cola, ve **cero filas**, y llamando `professionals_awaiting_verification()` directamente
recibe **cero identificadores**. La administradora ve las tres y recibe los dos.

**En el remoto, tras aplicar la migración:** la cola de la administradora muestra las dos
tarjetas que corresponden, `ledezma aramayo` con 4 documentos pendientes y
`Christian Ledezma Silva` con 0 pendientes y **listo para verificar**. `supabase db diff
--linked --schema public` responde «No schema changes found».

**Recorrido en dispositivo por el autor el 2026-10-09, con las dos cuentas: correcto.**
Se eligió deliberadamente **no** promover por SQL, para que el circuito quedara demostrado
en pantalla y no solo contra la base. El autor subió los cinco documentos con su cuenta de
rol doble, los aprobó con la cuenta administradora, pulsó «Verificar profesional», y
comprobó desde la primera cuenta que **la ficha pública ya se publica**. Verificado además
contra el remoto: `verification_status = APPROVED`, **1 fila en `professional_directory`**,
y la cola de la administradora pasó a mostrar una sola tarjeta —los 4 documentos de HU-07—
porque la persona verificada salió de la cola al no quedar trabajo sobre ella.

Es la primera vez que el circuito completo de confianza se recorre en un dispositivo, y
cierra la cautela que este documento escribió en «Incremento del sprint».

**Revisión de `plan.md` al inicio y al final: realizadas las dos.**

**Archivos creados, modificados o eliminados el 2026-10-08 y el 2026-10-09.**

| Capa | Archivos |
|---|---|
| Migraciones | `supabase/migrations/20261008120000_group_review_queue_by_subject.sql` · `supabase/migrations/20261009120000_queue_includes_professionals_awaiting_verification.sql` (nuevas, aplicadas en local y en el remoto) |
| Dominio | `domain/model/{PendingReviewSubject, PendingReviewQuery}.kt` · `domain/usecase/{GetPendingReviewSubjects, ApproveDocuments, RejectDocuments}UseCase.kt` (nuevos) · `domain/model/{DocumentReviewResult, DocumentReviewDossier}.kt` · `domain/repository/IDocumentReviewRepository.kt` (modificados) · `domain/model/PendingDocumentReview.kt` · `domain/usecase/{GetPendingDocumentReviews, ApproveDocument, RejectDocument}UseCase.kt` (eliminados) |
| Datos | `data/model/DocumentReviewDto.kt` · `data/mapper/DocumentReviewMapper.kt` · `data/datasource/SupabaseVerificationDataSource.kt` · `data/repository/DocumentReviewRepository.kt` (modificados) |
| Presentación | `presentation/{DocumentReviewQueueViewModel, DocumentReviewQueueScreen, DocumentReviewViewModel, DocumentReviewScreen, VerificationLabels}.kt` (modificados) |
| Interfaz compartida | `ui/components/ScreenHeader.kt` (nuevo) |
| Visor a pantalla completa | `features/verification/presentation/FullScreenDocumentImage.kt` (nuevo, con `ImageTransform` y su aritmética) · `test/.../presentation/ImageTransformTest.kt` (nuevo) |
| Inyección y navegación | `di/VerificationModule.kt` · `navigation/{Routes, SaludEnCasaNavHost}.kt` (la ruta del detalle deja de llevar el tipo de documento) |
| Recursos | `res/values/strings.xml` · `res/values-es/strings.xml` (claves de búsqueda, filtro, orden y selección; dos plurales nuevos; `cd_back_button`, `cd_select_document`, `cd_expand_document_image`, `cd_close_full_image` y `review_queue_ready_to_verify`; se retiran `review_action_approve`, `review_action_reject` y `review_queue_item_submitted`) |
| Pruebas | `test/.../verification/FakeDocumentReviewRepository.kt` · `presentation/{DocumentReviewQueueViewModelTest, DocumentReviewViewModelTest, DocumentReviewScreensLargeFontTest}.kt` · `data/mapper/DocumentReviewMapperTest.kt` · `ui/LargeFont.kt` (ayudante `plural`) (modificados) · `domain/usecase/{GetPendingReviewSubjects, RejectDocuments}UseCaseTest.kt` (nuevos, reemplazan a los de nombre singular) |
| Reglas y documentos | `.claude/rules/glosario.md` · `.claude/rules/testing.md` (la trampa del `LazyColumn`) · `docs/decisions.md` (cuatro entradas del 2026-10-08 y dos del 2026-10-09) · `plan.md` (esta sección) |

## Incremento del sprint

Una prueba de humo con un botón que autentica con Google, crea el perfil en la
base de datos y muestra el identificador obtenido. Es código desechable: se
elimina en HU-01.

## Retrospectiva

**Se cierra en blanco, por decisión del autor el 2026-10-15.** Este sprint no
tuvo un tramo de fechas ni una fecha de corte: empezó con el proyecto y recibió
trabajo de fundación durante meses, hasta HT-21. Una retrospectiva escrita hoy
sobre ese tramo no diría qué cambiar en el siguiente, que es para lo que sirve.
Lo que cada historia técnica aprendió está en su propia sección, y las
retrospectivas que sí miden —Sprint 1 en adelante— llevan su tramo de fechas y su
velocidad.

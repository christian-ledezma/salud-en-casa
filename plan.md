# plan.md — Salud en Casa

Estado de trabajo del proyecto. Léelo al inicio de cada sesión.

## Objetivo del producto

Una aplicación Android que permita a un paciente encontrar un profesional de salud
verificado cerca de su domicilio, negociar la tarifa, coordinar la atención,
registrar el pago y calificar el servicio. Y que permita al profesional
independiente acceder a trabajo sin depender de su promoción personal.

## Cómo usar este archivo

- Trabaja únicamente el **sprint activo**. No comiences el siguiente.
- Dentro del sprint, trabaja **una historia a la vez**, en el orden listado.
- Cada historia es un **incremento vertical**: migración si corresponde, dominio,
  datos, presentación y pruebas. Deja algo que un usuario pueda usar.
- Una historia está terminada cuando cumple la Definición de Terminado.
- Al cerrar un sprint, registra las decisiones no evidentes en `docs/decisions.md`
  y completa la retrospectiva.

Leyenda de estado: `[ ]` no iniciado, `[~]` en curso, `[x]` terminado.

---

## Marco SCRUM aplicado

Proyecto académico de un solo desarrollador. Los roles de SCRUM se cumplen de
forma reducida y así se documenta.

| Elemento | Cómo se aplica aquí |
|---|---|
| **Product Owner** | El autor, con la validación del docente tutor y de la contraparte del negocio |
| **Scrum Master** | El autor. Las ceremonias se autoadministran |
| **Equipo de desarrollo** | El autor |
| **Duración del sprint** | 2 semanas |
| **Product Backlog** | El índice de historias de este archivo, ordenado por dependencia y valor |
| **Sprint Backlog** | Las historias del sprint activo, con sus tareas técnicas |
| **Sprint Planning** | Al inicio de cada sprint: seleccionar historias y descomponer en tareas |
| **Daily** | Registro breve diario de avance y obstáculos |
| **Sprint Review** | Demostración del incremento al final de cada sprint |
| **Retrospectiva** | Qué funcionó, qué no, qué cambiar. Se anota al cierre de cada sprint |
| **Incremento** | Cada sprint entrega funcionalidad usable, nunca una capa horizontal |

### Estimación

Puntos de historia en escala Fibonacci (1, 2, 3, 5, 8, 13). La velocidad no se
supone: se **mide** al terminar el Sprint 1 y se ajusta la planificación de los
siguientes con el valor real. La capacidad inicial estimada es de **20 a 25 puntos
por sprint**; si la velocidad medida resulta menor, la historia de menor prioridad
se traslada al sprint siguiente. Ese traslado no es un fracaso: es el mecanismo
de ajuste de SCRUM y debe quedar documentado en la retrospectiva.

### Diseño previo a cada sprint

Antes de programar las pantallas de un sprint, se prototipan **solo las de ese
sprint**. No se prototipa la aplicación completa por adelantado: el diseño de un
flujo se beneficia de lo aprendido construyendo el anterior, y prototipar seis
flujos en el Sprint 0 es trabajo que se rehace.

El prototipado es la primera actividad del Sprint Planning y no bloquea el cierre
del sprint anterior.

### Definición de Terminado

Una historia está terminada cuando **todo** lo siguiente se cumple:

1. Sus criterios de aceptación están verificados y marcados.
2. La migración correspondiente está aplicada y la seguridad a nivel de fila
   habilitada en toda tabla nueva.
3. Los casos de uso tienen prueba unitaria, y las pruebas obligatorias que
   `.claude/rules/testing.md` asigna a la característica existen.
4. **La suite completa pasa. Sin pruebas deshabilitadas, ignoradas ni comentadas.**
5. `./gradlew build` y `./gradlew test` concluyen sin error.
6. El análisis estático no reporta hallazgos nuevos.
7. **Ningún texto visible escrito en el código.** Todas las claves usadas existen
   en `values-es/` y en `values/`.
8. **Ningún color, medida ni tipografía escrita en una pantalla.** Todo desde el tema.
9. La pantalla se ve correcta en esquema claro y oscuro, y con el tamaño de fuente
   del sistema al 200 %.
10. Los cuatro estados de la pantalla están resueltos: cargando, vacío, con
    contenido y error.
11. La funcionalidad se demostró manualmente en un dispositivo real.
12. Las decisiones no evidentes están registradas en `docs/decisions.md`.

> **Sobre el punto 4.** Una prueba en rojo no se apaga. O el código está mal y se
> corrige, o la prueba expresaba mal la regla y se corrige la prueba explicando por
> qué. Una prueba intermitente es un defecto, no una molestia.

> **Sobre el control de versiones.** El agente **nunca ejecuta comandos de git**.
> Al terminar una historia informa qué archivos creó o modificó, y el autor decide
> qué versiona.

---

## Índice de sprints

| # | Sprint | Objetivo específico | Puntos | Estado |
|---|---|---|---|---|
| 0 | Fundación técnica | 1 · Arquitectura | — | `[~]` |
| 1 | Ingreso e identidad | 2 · Perfiles | 21 | `[ ]` |
| 2 | Perfiles y ubicación | 2 · Perfiles | 24 | `[ ]` |
| 3 | Verificación de usuarios | 2 · Perfiles | 21 | `[ ]` |
| 4 | Catálogo y búsqueda por cercanía | 3 · Coordinación | 23 | `[ ]` |
| 5 | Solicitudes de atención | 3 · Coordinación | 24 | `[ ]` |
| 6 | Negociación de tarifas | 3 · Coordinación | 26 | `[ ]` |
| 7 | Comunicación y ciclo del servicio | 3 · Coordinación | 23 | `[ ]` |
| 8 | Confirmación mutua de pagos | 4 · Pagos | 21 | `[ ]` |
| 9 | Calificación e historial | 5 · Evaluación | 22 | `[ ]` |
| 10 | Validación con usuarios reales | 6 · Pruebas | — | `[ ]` |

**Primer incremento demostrable:** Sprint 1. **Ciclo completo de negocio de punta a
punta:** Sprint 8. **Sistema completo:** Sprint 9.

---

## Decisiones tomadas al planificar

Ya acordadas. Su razonamiento completo está en `docs/decisions.md`.

- **El orden de los sprints sigue el ciclo de vida de una atención.** Identidad,
  ubicación, búsqueda, solicitud, negociación, comunicación, pago, calificación.
  No hay forma de invertirlo sin romper dependencias.

- **Identificadores en inglés, documentos en español.** Todo nombre de clase,
  tabla, columna y enumerado va en inglés. `plan.md`, `requirements.md` y
  `decisions.md` van en español porque son evidencia académica.

- **Android exclusivamente, un solo módulo Gradle.** No hay multiplataforma. El
  alcance del proyecto declara Android, y toda complejidad que no sirva a ese
  alcance es una carga que además hay que defender. La estructura es la de
  paquetes por característica sobre un único módulo `:app`.

- **La regla de dependencia se sostiene por convención y análisis estático.** Al
  no haber módulos separados, nada impide técnicamente que la capa de presentación
  importe la de datos. La verificación es una prueba transversal que revisa las
  importaciones de `domain/` y una regla del análisis estático.

- **Sin base de datos local.** Ningún requisito exige operación sin conexión.
  DataStore cubre la sesión y las preferencias. Si aparece la necesidad, primero
  se agrega el requisito y luego la implementación.

- **Solo dependencias en versión estable.** Ninguna `alpha`, `beta`, `rc` ni
  `SNAPSHOT`. El proyecto se defiende dentro de varios meses y una API en
  desarrollo puede cambiar entre sprints.

- **Seguridad a nivel de fila en la misma migración que crea la tabla.** Nunca
  después. Postergarla lleva a habilitarla cuando media aplicación ya depende de
  no tenerla, y el resultado es desactivarla «temporalmente».

- **La búsqueda por cercanía se resuelve con PostGIS.** Nunca con un servicio
  externo de búsqueda por proximidad: con 200 búsquedas diarias, ese servicio
  costaría más que toda la infraestructura del proyecto.

- **El detalle de la atención vive en la conversación de la solicitud.** El
  registro clínico estructurado queda fuera de alcance (FA-01). La condición para
  que su incorporación posterior sea aditiva es no dispersar ese detalle en campos
  improvisados de otras tablas.

- **Sin pasarela de pagos.** Confirmación mutua con comisión acumulada. El modelo
  ya contempla `method`, `provider` y `external_reference` para incorporar una
  pasarela sin cambio de esquema (FA-02).

- **Sin publicación en la tienda en esta fase.** La distribución para la
  validación del Sprint 10 se hace por App Distribution, lo que evita el trámite
  de cuenta de organización (FA-03).

- **El administrador no accede a la conversación.** Su política excluye la tabla
  de mensajes, porque ahí reside el detalle clínico de la atención (RN-11, INV-12).

- **El agente nunca ejecuta comandos de git.** El autor gestiona el repositorio de
  forma manual. Al cerrar una historia, el agente informa qué archivos creó o
  modificó.

- **Internacionalización desde el primer día.** Ningún texto visible se escribe en
  el código. La estructura de recursos existe desde el Sprint 0, de modo que
  agregar un idioma sea traducir un archivo y no recorrer el proyecto.

- **La suite de pruebas pasa completa en cada iteración.** Sin pruebas
  deshabilitadas ni ignoradas. Las pruebas se escriben por el error que atrapan,
  no por la cobertura que producen.

---

# Sprint 0 — Fundación técnica

**Estado:** `[~]` en curso.

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

### HT-04 · Esquema de base de datos `[ ]`

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
- [ ] Los índices espaciales aparecen en el plan de ejecución de la consulta de
      cercanía — **solo verificable con datos.** Con las tablas vacías el
      planificador elige recorrido secuencial por ser más barato, así que el
      plan no prueba nada. Se verifica en HU-11, cuyo criterio ya exige medir con
      quinientos profesionales cargados
- [x] Las ocho migraciones se aplican sobre el proyecto remoto con
      `npx supabase db push`, previo `--dry-run`
- [ ] ~~`supabase db reset` reconstruye la base completa sin errores~~ —
      **aplazado.** El entorno local con Docker queda fuera de alcance en esta
      etapa. Es el criterio que demuestra que el esquema se reconstruye desde
      cero, así que se recupera cuando exista entorno local o proyecto de
      producción. Registrado en `docs/decisions.md`
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
elimina al implementar HU-01.

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

**Deuda reconocida.** El modelo de vista consume un cliente de infraestructura en
lugar de un caso de uso, que no es la forma que pide la arquitectura. Se acepta
solo porque esta pantalla es andamiaje sin dominio detrás: HU-01 la reemplaza por
el flujo real con su caso de uso. Queda anotado para que no se copie el patrón.

**Requisitos:** RF-01.1, RF-01.2, RF-01.3, RF-01.6, RNF-06.

### HT-06 · Sistema de diseño `[ ]`

Implementación del tema a partir de `docs/design-system.md`: paleta con ambos
esquemas, tipografía Inter con la escala definida, formas, espaciado y colores
semánticos de estado como extensión del tema.

- [ ] Ambos esquemas, claro y oscuro, definidos y verificados
- [ ] El contraste de cada combinación de texto cumple los criterios de accesibilidad
- [ ] El ámbar no se usa como color de texto sobre fondo claro
- [ ] Componentes base disponibles: tarjeta de profesional, chip de especialidad,
      distintivo de calificación, selector de fecha, control segmentado, campo de
      formulario, botón principal y barra de navegación flotante
- [ ] Cada componente tiene previsualización en ambos esquemas

**Restos de la plantilla que contradicen el sistema de diseño**

- [ ] **`dynamicColor` en `false`, o el parámetro eliminado.** Es el punto más
      importante de esta historia: el color dinámico de Material You reemplaza la
      paleta completa en cualquier dispositivo desde Android 12, lo que anularía
      el tema, las dos correcciones de contraste registradas en `decisions.md` y
      el punto 9 de la Definición de Terminado
- [ ] El tema deja de heredar de `Theme.Material.Light.NoActionBar`, que fija el
      esquema claro
- [ ] Existe `res/values-night/` con el esquema oscuro
- [ ] La paleta de la plantilla queda reemplazada por la de `docs/design-system.md`
- [ ] La tipografía pasa del único estilo con la familia por omisión a los diez
      estilos con Inter de `docs/design-system.md`

**Requisitos:** RNF-07.

> El prototipado en Figma **no forma parte de esta historia**. Se prototipan las
> pantallas de cada sprint al planificarlo, no las seis del producto por
> adelantado. Ver «Diseño previo a cada sprint».

### HT-07 · Estructura de internacionalización `[ ]`

Recursos de cadenas con `values/` como reserva y `values-es/` como idioma inicial.
Convención de claves aplicada. Regla de análisis estático que detecte texto escrito
en el código.

- [ ] Existen `app/src/main/res/values/strings.xml` y `values-es/strings.xml`
- [ ] La regla de análisis estático señala cualquier texto visible escrito en el código
- [ ] Los tipos de error del dominio transportan tipos, nunca frases
- [ ] La prueba `everyStringKeyUsedInCodeExistsInAllLocales` existe y pasa

**Restos de la plantilla que contradicen la regla**

- [ ] El componible `Greeting` y su texto escrito en el código quedan eliminados.
      La regla de análisis estático lo marcaría el primer día
- [ ] Las pruebas de ejemplo de la plantilla quedan eliminadas. Son exactamente el
      tipo que `.claude/rules/testing.md` describe como «no aporta», y la suite
      debe arrancar limpia

**Requisitos:** RNF-07.

### HT-08 · Integración continua, análisis estático y monitoreo `[ ]`

Flujo automático que se ejecuta en cada envío de código y en cada solicitud de
fusión, más el monitoreo de errores en producción.

Etapas del flujo:

| Etapa | Acción |
|---|---|
| Preparación | Descarga del código, JDK 17, caché de dependencias |
| Secretos | Generación de `local.properties` desde los secretos del repositorio |
| Análisis estático | ktlint y las reglas propias del proyecto |
| Pruebas | `./gradlew test` |
| Compilación | `./gradlew assembleDebug` |

- [ ] Las cinco etapas concluyen correctamente en un envío de prueba
- [ ] El análisis estático incluye las reglas de arquitectura: `domain/` sin
      importaciones de plataforma, `presentation/` sin importaciones de `data/`
- [ ] El análisis estático incluye la regla de texto escrito en el código
- [ ] La prueba `domainLayerHasNoPlatformImports` existe y pasa
- [ ] Un error provocado deliberadamente aparece en el panel de monitoreo

**Requisitos:** RNF-06, RNF-09.

> Estas reglas del análisis estático sustituyen la garantía que antes daba el
> compilador al separar módulos. Sin ellas, la regla de dependencia queda
> únicamente en la disciplina de quien programa.

### HT-09 · Documentación de arquitectura `[ ]`

Diagramas en `docs/architecture/`, escritos en **Mermaid dentro de archivos
Markdown**, no como imágenes sueltas. Así se versionan, se comparan entre
revisiones y se renderizan en el repositorio.

| Archivo | Contenido |
|---|---|
| `docs/architecture/components.md` | Aplicación, servicios externos y sus relaciones |
| `docs/architecture/packages.md` | Estructura de paquetes y la regla de dependencia |
| `docs/architecture/auth-sequence.md` | Secuencia completa de autenticación |
| `docs/architecture/deployment.md` | Dónde se ejecuta cada componente |

- [ ] Los cuatro diagramas están escritos y renderizan correctamente
- [ ] `docs/decisions.md` está iniciado con las decisiones ya tomadas
- [ ] El archivo `README.md` permite a una persona ajena levantar el proyecto desde cero

## Incremento del sprint

Una prueba de humo con un botón que autentica con Google, crea el perfil en la
base de datos y muestra el identificador obtenido. Es código desechable: se
elimina en HU-01.

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 1 — Ingreso e identidad

**Objetivo del sprint.** Una persona ingresa con su cuenta de Google, elige si es
paciente o profesional, y completa sus datos básicos.

**Objetivo específico.** 2 · Desarrollar la gestión de perfiles de usuarios.

**Puntos:** 21.

### HU-01 · Ingresar con cuenta de Google `[ ]` — 8 puntos

> Como **persona que necesita atención domiciliaria**, quiero **ingresar con mi
> cuenta de Google sin crear una contraseña**, para **empezar a usar la
> aplicación sin fricción**.

**Criterios de aceptación**

- [ ] Dado que tengo una cuenta de Google en el dispositivo, cuando pulso
      «Continuar con Google», entonces aparece el selector nativo del sistema.
- [ ] Dado que elijo una cuenta, cuando la autenticación concluye, entonces
      accedo a la aplicación sin escribir credenciales.
- [ ] Dado que cancelo el selector, cuando vuelvo a la pantalla, entonces no se
      muestra ningún error y puedo reintentar.
- [ ] Dado que no hay cuentas en el dispositivo, cuando pulso el botón, entonces
      se me indica cómo agregar una.
- [ ] Dado que ya ingresé antes, cuando abro la aplicación, entonces entro
      directamente sin volver a autenticarme.
- [ ] Dado que cierro sesión, cuando vuelvo a abrir, entonces se me pide ingresar.

**Requisitos:** RF-01.1, RF-01.2, RF-01.6, RF-01.7.

**Tareas técnicas.** Objetos de valor `Email`, `PhoneNumber`, `PersonName` con sus
pruebas · `IAuthRepository` en dominio y su implementación en datos · casos de uso
de ingreso, cierre de sesión y consulta de sesión · `SupabaseAuthDataSource` ·
integración de Credential Manager con nonce en la capa de presentación · pantalla de
bienvenida y modelo de vista · pantalla de arranque que decide destino según sesión.

### HU-02 · Elegir mi rol `[ ]` — 5 puntos

> Como **usuario que ingresa por primera vez**, quiero **indicar si soy paciente o
> profesional de salud**, para **que la aplicación me muestre lo que me corresponde**.

**Criterios de aceptación**

- [ ] Dado que ingreso por primera vez, cuando la sesión se establece, entonces se
      me pide elegir entre paciente y profesional.
- [ ] Dado que elijo un rol, cuando confirmo, entonces se crea mi registro
      específico y no se me vuelve a preguntar.
- [ ] Dado que ya tengo rol, cuando vuelvo a ingresar, entonces voy directo a la
      pantalla principal de mi rol.
- [ ] Dado que abandono la aplicación sin elegir, cuando vuelvo a entrar, entonces
      se me vuelve a pedir la elección.
- [ ] El rol de administrador nunca aparece como opción.

**Requisitos:** RF-01.3, RF-01.4, RF-01.5.

### HU-03 · Completar mis datos básicos `[ ]` — 8 puntos

> Como **usuario registrado**, quiero **completar y editar mi nombre, teléfono y
> fotografía**, para **que la contraparte sepa con quién trata**.

**Criterios de aceptación**

- [ ] Dado que elegí mi rol, cuando llego a mi perfil, entonces veo el nombre y la
      fotografía que trajo Google, editables.
- [ ] Dado que escribo un teléfono con formato inválido, cuando intento guardar,
      entonces se me indica el error y no se guarda.
- [ ] Dado que soy paciente, cuando abro mi perfil, entonces puedo registrar fecha
      de nacimiento, contacto de emergencia y notas relevantes.
- [ ] Dado que guardo cambios, cuando vuelvo a abrir la aplicación, entonces los
      cambios persisten.
- [ ] Dado que la carga falla por conexión, cuando reintento, entonces se muestra
      un mensaje que explica qué ocurrió y cómo reintentar.

**Requisitos:** RF-02.1.

**Tareas técnicas.** `IProfileRepository` y casos de uso · `SupabaseProfileDataSource`
· pantalla de perfil de paciente con validación por objetos de valor · manejo de
error de red con tipo de error, no con frase.

## Incremento del sprint

Un usuario nuevo ingresa con Google, elige ser paciente, completa su perfil y lo
ve persistido al reabrir la aplicación. **Primera demostración a la contraparte.**

## Retrospectiva

_Completar al cerrar el sprint. Registrar aquí la velocidad medida._

---

# Sprint 2 — Perfiles y ubicación

**Objetivo del sprint.** El profesional publica su perfil profesional completo y
ambos roles registran sus direcciones georreferenciadas.

**Objetivo específico.** 2 · Desarrollar la gestión de perfiles de usuarios.

**Puntos:** 24.

### HU-04 · Publicar mi perfil profesional `[ ]` — 8 puntos

> Como **profesional de salud**, quiero **declarar mi especialidad, experiencia,
> tarifa base y radio de cobertura**, para **que los pacientes sepan qué ofrezco y
> a qué precio**.

**Criterios de aceptación**

- [ ] Dado que soy profesional, cuando abro mi perfil, entonces puedo declarar
      tipo, especialidad, biografía, años de experiencia, tarifa base y radio.
- [ ] Dado que ingreso una tarifa negativa o cero, cuando guardo, entonces se
      rechaza con un mensaje claro.
- [ ] Dado que ingreso un radio fuera del rango de 1 a 50 kilómetros, cuando
      guardo, entonces se rechaza.
- [ ] Dado que guardo mi perfil, cuando lo consulto como paciente, entonces veo la
      información publicada.
- [ ] Dado que activo o desactivo mi disponibilidad inmediata, cuando cambio el
      interruptor, entonces el estado se refleja de inmediato.

**Requisitos:** RF-02.2, RF-02.5, RF-02.6.

**Tareas técnicas.** Objetos de valor `AmountBob` y `CoverageRadiusKm` con sus
pruebas · repositorio y casos de uso de perfil profesional · pantalla de perfil
profesional · pantalla de perfil público.

### HU-05 · Registrar mi dirección en el mapa `[ ]` — 13 puntos

> Como **usuario**, quiero **marcar mi dirección sobre un mapa**, para **que la
> atención llegue al lugar correcto**.

**Criterios de aceptación**

- [ ] Dado que abro el registro de dirección, cuando concedo el permiso de
      ubicación, entonces el mapa se centra en mi posición actual.
- [ ] Dado que deniego el permiso, cuando abro el mapa, entonces puedo buscar y
      marcar la dirección manualmente sin que la aplicación falle.
- [ ] Dado que arrastro el marcador, cuando lo suelto, entonces la dirección
      textual se actualiza según las coordenadas.
- [ ] Dado que guardo una dirección, cuando la vuelvo a abrir, entonces no se
      consulta nuevamente el servicio de geocodificación.
- [ ] Dado que guardo una dirección, cuando consulto la base de datos, entonces la
      ubicación está almacenada como punto geográfico.
- [ ] Dado que registro una referencia textual, cuando la contraparte vea la
      atención, entonces la referencia estará disponible.

**Requisitos:** RF-03.1, RF-03.2, RF-03.3, RF-03.6.

**Tareas técnicas.** Objeto de valor `Coordinate` con pruebas de rango ·
`ILocationRepository` y casos de uso · fuente de datos con caché de geocodificación
· solicitud de permisos con justificación previa · pantalla de mapa con marcador
arrastrable.

### HU-06 · Administrar mis direcciones `[ ]` — 3 puntos

> Como **usuario**, quiero **tener varias direcciones y marcar una como
> principal**, para **solicitar atención en distintos lugares**.

**Criterios de aceptación**

- [ ] Dado que tengo varias direcciones, cuando abro la lista, entonces las veo
      con su alias y su referencia.
- [ ] Dado que marco una como principal, cuando marco otra, entonces la anterior
      deja de serlo automáticamente.
- [ ] Dado que elimino una dirección, cuando confirmo, entonces desaparece de la lista.
- [ ] Dado que soy profesional, cuando no tengo dirección principal, entonces no
      aparezco en búsquedas.

**Requisitos:** RF-03.4, RF-03.5, RN-02.

## Incremento del sprint

Un profesional publica su perfil con tarifa y radio, y registra su domicilio sobre
el mapa. La consulta de cercanía en la base de datos ya lo encuentra.

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 3 — Verificación de usuarios

**Objetivo del sprint.** Ambos roles someten sus documentos a verificación y el
administrador los aprueba o rechaza.

**Objetivo específico.** 2 · Desarrollar la gestión de perfiles de usuarios.

**Puntos:** 21.

### HU-07 · Cargar mis documentos de verificación `[ ]` — 8 puntos

> Como **usuario**, quiero **subir mis documentos**, para **acreditar mi identidad
> y generar confianza en la contraparte**.

**Criterios de aceptación**

- [ ] Dado que soy paciente, cuando abro la verificación, entonces se me piden
      documento de identidad y fotografía de rostro.
- [ ] Dado que soy profesional, cuando abro la verificación, entonces se me piden
      además título profesional y matrícula o carnet de estudiante.
- [ ] Dado que capturo una imagen, cuando la envío, entonces se comprime antes de
      subirse.
- [ ] Dado que subo un documento, cuando otro usuario consulta mi perfil, entonces
      no puede acceder al archivo.
- [ ] Dado que la carga falla por conexión, cuando recupero la señal, entonces
      puedo reintentar sin volver a capturar.

**Requisitos:** RF-04.1, RF-04.2, RF-04.3.

**Tareas técnicas.** Contenedor privado con políticas · `IVerificationRepository`
y casos de uso · captura desde cámara y galería · compresión previa · pantalla de
carga con estado por documento.

### HU-08 · Conocer el estado de mi verificación `[ ]` — 3 puntos

> Como **usuario**, quiero **ver si mis documentos fueron aprobados o rechazados**,
> para **saber si puedo operar y qué debo corregir**.

**Criterios de aceptación**

- [ ] Dado que subí mis documentos, cuando abro la verificación, entonces veo el
      estado de cada uno.
- [ ] Dado que un documento fue rechazado, cuando lo consulto, entonces veo el
      motivo y puedo volver a subirlo.
- [ ] Dado que mi verificación fue aprobada, cuando la contraparte ve mi perfil,
      entonces aparece el distintivo de verificado.
- [ ] Dado que soy profesional sin verificación aprobada, cuando un paciente busca,
      entonces no aparezco en los resultados.

**Requisitos:** RF-04.5, RF-04.6, RN-01, INV-07.

### HU-09 · Revisar documentos pendientes `[ ]` — 10 puntos

> Como **administrador**, quiero **revisar los documentos cargados y aprobarlos o
> rechazarlos**, para **garantizar que solo participen personas acreditadas**.

**Criterios de aceptación**

- [ ] Dado que hay documentos pendientes, cuando abro el panel, entonces los veo
      ordenados por antigüedad.
- [ ] Dado que abro un documento, cuando lo visualizo, entonces veo la imagen y los
      datos del usuario.
- [ ] Dado que apruebo todos los documentos de un usuario, cuando confirmo,
      entonces su perfil pasa a verificado.
- [ ] Dado que rechazo un documento, cuando indico el motivo, entonces el usuario
      lo ve en su aplicación.
- [ ] Dado que soy administrador, cuando intento leer una conversación, entonces
      el acceso se deniega.

**Requisitos:** RF-04.4, RN-11, INV-12.

**Tareas técnicas.** Panel administrativo web mínimo · políticas de acceso del
administrador, con exclusión explícita de `messages` · prueba automatizada que
verifique esa exclusión.

## Incremento del sprint

Un profesional carga su título, el administrador lo aprueba, y el profesional
pasa a ser visible en las búsquedas. **El circuito de confianza queda cerrado.**

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 4 — Catálogo y búsqueda por cercanía

**Objetivo del sprint.** El paciente encuentra profesionales verificados cerca de
su domicilio, filtrados por el tipo de atención que necesita.

**Objetivo específico.** 3 · Comunicación, coordinación y agenda.

**Puntos:** 23.

### HU-10 · Declarar los servicios que presto `[ ]` — 5 puntos

> Como **profesional**, quiero **indicar qué tipos de atención presto y a qué
> precio**, para **aparecer en las búsquedas correctas**.

**Criterios de aceptación**

- [ ] Dado que abro mis servicios, cuando consulto el catálogo, entonces veo los
      tipos de atención disponibles.
- [ ] Dado que selecciono un tipo, cuando fijo su precio de referencia, entonces
      queda asociado a mi perfil.
- [ ] Dado que intento agregar dos veces el mismo tipo, cuando guardo, entonces se
      rechaza.
- [ ] Dado que no declaro ningún servicio, cuando un paciente filtra por tipo,
      entonces no aparezco.

**Requisitos:** RF-02.3, RF-05.1, RF-05.2.

### HU-11 · Buscar profesionales cerca de mi domicilio `[ ]` — 13 puntos

> Como **paciente**, quiero **ver qué profesionales hay cerca de mi dirección**,
> para **elegir a quién solicitar la atención**.

**Criterios de aceptación**

- [ ] Dado que tengo una dirección registrada, cuando abro la búsqueda, entonces
      veo los profesionales dentro del radio sobre un mapa.
- [ ] Dado que cambio a vista de lista, cuando la consulto, entonces veo distancia,
      tarifa base y reputación de cada uno.
- [ ] Dado que filtro por tipo de servicio, cuando aplico el filtro, entonces solo
      aparecen quienes lo declararon.
- [ ] Dado que filtro por disponibilidad inmediata, cuando aplico el filtro,
      entonces solo aparecen quienes están disponibles ahora.
- [ ] Los resultados están ordenados por distancia ascendente.
- [ ] Solo aparecen profesionales verificados y activos.
- [ ] Con quinientos profesionales cargados, la búsqueda responde en menos de un segundo.

**Requisitos:** RF-06.1 a RF-06.5, RNF-01, RN-01, RN-02, INV-08.

**Tareas técnicas.** Consumo de la función de cercanía desde la capa de datos
· `IProfessionalSearchRepository` y caso de uso · mapa con marcadores agrupados ·
vista de lista alterna · prueba que verifique el uso del índice espacial.

### HU-12 · Consultar la ficha de un profesional `[ ]` — 5 puntos

> Como **paciente**, quiero **ver el detalle de un profesional antes de
> contactarlo**, para **decidir con información**.

**Criterios de aceptación**

- [ ] Dado que toco un resultado, cuando se abre la ficha, entonces veo biografía,
      especialidad, experiencia, servicios y tarifas.
- [ ] Dado que el profesional tiene calificaciones, cuando abro la ficha, entonces
      veo su promedio, su total de atenciones y los comentarios recibidos.
- [ ] Dado que el profesional está verificado, cuando abro la ficha, entonces veo
      el distintivo, pero nunca el archivo de su título.
- [ ] Los comentarios se cargan paginados.

**Requisitos:** RF-02.6, RF-04.6, RF-12.4, RNF-08.

## Incremento del sprint

Un paciente abre la aplicación y ve en el mapa los fisioterapeutas verificados a
menos de cinco kilómetros de su casa, ordenados por distancia, con su tarifa.
**El diferenciador central del producto queda demostrado.**

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 5 — Solicitudes de atención

**Objetivo del sprint.** El paciente solicita atención en las dos modalidades y el
profesional ve las solicitudes de su zona.

**Objetivo específico.** 3 · Comunicación, coordinación y agenda.

**Puntos:** 24.

### HU-13 · Solicitar atención inmediata `[ ]` — 8 puntos

> Como **paciente con una necesidad urgente**, quiero **publicar una solicitud que
> vean los profesionales cercanos**, para **recibir atención lo antes posible**.

**Criterios de aceptación**

- [ ] Dado que necesito atención, cuando creo una solicitud inmediata, entonces
      indico tipo de servicio, dirección, descripción y presupuesto sugerido.
- [ ] Dado que publico la solicitud, cuando consulto su estado, entonces figura
      como publicada.
- [ ] Dado que publico la solicitud, cuando la consultan los profesionales del
      radio, entonces la ven.
- [ ] Dado que nadie oferta en el plazo definido, cuando el plazo vence, entonces
      la solicitud queda expirada.
- [ ] Dado que la solicitud no fue aceptada, cuando la cancelo, entonces deja de
      estar publicada.

**Requisitos:** RF-07.1, RF-07.2, RF-07.5, RF-07.6, INV-03.

### HU-14 · Ver las solicitudes de mi zona `[ ]` — 8 puntos

> Como **profesional**, quiero **ver en tiempo real las solicitudes publicadas
> dentro de mi radio**, para **ofertar por las que me interesan**.

**Criterios de aceptación**

- [ ] Dado que soy profesional verificado, cuando abro mi bandeja, entonces veo las
      solicitudes publicadas dentro de mi radio de cobertura.
- [ ] Dado que se publica una solicitud nueva en mi radio, cuando estoy en la
      bandeja, entonces aparece sin que yo recargue.
- [ ] Dado que una solicitud es de un tipo que no presto, cuando abro la bandeja,
      entonces no la veo.
- [ ] Dado que una solicitud ya fue aceptada, cuando abro la bandeja, entonces
      desaparece.
- [ ] La bandeja está paginada.

**Requisitos:** RF-07.4, RNF-08, RN-04.

**Tareas técnicas.** Suscripción en tiempo real expuesta como flujo desde el
la capa de datos · filtrado por radio y tipo de servicio.

### HU-15 · Declarar mi disponibilidad horaria `[ ]` — 3 puntos

> Como **profesional**, quiero **indicar en qué franjas horarias atiendo**, para
> **recibir solicitudes agendadas solo cuando puedo**.

**Criterios de aceptación**

- [ ] Dado que abro mi disponibilidad, cuando agrego una franja, entonces indico
      día de la semana, hora de inicio y hora de fin.
- [ ] Dado que la hora de fin es anterior a la de inicio, cuando guardo, entonces
      se rechaza.
- [ ] Dado que declaro mis franjas, cuando un paciente intenta agendar, entonces
      solo puede elegir dentro de ellas.

**Requisitos:** RF-02.4.

### HU-16 · Agendar una cita `[ ]` — 5 puntos

> Como **paciente que no tiene urgencia**, quiero **agendar una atención con un
> profesional específico**, para **coordinar con anticipación**.

**Criterios de aceptación**

- [ ] Dado que abro la ficha de un profesional, cuando elijo agendar, entonces veo
      sus franjas disponibles.
- [ ] Dado que elijo fecha y hora, cuando confirmo, entonces se crea una solicitud
      agendada dirigida a ese profesional.
- [ ] Dado que elijo un horario fuera de su disponibilidad, cuando confirmo,
      entonces se rechaza.
- [ ] Dado que creo la solicitud, cuando el profesional abre su bandeja, entonces
      la ve identificada como agendada.

**Requisitos:** RF-07.3.

## Incremento del sprint

Un paciente publica una solicitud inmediata y el profesional la ve aparecer en su
bandeja sin recargar. Otro paciente agenda una cita para el jueves.

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 6 — Negociación de tarifas

**Objetivo del sprint.** Las partes acuerdan el precio mediante ofertas y
contraofertas, y el acuerdo genera el servicio.

**Objetivo específico.** 3 · Comunicación, coordinación y agenda.

**Puntos:** 26.

### HU-17 · Ofertar por una solicitud `[ ]` — 5 puntos

> Como **profesional**, quiero **proponer un monto por una atención**, para
> **conseguir el trabajo**.

**Criterios de aceptación**

- [ ] Dado que veo una solicitud publicada, cuando emito una oferta, entonces
      indico monto y mensaje.
- [ ] Dado que emito la oferta, cuando el paciente abre la solicitud, entonces la
      ve sin recargar.
- [ ] Dado que la solicitud ya fue aceptada, cuando intento ofertar, entonces se
      rechaza.
- [ ] Dado que no estoy verificado, cuando intento ofertar, entonces se rechaza.
- [ ] Dado que emití una oferta, cuando intento editarla, entonces no es posible.

**Requisitos:** RF-08.1, RN-01, RN-04, RN-05, INV-06.

### HU-18 · Contraofertar `[ ]` — 8 puntos

> Como **paciente**, quiero **responder con otro monto**, para **acordar un precio
> que me convenga**.

**Criterios de aceptación**

- [ ] Dado que recibo una oferta, cuando respondo con otro monto, entonces se crea
      una contraoferta que referencia la anterior.
- [ ] Dado que existe una cadena de ofertas, cuando abro la negociación, entonces
      veo el hilo completo en orden cronológico, con el emisor de cada una.
- [ ] Dado que el profesional responde, cuando estoy en la pantalla, entonces veo
      la respuesta sin recargar.
- [ ] Dado que rechazo una oferta, cuando confirmo, entonces la solicitud sigue
      publicada para otros profesionales.

**Requisitos:** RF-08.2, RF-08.3, RF-08.4.

**Tareas técnicas.** Suscripción en tiempo real de la tabla de ofertas expuesta
como flujo · reconstrucción del hilo mediante la referencia al padre.

### HU-19 · Aceptar una oferta `[ ]` — 8 puntos

> Como **cualquiera de las partes**, quiero **aceptar el monto propuesto**, para
> **cerrar el acuerdo y dar inicio a la atención**.

**Criterios de aceptación**

- [ ] Dado que acepto una oferta, cuando confirmo, entonces se marca aceptada, la
      solicitud pasa a aceptada, se crea el servicio y se crea el pago pendiente.
- [ ] Dado que la operación falla a la mitad, cuando consulto la base de datos,
      entonces no queda ningún registro parcial.
- [ ] Dado que se acepta la oferta, cuando abro la atención, entonces veo los datos
      de contacto de la contraparte, que antes no eran visibles.
- [ ] Dado que se acepta la oferta, cuando consulto el servicio, entonces el monto
      congelado coincide con el aceptado.
- [ ] Dado que la tarifa base del profesional cambia después, cuando consulto el
      servicio, entonces el monto no cambia.

**Requisitos:** RF-08.5, RF-08.6, RN-06, RN-07, INV-04.

**Tareas técnicas.** Función de servidor que ejecute la aceptación en una sola
transacción · prueba de atomicidad.

### HU-20 · Recibir aviso de una solicitud cercana `[ ]` — 5 puntos

> Como **profesional**, quiero **recibir una notificación cuando se publique una
> solicitud cerca de mí**, para **no perder oportunidades por no estar mirando la
> aplicación**.

**Criterios de aceptación**

- [ ] Dado que se publica una solicitud dentro de mi radio y de un tipo que presto,
      cuando ocurre, entonces recibo una notificación con la distancia.
- [ ] Dado que toco la notificación, cuando se abre la aplicación, entonces voy
      directo a esa solicitud.
- [ ] Dado que no estoy verificado o no estoy disponible, cuando se publica una
      solicitud, entonces no me llega notificación.
- [ ] Dado que cambio de dispositivo, cuando ingreso, entonces las notificaciones
      llegan al nuevo.

**Requisitos:** RF-09.4, RF-09.5.

**Tareas técnicas.** Registro y renovación de identificadores de dispositivo ·
función de servidor que localice destinatarios y envíe la notificación.

## Incremento del sprint

Una solicitud publicada dispara una notificación a los profesionales cercanos;
uno oferta, el paciente contraoferta, el profesional acepta y el servicio queda
creado con su pago pendiente. **El mecanismo distintivo del producto está completo.**

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 7 — Comunicación y ciclo del servicio

**Objetivo del sprint.** Las partes se comunican, el profesional registra lo
realizado y la atención se completa.

**Objetivo específico.** 3 · Comunicación, coordinación y agenda.

**Puntos:** 23.

### HU-21 · Conversar sobre la atención `[ ]` — 10 puntos

> Como **cualquiera de las partes**, quiero **intercambiar mensajes dentro de la
> solicitud**, para **coordinar los detalles sin salir de la aplicación**.

**Criterios de aceptación**

- [ ] Dado que la oferta fue aceptada, cuando abro la atención, entonces puedo
      enviar y recibir mensajes.
- [ ] Dado que la contraparte envía un mensaje, cuando estoy en la conversación,
      entonces lo veo aparecer sin recargar.
- [ ] Dado que leo un mensaje, cuando la contraparte consulta, entonces ve que fue leído.
- [ ] Dado que no estoy en la aplicación, cuando recibo un mensaje, entonces me
      llega una notificación.
- [ ] Dado que soy administrador, cuando intento leer una conversación, entonces
      el acceso se deniega.
- [ ] La conversación se carga paginada, del mensaje más reciente hacia atrás.

**Requisitos:** RF-09.1, RF-09.2, RF-09.5, RN-11, INV-12, RNF-08.

### HU-22 · Registrar lo realizado en la atención `[ ]` — 3 puntos

> Como **profesional**, quiero **dejar constancia de lo que hice durante la
> atención**, para **que el paciente y yo tengamos el registro**.

**Criterios de aceptación**

- [ ] Dado que la atención está en curso, cuando registro lo realizado, entonces
      queda en la conversación de la solicitud.
- [ ] Dado que la atención finalizó, cuando consulto el historial, entonces accedo
      a esa conversación.
- [ ] Dado que soy estudiante, cuando registro, entonces puedo describir lo
      realizado pero la aplicación no ofrece emitir diagnóstico.

**Requisitos:** RF-09.3, RN-03, FA-01.

> **Nota de alcance.** El registro clínico estructurado está fuera de alcance
> (FA-01). Esta historia deja el detalle en la conversación, de modo deliberado.
> No crear campos improvisados en otras tablas para este propósito: eso
> comprometería la incorporación aditiva del registro clínico en una fase posterior.

### HU-23 · Marcar el avance de la atención `[ ]` — 5 puntos

> Como **profesional**, quiero **indicar que llegué y que terminé**, para **que el
> paciente conozca el estado y se habilite el pago**.

**Criterios de aceptación**

- [ ] Dado que la atención está asignada, cuando marco mi llegada, entonces pasa a
      en curso y el paciente lo ve.
- [ ] Dado que la atención está en curso, cuando la marco completada, entonces se
      habilita la confirmación de pago.
- [ ] Dado que intento marcar llegada en una atención cancelada, cuando lo intento,
      entonces se rechaza.
- [ ] Dado que la atención se completó, cuando consulto sus tiempos, entonces
      figuran el inicio y el fin.

**Requisitos:** RF-10.1, RF-10.2, RF-10.4.

**Tareas técnicas.** Máquina de estados modelada como jerarquía sellada · prueba
de las transiciones válidas e inválidas.

### HU-24 · Cancelar una atención `[ ]` — 5 puntos

> Como **cualquiera de las partes**, quiero **cancelar una atención acordada
> indicando el motivo**, para **resolver un imprevisto sin dejar el registro abierto**.

**Criterios de aceptación**

- [ ] Dado que la atención no se completó, cuando la cancelo, entonces indico un
      motivo obligatorio.
- [ ] Dado que cancelo, cuando la contraparte consulta, entonces ve el estado y el motivo.
- [ ] Dado que cancelo, cuando consulto el pago asociado, entonces no queda pendiente.
- [ ] Dado que la atención ya se completó, cuando intento cancelar, entonces se rechaza.

**Requisitos:** RF-10.3, INV-05.

## Incremento del sprint

Las partes conversan, el profesional registra lo realizado, marca llegada y
finalización, y la atención queda lista para el pago.

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 8 — Confirmación mutua de pagos

**Objetivo del sprint.** El pago de cada atención se registra con confirmación de
ambas partes y la comisión queda controlada.

**Objetivo específico.** 4 · Controles de confirmación y validación mutua de pagos.

**Puntos:** 21.

### HU-25 · Confirmar que pagué `[ ]` — 5 puntos

> Como **paciente**, quiero **declarar que pagué la atención**, para **cerrar mi
> parte del compromiso**.

**Criterios de aceptación**

- [ ] Dado que la atención se completó, cuando abro el pago, entonces veo el monto
      acordado y el medio de pago a elegir.
- [ ] Dado que confirmo el pago, cuando el profesional consulta, entonces ve mi confirmación.
- [ ] Dado que confirmé, cuando el profesional aún no confirma, entonces el pago
      figura pendiente de la otra parte.
- [ ] Dado que la atención no se completó, cuando intento confirmar, entonces se rechaza.

**Requisitos:** RF-11.2, RF-11.4, INV-09.

### HU-26 · Confirmar que cobré `[ ]` — 8 puntos

> Como **profesional**, quiero **declarar que recibí el pago**, para **que la
> atención quede cerrada y mi comisión quede registrada**.

**Criterios de aceptación**

- [ ] Dado que el paciente confirmó, cuando confirmo la recepción, entonces el pago
      pasa a confirmado por ambas partes.
- [ ] Dado que el pago se confirma, cuando consulto el desglose, entonces veo el
      monto total, la comisión y lo que me corresponde.
- [ ] Dado que consulto la base de datos, cuando reviso el registro, entonces el
      total siempre iguala la suma de comisión más monto del profesional.
- [ ] Dado que el paciente declara haber pagado y yo declaro no haber recibido,
      cuando ambos registramos, entonces el pago queda en disputa.

**Requisitos:** RF-11.1, RF-11.3, RF-11.4, RF-11.5, RN-08, RN-09, INV-09, INV-10.

**Tareas técnicas.** Cálculo de comisión como caso de uso con pruebas de valores
límite · restricción de cuadre en el motor · estado de disputa.

### HU-27 · Consultar mi saldo de comisiones `[ ]` — 3 puntos

> Como **profesional**, quiero **saber cuánto debo a la plataforma**, para
> **liquidar a tiempo**.

**Criterios de aceptación**

- [ ] Dado que tengo atenciones cobradas, cuando abro mi saldo, entonces veo las
      comisiones acumuladas, las liquidadas y las pendientes.
- [ ] Dado que el administrador registra una liquidación, cuando consulto,
      entonces mi saldo pendiente disminuye.
- [ ] El detalle se presenta paginado y ordenado de la más reciente a la más antigua.

**Requisitos:** RF-11.6, RNF-08.

### HU-28 · Conciliar los pagos de la plataforma `[ ]` — 5 puntos

> Como **administrador**, quiero **registrar las liquidaciones y resolver
> disputas**, para **mantener el control financiero de la plataforma**.

**Criterios de aceptación**

- [ ] Dado que hay comisiones pendientes, cuando abro la conciliación, entonces las
      veo agrupadas por profesional.
- [ ] Dado que un profesional liquida, cuando registro el pago recibido, entonces
      las comisiones correspondientes pasan a liquidadas.
- [ ] Dado que hay un pago en disputa, cuando lo reviso, entonces puedo resolverlo
      en cualquiera de los dos sentidos, dejando registro.
- [ ] Dado que soy administrador, cuando abro la conciliación, entonces veo montos,
      estados y participantes, pero nunca el contenido de las conversaciones.

**Requisitos:** RF-11.5, RF-11.7, RN-11, INV-12.

## Incremento del sprint

Una atención se completa, ambas partes confirman el pago, la comisión se acumula
en el saldo del profesional y el administrador la concilia. **El ciclo de negocio
está completo de punta a punta.**

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 9 — Calificación e historial

**Objetivo del sprint.** Ambas partes se califican, la reputación se construye y
cada usuario consulta su actividad.

**Objetivo específico.** 5 · Evaluación, calificación y registro histórico centralizado.

**Puntos:** 22.

### HU-29 · Calificar al profesional `[ ]` — 5 puntos

> Como **paciente**, quiero **calificar la atención recibida**, para **ayudar a
> otros pacientes a decidir**.

**Criterios de aceptación**

- [ ] Dado que la atención se completó, cuando abro la calificación, entonces
      elijo entre una y cinco estrellas y escribo un comentario.
- [ ] Dado que ya califiqué esa atención, cuando intento calificar de nuevo,
      entonces se rechaza.
- [ ] Dado que califico, cuando otro paciente abre la ficha del profesional,
      entonces ve mi comentario.
- [ ] Dado que la atención fue cancelada, cuando intento calificar, entonces se rechaza.
- [ ] Dado que registro mi calificación, cuando se guarda, entonces el promedio y
      el total de atenciones del profesional se recalculan de forma automática, sin
      intervención de la aplicación.

**Requisitos:** RF-12.1, RF-12.3, RN-10, INV-11.

### HU-30 · Calificar al paciente `[ ]` — 3 puntos

> Como **profesional**, quiero **calificar al paciente atendido**, para **que otros
> profesionales tengan referencia antes de acudir a un domicilio**.

**Criterios de aceptación**

- [ ] Dado que completé una atención, cuando la califico, entonces registro
      estrellas y comentario sobre el paciente.
- [ ] Dado que intento calificarme a mí mismo, cuando se procesa, entonces se rechaza.
- [ ] Dado que califico, cuando otro profesional recibe una solicitud de ese
      paciente, entonces ve su reputación.

**Requisitos:** RF-12.2, INV-11.

### HU-31 · Consultar mi historial de atenciones `[ ]` — 5 puntos

> Como **paciente**, quiero **ver todas las atenciones que recibí**, para
> **consultar lo que se hizo y volver a contactar a quien me atendió bien**.

**Criterios de aceptación**

- [ ] Dado que tuve atenciones, cuando abro mi historial, entonces las veo con
      fecha, profesional, servicio, monto, estado y mi calificación.
- [ ] Dado que abro una atención del historial, cuando la consulto, entonces accedo
      a su conversación, donde está el detalle de lo realizado.
- [ ] El historial está paginado y ordenado de la más reciente a la más antigua.
- [ ] Dado que un profesional cambia su tarifa, cuando consulto una atención
      pasada, entonces el monto no cambia.

**Requisitos:** RF-13.1, RF-13.4, RNF-08, INV-04.

### HU-32 · Consultar mi historial e ingresos `[ ]` — 5 puntos

> Como **profesional**, quiero **ver mis atenciones con su detalle económico**,
> para **llevar el control de mi trabajo independiente**.

**Criterios de aceptación**

- [ ] Dado que atendí pacientes, cuando abro mi historial, entonces veo cada
      atención con su monto, su comisión y lo que me correspondió.
- [ ] Dado que abro mi historial, cuando consulto el resumen, entonces veo el total
      del período.
- [ ] El historial está paginado y filtrable por rango de fechas.

**Requisitos:** RF-13.2, RF-13.4, RNF-08.

### HU-33 · Consultar el registro centralizado `[ ]` — 4 puntos

> Como **administrador**, quiero **ver todas las solicitudes y atenciones de la
> plataforma**, para **supervisar la operación**.

**Criterios de aceptación**

- [ ] Dado que abro el registro, cuando lo consulto, entonces veo solicitudes y
      atenciones con estado, participantes y montos.
- [ ] Dado que filtro por estado o por rango de fechas, cuando aplico el filtro,
      entonces los resultados se ajustan.
- [ ] Dado que soy administrador, cuando abro cualquier registro, entonces no veo
      el contenido de las conversaciones.

**Requisitos:** RF-13.3, RN-11, INV-12.

## Incremento del sprint

El sistema está funcionalmente completo: buscar, solicitar, negociar, comunicar,
atender, pagar, calificar y consultar. **Listo para validación con usuarios reales.**

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 10 — Validación con usuarios reales

**Objetivo del sprint.** Validar el sistema con pacientes y profesionales reales y
producir la evidencia empírica del informe.

**Objetivo específico.** 6 · Ejecutar las pruebas finales con datos reales.

Este sprint no agrega funcionalidad. Contiene tareas de validación y las
correcciones que surjan de ella.

## Tareas

### HT-10 · Preparación de la distribución `[ ]`

Versión firmada, verificación de la autenticación con la clave de publicación,
configuración de App Distribution y del grupo de evaluadores.

- [ ] La versión firmada autentica correctamente con Google
- [ ] Los evaluadores reciben la invitación y pueden instalar

### HT-11 · Datos iniciales `[ ]`

Catálogo de tipos de servicio poblado e incorporación manual de entre veinte y
treinta profesionales reales.

- [ ] Al menos veinte profesionales verificados y georreferenciados
- [ ] Cobertura de los tipos de servicio previstos

> Sin oferta disponible no hay nada que probar. Es trabajo de campo, no de
> programación, y determina el resultado de la validación más que cualquier
> decisión técnica.

### HT-12 · Instrumentación de métricas `[ ]`

Registro de los eventos necesarios para calcular los indicadores.

- [ ] Los eventos permiten reconstruir cada indicador de la tabla siguiente

### HT-13 · Ejecución de la prueba `[ ]`

Período de uso real con seguimiento de incidencias.

- [ ] Consentimiento informado firmado por los participantes
- [ ] Al menos doce participantes activos durante el período
- [ ] Registro de incidencias mantenido

### HT-14 · Corrección de defectos `[ ]`

Resolución de los fallos detectados.

- [ ] Los defectos bloqueantes están corregidos y verificados

### HT-15 · Informe de resultados `[ ]`

Análisis de métricas, contraste con los requisitos no funcionales y redacción del
capítulo de validación.

- [ ] Cada indicador tiene su valor medido
- [ ] Cada requisito no funcional está contrastado contra su criterio

## Indicadores a medir

| Indicador | Requisito que verifica |
|---|---|
| Tiempo desde publicación hasta primera oferta, mediana y percentil 90 | RF-07, RF-08 |
| Proporción de solicitudes que alcanzan acuerdo | RF-08 |
| Número de rondas de negociación hasta el acuerdo | RF-08.2 |
| Proporción de profesionales notificados dentro del radio | RF-09.4, RN-02 |
| Latencia de la búsqueda por cercanía | RNF-01 |
| Tiempo de arranque en frío | RNF-02 |
| Tamaño de la aplicación descargada | RNF-03 |
| Puntaje SUS por rol | RNF-07 |

## Retrospectiva final

_Completar al cerrar el proyecto. Incluir la evolución de la velocidad por sprint,
que es evidencia directa del carácter incremental e iterativo del desarrollo._

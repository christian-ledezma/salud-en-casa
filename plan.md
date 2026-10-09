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
| **Duración del sprint** | 1 semana. Se declaró en dos semanas al planificar y se corrigió al cerrar el Sprint 1 con el tramo real medido. Ver la retrospectiva de ese sprint |
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

**El rango vale por sprint de una semana.** Se escribió cuando el sprint se
declaraba de dos, y el Sprint 1 entregó 21 puntos en cuatro días. Al corregir la
duración se mantuvieron los números porque el dato medido cae dentro del rango;
lo que cambió es la unidad a la que se refiere.

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
9. La pantalla tiene previsualización en esquema claro, en oscuro y con el tamaño
   de fuente al 200 %, y se ve correcta en las tres.
10. Los cuatro estados de la pantalla están resueltos: cargando, vacío, con
    contenido y error.
11. **Si la historia es crítica**, su recorrido se demostró en el emulador; **si es
    de criticidad alta**, en un dispositivo físico. El resto no exige ninguno de
    los dos. Ver «Cuándo hace falta un dispositivo».
12. Las decisiones no evidentes están registradas en `docs/decisions.md`.

> **Sobre el punto 4.** Una prueba en rojo no se apaga. O el código está mal y se
> corrige, o la prueba expresaba mal la regla y se corrige la prueba explicando por
> qué. Una prueba intermitente es un defecto, no una molestia.

> **Sobre el punto 9.** Las tres previsualizaciones sustituyen a la inspección
> manual que el punto 11 pedía antes para toda historia. Son el control que
> compensa haber dejado de recorrer cada pantalla a mano, así que no son opcionales
> ni siquiera en una pantalla sencilla: es donde se atrapan los recortes de texto y
> las filas que dejan de componerse.

> **Sobre el control de versiones.** El agente **nunca ejecuta comandos de git**.
> Al terminar una historia informa qué archivos creó o modificó, y el autor decide
> qué versiona.

---

## Cuándo hace falta un dispositivo

Recorrer una pantalla a mano es caro y frágil: depende de que la máquina sostenga
un emulador, de que haya una cuenta de Google viva en él y de que nada se caiga a
medias. Hasta el 2026-10-04 la Definición de Terminado lo exigía para **toda**
historia, y en la práctica se convirtió en el paso que más veces bloqueó un cierre
sin aportar hallazgos proporcionales. Desde esa fecha se gradúa en tres niveles.
Registrado en `docs/decisions.md`, 2026-10-04.

### Criticidad alta — dispositivo físico

Solo lo que un emulador **no puede representar con fidelidad**:

- Cámara real y compresión de la imagen capturada (HU-07).
- Ubicación por GPS en exteriores y su precisión (HU-05, HU-11).
- Entrega efectiva de notificaciones push (HU-20).
- La validación con usuarios reales del Sprint 10, que por definición ocurre ahí.

Nada más entra aquí. Si la duda es «¿se ve bien?», no es criticidad alta.

### Criticidad crítica — emulador

Una historia es crítica cuando cumple **las dos** condiciones:

1. Su fallo rompe el flujo central del negocio, o pierde o corrompe datos.
2. Su corrección depende de cómo se **compone** la pantalla, no solo de la lógica,
   de modo que una prueba unitaria no puede atraparla.

Con el alcance actual, eso son:

| Historia | Por qué |
|---|---|
| HU-01, HU-02 | Primer ingreso y elección de rol: si fallan, nadie entra |
| HU-05, HU-06 | Mapa, permisos y geocodificador; y la eliminación de direcciones, que borra datos |
| HU-11 | Búsqueda por cercanía, que es la pantalla que sostiene el producto |
| HU-17, HU-18, HU-19 | Negociación y aceptación: hay dinero y un contrato de por medio |
| HU-25, HU-26 | Confirmación de pagos (INV-09) |
| HU-29, HU-30 | Calificación, que es irreversible |

**HU-06 es la única que cierra con este requisito incumplido.** Está en la tabla
porque elimina direcciones, y se cerró el 2026-10-04 —el mismo día que se escribió
esta regla— sin recorrido en emulador, después de que el emulador fallara cuatro
veces. La historia lo dice en su propio apartado. Se deja así y no se retoca la
tabla para que encaje: ajustar el criterio hasta que lo ya hecho cumpla es
exactamente lo que vacía una Definición de Terminado.

### Todo lo demás

No exige emulador ni dispositivo. Lo cubren las pruebas unitarias, los
experimentos SQL de política y disparador, y las tres previsualizaciones del punto
9 de la Definición de Terminado.

### Lo que esto acepta a cambio

**Los defectos de disposición se van a encontrar más tarde, o no se van a
encontrar.** El Sprint 2.5 halló dos recorriendo pantallas a mano, y HU-06 un
tercero, de modo que la clase de defecto es real y frecuente en este proyecto. Lo
que compensa es el punto 9, que pasa de recomendación a obligación en las tres
configuraciones, y lo que lo compensaría de verdad es la suite de
`app/src/androidTest`, que sigue vacía. Mientras siga vacía, este cambio aumenta
el riesgo de forma consciente: está aceptado, no ignorado.

---

## Índice de sprints

| # | Sprint | Objetivo específico | Puntos | Estado |
|---|---|---|---|---|
| 0 | Fundación técnica | 1 · Arquitectura | — | `[~]` |
| 1 | Ingreso e identidad | 2 · Perfiles | 21 | `[x]` |
| 2 | Perfiles y ubicación | 2 · Perfiles | 21 | `[x]` |
| 2.5 | Rol múltiple con rol activo | 2 · Perfiles | 23 | `[x]` |
| 3 | Verificación de usuarios | 2 · Perfiles | 24 | `[x]` |
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

- [x] Las cinco etapas concluyen correctamente en un envío de prueba —
      **pendiente del autor.** El flujo está escrito y sus comandos se verificaron
      uno por uno en local, pero la primera ejecución real exige cargar los cuatro
      secretos del repositorio y hacer un envío. El procedimiento está en el
      `README.md`
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

## Incremento del sprint

Una prueba de humo con un botón que autentica con Google, crea el perfil en la
base de datos y muestra el identificador obtenido. Es código desechable: se
elimina en HU-01.

## Retrospectiva

_Completar al cerrar el sprint._

---

# Sprint 1 — Ingreso e identidad

**Estado:** `[x]` terminado. Las tres historias cumplen la Definición de
Terminado y sus dieciséis criterios de aceptación están verificados en el
emulador.

**Objetivo del sprint.** Una persona ingresa con su cuenta de Google, elige si es
paciente o profesional, y completa sus datos básicos.

**Objetivo específico.** 2 · Desarrollar la gestión de perfiles de usuarios.

**Puntos:** 21.

### HU-01 · Ingresar con cuenta de Google `[x]` — 8 puntos

> Como **persona que necesita atención domiciliaria**, quiero **ingresar con mi
> cuenta de Google sin crear una contraseña**, para **empezar a usar la
> aplicación sin fricción**.

**Criterios de aceptación**

- [x] Dado que tengo una cuenta de Google en el dispositivo, cuando pulso
      «Continuar con Google», entonces aparece el selector nativo del sistema.
- [x] Dado que elijo una cuenta, cuando la autenticación concluye, entonces
      accedo a la aplicación sin escribir credenciales.
- [x] Dado que cancelo el selector, cuando vuelvo a la pantalla, entonces no se
      muestra ningún error y puedo reintentar.
- [x] Dado que no hay cuentas en el dispositivo, cuando pulso el botón, entonces
      se me indica cómo agregar una.
- [x] Dado que ya ingresé antes, cuando abro la aplicación, entonces entro
      directamente sin volver a autenticarme.
- [x] Dado que cierro sesión, cuando vuelvo a abrir, entonces se me pide ingresar.

**Requisitos:** RF-01.1, RF-01.2, RF-01.6, RF-01.7.

**Tareas técnicas.** Objetos de valor `Email`, `PhoneNumber`, `PersonName` con sus
pruebas · `IAuthRepository` en dominio y su implementación en datos · casos de uso
de ingreso, cierre de sesión y consulta de sesión · `SupabaseAuthDataSource` ·
integración de Credential Manager con nonce en la capa de presentación · pantalla de
bienvenida y modelo de vista · pantalla de arranque que decide destino según sesión.

**Las nueve tareas técnicas están terminadas.** La característica `auth` es la
primera del proyecto con sus tres capas:

| Capa | Qué contiene |
|---|---|
| `domain/model/` | `AuthSession`, `SessionState`, `AuthError`, `AuthResult`, `SignOutResult` |
| `domain/repository/` | `IAuthRepository` |
| `domain/usecase/` | `SignInWithGoogleUseCase`, `SignOutUseCase`, `ObserveSessionUseCase` |
| `data/datasource/` | `SupabaseAuthDataSource` |
| `data/mapper/` | `AuthSessionMapper`, de `UserInfo` y `SessionStatus` al dominio |
| `data/repository/` | `AuthRepository`, único lugar donde una excepción de la biblioteca se convierte en un tipo de error |
| `presentation/` | `StartupScreen`, `WelcomeScreen`, `AccountScreen`, sus tres modelos de vista y `GoogleCredentialClient` |
| `navigation/` | Rutas tipadas y el grafo con las tres pantallas |
| `core/vo/` | `Email`, `PhoneNumber`, `PersonName` |

**Esta característica no tiene objeto de transporte.** El resto del proyecto
tendrá su `<Entity>Dto` en `data/model/`, pero aquí el transporte lo define la
biblioteca: `UserInfo` y `SessionStatus` ya son los tipos que llegan del
servidor. Copiarlos a un tipo propio idéntico no habría agregado nada, así que
el transformador va directo de ellos al dominio.

**Decisiones no evidentes, en `docs/decisions.md`, 2026-09-12.** Dónde viven los
objetos de valor compartidos · por qué Credential Manager queda en la
presentación y el modelo de vista recibe la petición como función · por qué el
cierre de sesión sin conexión limpia la sesión guardada · por qué el teléfono se
guarda con su código de país.

**Los porqués menores, que no llegan a decisión pero tampoco se deducen leyendo.**
El código de HU-01 no lleva comentarios: la regla de `CLAUDE.md` los reserva para
lo que el código no puede expresar, y el «por qué» del sistema vive aquí y en
`docs/decisions.md`. Estos ocho son los que sostenían un comentario y ahora
viven en esta lista.

| Dónde | Por qué está así |
|---|---|
| `AuthSession` trae el correo y el nombre anulables | El proveedor puede entregar un valor que los objetos de valor de este proyecto rechazan, y eso debe costar el dato, nunca la sesión que Supabase ya aceptó |
| `Email` valida la forma de manera laxa | La dirección que llega es la que el proveedor de identidad ya verificó; un patrón más estricto solo rechaza direcciones válidas que nadie previó |
| `SupabaseAuthDataSource.isConfigured` | Una copia limpia del repositorio compila con los secretos vacíos a propósito, así que la ausencia se detecta aquí y se informa como configuración y no como token rechazado |
| `setFilterByAuthorizedAccounts(false)` | Con el filtro activo, quien entra por primera vez ve un selector de cuentas vacío |
| Las cuatro banderas de `install(Auth)` en `CoreModule` | Se escriben aunque sean las de omisión porque RF-01.6 depende de que sigan siendo esas |
| El grafo reemplaza la pila al ingresar y al cerrar sesión | Ambos cambios alteran de quién es la aplicación, y el botón de retroceso no debe devolver a la pantalla de quien ya no está |
| La bienvenida desplaza el encabezado y fija la llamada a la acción al pie | Es lo que mantiene la pantalla utilizable con el tamaño de fuente del sistema al 200 % |
| `AuthError.Cancelled` tiene clave de recurso pero nunca se muestra | El modelo de vista lo convierte en estado de reposo antes de que llegue a la pantalla; la clave existe para que el `when` sea exhaustivo |

**Qué atrapan las pruebas nuevas.** Cuarenta y cinco pruebas nuevas, que llevan
la suite de 9 a 54. Todas viven en `app/src/test` y corren sin emulador:

| Prueba | El error real que atrapa |
|---|---|
| `EmailTest`, `PersonNameTest`, `PhoneNumberTest` | Los límites exactos de cada regla, y que el código de país no mutile un número nacional que empieza por `591` |
| `SignInWithGoogleUseCaseTest` | Un token vacío viajando al servidor, y el token y el nonce intercambiados entre sí |
| `WelcomeViewModelTest` | Que cancelar el selector aparezca como error, y que la pantalla avance con la credencial obtenida aunque Supabase la rechace |
| `StartupViewModelTest` | El defecto de HT-05: decidir el destino antes de que la sesión guardada termine de restaurarse |
| `AuthSessionMapperTest` | Leer una sola de las dos formas en que Google escribe el nombre, y tratar como sesión iniciada un estado autenticado sin usuario |
| `DataStoreSessionManagerTest` | Que la sesión no sobreviva el viaje de ida y vuelta por la serialización, que en el dispositivo se ve como pedir el ingreso cada mañana |

`ArchitectureRulesTest.domainLayerHasNoPlatformImports` se amplió para recorrer
también `core/vo/`, y la ampliación se comprobó provocando su fallo con un
archivo sonda que importaba `android.util.Log`, igual que se hizo en HT-08.

**Los cuatro estados de cada pantalla.** «Vacío» no aplica a ninguna de las tres:
ninguna presenta una colección. Arranque resuelve cargando; bienvenida resuelve
reposo, ingresando y error; cuenta resuelve cargando, con contenido y error. Las
diez previsualizaciones cubren esos estados en ambos esquemas.

**Verificado en el emulador el 2026-09-13**, con la cuenta real de Google del
autor, dirigiendo el emulador por `adb` (selector de cuentas, botones, cierre y
apertura completa del proceso) y capturando pantalla en cada paso:

| # | Criterio | Cómo se verificó |
|---|---|---|
| 1 | El selector nativo aparece | Se pulsó «Continuar con Google» con sesión cerrada; apareció la hoja de Credential Manager, «Sign in with Google», con la cuenta del dispositivo y el botón «Continue» |
| 2 | La autenticación concluye sin escribir credenciales | Tras elegir la cuenta, la aplicación llegó a «Mi cuenta» mostrando el nombre y el correo reales de esa cuenta, sin ningún campo de contraseña en el camino |
| 3 | Cancelar no deja error | Se descartó el selector con el gesto de retroceso; la pantalla volvió a su estado de reposo, con el botón disponible y sin texto en rojo |
| 4 | Sin cuentas en el dispositivo | Se quitó la única cuenta de Google del emulador (Ajustes → Contraseñas y cuentas → Quitar cuenta) y se pulsó el botón; apareció `error_sign_in_no_google_account` tal como está en `values-es/strings.xml`, con «Reintentar» |
| 5 | La sesión persiste | Con sesión iniciada, se forzó el cierre completo del proceso (`am force-stop`) y se relanzó; la aplicación entró directo a «Mi cuenta», sin pasar por la bienvenida |
| 6 | El cierre de sesión se sostiene | Se pulsó «Cerrar sesión», se forzó el cierre completo y se relanzó; la aplicación pidió ingresar de nuevo, en la bienvenida |

De paso se verificaron dos puntos más de la Definición de Terminado: el
esquema oscuro (contraste correcto, botón primario invertido) y el tamaño de
fuente del sistema al 200 % (el texto de «Mi cuenta» se lee completo,
desplazándose, sin recortes), ambos revertidos a su valor original al terminar.

**Consecuencia para el autor.** Verificar el criterio 4 exigió quitar la única
cuenta de Google del emulador. Antes de volver a usarlo para cualquier otra
cosa, hay que agregarla de nuevo desde Ajustes → Contraseñas y cuentas →
Agregar cuenta — esto no se puede hacer sin la contraseña, así que queda para
el autor.

**Sin verificar en dispositivo físico.** Todo lo anterior se hizo sobre el
emulador (`sdk_gphone64_arm64`), no sobre un teléfono real. El proyecto ya
aceptó esa misma equivalencia en HT-05, así que la historia se da por
terminada en ese mismo criterio; si el autor prueba igual en un teléfono y
encuentra una diferencia, se registra como hallazgo nuevo.

#### Revisión del pull request #2

El revisor automático de GitHub señaló cinco observaciones. Cada una se
verificó contra el código real —y, en dos casos, contra el fuente de la
biblioteca— antes de aceptarla o descartarla. **Cuatro eran reales y una no.**
Revisando el resto del cambio aparecieron dos defectos más que el revisor no
vio, uno de ellos más grave que cualquiera de los suyos.

| # | Observación | Veredicto |
|---|---|---|
| 1 | El grafo elimina siempre el destino de arranque | **Real.** El fuente de Navigation 2.10.1 confirma que un `popUpTo` a un destino ausente se ignora entero, así que la pila crecía una entrada por ciclo. El daño que el revisor describía —ver datos de la cuenta tras cerrar sesión— **no** era cierto |
| 2 | «Reintentar» del fallo de cierre de sesión no reintenta | **Real.** Contradice `.claude/rules/compose.md`: el control no decía lo que hacía |
| 3 | `auth-sequence.md` describe símbolos borrados | **Real, y mayor de lo señalado:** eran nueve referencias muertas, no dos |
| 4 | `components.md` apunta a `GoogleAuthClient.kt` | **Real.** Una línea |
| 5 | La tabla de pruebas de `plan.md` documenta lo contrario de lo implementado | **Falso positivo.** El encabezado de esa columna es «El error real que atrapa»: la celda describe el defecto del que la prueba protege, no la conducta implementada |

**Los dos hallazgos propios.**

- **El tiempo de espera agotado no se reconocía como falla de red.** supabase-kt
  relanza `HttpRequestTimeoutException` sin envolverla, de modo que caía en la
  rama genérica y salía como error inesperado. Lo grave no era el mensaje sino
  que el respaldo del cierre de sesión sin conexión vive en la rama de red: la
  sesión guardada no se limpiaba cuando la falta de conexión se manifestaba
  como tiempo agotado. Registrado en `docs/decisions.md`, 2026-09-13.
- **`AccountViewModel` y `SignOutUseCase` no los tocaba ninguna prueba.** Era el
  único camino de la característica sin cobertura, y es donde vivía el
  defecto 2.

**Qué se corrigió y qué quedó como deuda.** Las cuatro observaciones reales y
los dos hallazgos propios se corrigieron. La suite pasa de 54 a 60 pruebas: seis
nuevas entre `AccountViewModelTest` y `AuthErrorMapperTest`. Esta última se
comprobó revirtiendo el mapeo al anterior y viéndola fallar, igual que se hizo
con las reglas de arquitectura en HT-08.

Quedan dos deudas anotadas, ninguna de ellas corregible con una prueba de la
máquina virtual de Java:

1. **La corrección del grafo no tiene prueba automática.** Fijarla exige una
   prueba instrumentada con `TestNavHostController`, y el proyecto no tiene
   todavía nada bajo `androidTest`. Se verificó a mano en el emulador que el
   retroceso desde la bienvenida sale de la aplicación; las dos transiciones
   posteriores al ingreso se razonaron desde el fuente de la biblioteca, porque
   el emulador se quedó sin cuenta de Google al verificar el criterio 4.
2. **El cableado del botón «Reintentar» tampoco.** `AccountViewModelTest` fija
   el contrato en que ese botón se apoya, pero no puede ver a qué lambda está
   conectado; eso lo atraparía una prueba de Compose sobre `AccountContent`.

**El pull request arrastra HT-09.** `master` estaba en `0f4ca75`, así que la
rama de HU-01 lleva también el commit de los cuatro diagramas de arquitectura.
Al fusionar, el Sprint 0 queda cerrado en `master` junto con esta historia. Se
anota para que el registro no dé a entender que HT-09 entró por su cuenta.

### HU-02 · Elegir mi rol `[x]` — 5 puntos

> Como **usuario que ingresa por primera vez**, quiero **indicar si soy paciente o
> profesional de salud**, para **que la aplicación me muestre lo que me corresponde**.

**Criterios de aceptación**

- [x] Dado que ingreso por primera vez, cuando la sesión se establece, entonces se
      me pide elegir entre paciente y profesional.
- [x] Dado que elijo un rol, cuando confirmo, entonces se crea mi registro
      específico y no se me vuelve a preguntar.
- [x] Dado que ya tengo rol, cuando vuelvo a ingresar, entonces voy directo a la
      pantalla principal de mi rol.
- [x] Dado que abandono la aplicación sin elegir, cuando vuelvo a entrar, entonces
      se me vuelve a pedir la elección.
- [x] El rol de administrador nunca aparece como opción.

**Requisitos:** RF-01.3, RF-01.4, RF-01.5.

**Tareas técnicas.** Migración que hace atómica la elección · `IProfileRepository`
en dominio y su implementación en datos · casos de uso de leer y elegir el rol ·
`SupabaseProfileDataSource` · pantalla de elección de rol y modelo de vista ·
arranque que decide entre bienvenida, elección y pantalla principal.

**`features/profile/` es la segunda característica del proyecto**, y la primera
que se cruza con otra:

| Capa | Qué contiene |
|---|---|
| `domain/model/` | `UserRole`, `AssignableRole`, `ProfileError`, `RoleResult`, `ChooseRoleResult` |
| `domain/repository/` | `IProfileRepository` |
| `domain/usecase/` | `GetRoleUseCase`, `ChooseRoleUseCase` |
| `data/model/` | `ProfileRoleDto` y `AssignRoleParams` |
| `data/datasource/` | `SupabaseProfileDataSource` |
| `data/mapper/` | `ProfileRoleMapper` y `ProfileErrorMapper` |
| `data/repository/` | `ProfileRepository` |
| `presentation/` | `RoleSelectionScreen`, `RoleSelectionViewModel`, `ProfileErrorMessages` |

**Esta característica sí tiene objeto de transporte**, al revés que `auth`: lo
que llega de `profiles` es una fila cualquiera de PostgREST, no un tipo que la
biblioteca ya modele, así que `ProfileRoleDto` existe y el transformador va de
él al dominio.

**El cruce entre características.** `StartupViewModel` vive en
`features/auth/presentation/` y consume `GetRoleUseCase`, que vive en
`features/profile/domain/usecase/`. Es el tipo de cruce que la regla de
dependencia permite —por el caso de uso, nunca por la capa `data` de la otra
característica— y, hasta HU-02, no existía ninguno real:
`ArchitectureRulesTest.featureNeverImportsTheDataLayerOfAnotherFeature` se había
verificado en HT-08 con archivos sonda desechables porque no había dos
características entre las que fallar.

**Decisiones no evidentes, en `docs/decisions.md`, 2026-09-13.** Por qué la
elección del rol se escribe con una función almacenada · por qué el tipo y la
tarifa del profesional pasan a admitir nulo · por qué el rol que se elige es un
tipo distinto del rol que se tiene · por qué el arranque resuelve sesión y rol
juntos y el ingreso vuelve al arranque.

**Los porqués menores, que no llegan a decisión pero tampoco se deducen leyendo.**

| Dónde | Por qué está así |
|---|---|
| `ChooseRoleUseCase` lee el rol antes de escribirlo | La regla de negocio vive en el caso de uso, y la lectura evita una petición que la base de datos iba a rechazar. La garantía sigue siendo de la base: la función y el disparador `profiles_guard_role` la repiten |
| `ProfileError.Unexpected` cubre lo que rechaza la función almacenada | El caso de uso ya filtró el rol repetido y el administrador, así que si la base los rechaza es por una condición que la aplicación creía imposible, y esa es la definición de inesperado |
| Un rol desconocido no se lee como rol ausente | Si `user_role` gana un valor que esta versión no conoce, tratarlo como «todavía no eligió» pondría a esa persona frente a una pregunta cuya respuesta la base rechazaría |
| El estado de fallo conserva la opción elegida | Sin ella, «Reintentar» no tendría qué reintentar y la persona volvería a una pantalla vacía |
| `assign_my_role` devuelve el rol que escribió | El resultado del dominio transporta lo que la base registró, no lo que el cliente pidió |
| El arranque tiene estado de error | Un rol que no se puede leer no es un rol que falta, y adivinar cualquiera de los dos lados rompe un criterio distinto |

**Qué atrapan las pruebas nuevas.** Diecisiete pruebas nuevas, que llevan la
suite de 60 a 77. Todas viven en `app/src/test` y corren sin emulador:

| Prueba | El error real que atrapa |
|---|---|
| `AssignableRoleTest` | Que `ADMIN` llegue a la lista de opciones de la pantalla. Es la prueba obligatoria `adminRoleIsNeverSelfAssignable` |
| `ChooseRoleUseCaseTest` | Un segundo rol viajando al servidor —la prueba obligatoria `assignsRoleOnlyOnceAndRejectsSecondAssignment`— y una escritura hecha cuando ni siquiera se pudo leer el rol actual |
| `ProfileRoleMapperTest` | Leer el rol nulo del primer ingreso como un fallo, y leer un valor desconocido de `user_role` como si no hubiera rol |
| `ProfileErrorMapperTest` | El mismo tiempo de espera agotado que la revisión del PR #2 encontró en `auth`, esta vez en el arranque |
| `RoleSelectionViewModelTest` | Confirmar sin haber elegido, y perder la opción elegida al fallar, que dejaría «Reintentar» sin nada que reintentar |
| `StartupViewModelTest` | Las cinco salidas del arranque, incluida la peor: tratar un fallo de lectura como «no hay rol» y repetir la pregunta a quien ya respondió |

**Lo que la base de datos hace cumplir, y la aplicación no puede.** La migración
`20260913202844_assign_role_atomically` se aplicó sobre el proyecto remoto el
2026-09-13 y se verificó con los tipos generados desde ahí:

| Qué se comprobó | Resultado |
|---|---|
| La función existe con su firma | `assign_my_role(p_role: user_role) → user_role` |
| Las dos columnas admiten nulo | `professional_type` y `base_rate_bob` figuran como anulables |
| La restricción de completitud existe | `professionals_approved_profile_is_complete` |

**Los cuatro estados de la pantalla de elección.** «Vacío» no aplica: no presenta
una colección. Resuelve reposo —con y sin opción elegida—, guardando y error.
Las cinco previsualizaciones cubren esos estados en ambos esquemas. El arranque
suma su estado de error, que antes no tenía.

**Verificado en el emulador el 2026-09-13**, con la cuenta real de Google del
autor, recién repuesta en el dispositivo, dirigiendo el emulador por `adb` y
capturando pantalla en cada paso. El perfil del autor llegaba a esta historia con
`role` nulo, de modo que el recorrido empezó donde empieza el de cualquiera que
entra por primera vez:

| # | Criterio | Cómo se verificó |
|---|---|---|
| 1 | Se pide elegir | Se ingresó con Google desde la bienvenida; la aplicación llegó a «¿Cómo vas a usar Salud en Casa?» en vez de a «Mi cuenta», con dos opciones y «Continuar» deshabilitado mientras no hubiera ninguna elegida |
| 4 | Abandonar sin elegir | **Se verificó antes que el 2, porque después ya no se puede.** Con la pregunta en pantalla y sin elegir nada, `am force-stop` y relanzamiento: volvió a la misma pregunta, no a la bienvenida, así que la sesión siguió intacta y la pregunta también |
| 2 | Se crea el registro | Se eligió «Paciente» —la tarjeta se rellenó en `primaryContainer` y «Continuar» se habilitó— y la aplicación llegó a «Mi cuenta» mostrando «Paciente» bajo el correo. **La revisión del pull request #3 movió después esa etiqueta a «Mi perfil»**; el criterio se verificó así en su momento y se volvió a comprobar en la pantalla nueva |
| 3 | Se va directo | `am force-stop` y relanzamiento: entró directo a «Mi cuenta», sin pasar por la pregunta. El rol que muestra se lee del servidor en cada arranque, no de la memoria de la sesión anterior |
| 5 | El administrador no aparece | La pantalla ofrece exactamente dos opciones. No es una condición de la interfaz: `AssignableRole` no tiene una constante para `ADMIN`, y `AssignableRoleTest` falla si alguien se la agrega |

**La transacción fue de verdad atómica.** Terminado el recorrido, las
estadísticas de tablas del proyecto remoto dan `profiles` con una fila,
`patients` con una fila y `professionals` con cero. Que la fila de `patients`
exista es lo que prueba las dos mitades a la vez: la política
`patients_insert_own` exige que `profiles.role` ya diga `PATIENT` cuando la fila
se inserta, de modo que la inserción solo pudo pasar viendo la actualización
hecha en la misma transacción.

De paso se verificó el esquema oscuro y el tamaño de fuente del sistema al 200 %
sobre «Mi cuenta», que es la pantalla que esta historia cambió: el nombre, el
correo y la línea del rol se leen completos, sin recortes, y el rol en `primary`
contrasta correctamente sobre el fondo oscuro. Ambos ajustes se devolvieron a su
valor original al terminar.

**Lo que no se pudo verificar en el dispositivo.** El esquema oscuro y el 200 %
**de la pantalla de elección de rol**. El rol se elige una sola vez y la base de
datos no deja deshacerlo, así que esa pantalla dejó de ser alcanzable en el
momento en que se verificó el criterio 2. Queda cubierta por sus cinco
previsualizaciones, que la dibujan en ambos esquemas, y por que cada color y cada
medida salen del tema; su estructura —encabezado desplazable con la llamada a la
acción fija al pie— es la misma de la bienvenida, que sí se verificó al 200 % en
HU-01. Se verifica en el dispositivo la próxima vez que exista una cuenta nueva
sin rol, por ejemplo al preparar la demostración del sprint.

**Deuda reconocida.** La misma que dejó HU-01 y que esta historia no salda: el
grafo de navegación sigue sin prueba automática, y ahora tiene una transición
más. Fijarlo exige `TestNavHostController` bajo `androidTest`, que el proyecto
todavía no tiene.

### HU-03 · Completar mis datos básicos `[x]` — 8 puntos

> Como **usuario registrado**, quiero **completar y editar mi nombre, teléfono y
> fotografía**, para **que la contraparte sepa con quién trata**.

**Criterios de aceptación**

- [x] Dado que elegí mi rol, cuando llego a mi perfil, entonces veo el nombre y la
      fotografía que trajo Google, editables.
- [x] Dado que escribo un teléfono con formato inválido, cuando intento guardar,
      entonces se me indica el error y no se guarda.
- [x] Dado que soy paciente, cuando abro mi perfil, entonces puedo registrar fecha
      de nacimiento, contacto de emergencia y notas relevantes.
- [x] Dado que guardo cambios, cuando vuelvo a abrir la aplicación, entonces los
      cambios persisten.
- [x] Dado que la carga falla por conexión, cuando reintento, entonces se muestra
      un mensaje que explica qué ocurrió y cómo reintentar.

> **Sobre el primer criterio.** La fotografía **se muestra** y no se reemplaza.
> Cambiarla convertiría a HU-03 en la primera historia con Supabase Storage
> —contenedor, políticas sobre `storage.objects`, selector de imágenes,
> compresión en el cliente y la decisión entre URL firmada y URL pública—, y esa
> decisión condiciona además la ficha pública del profesional del Sprint 4.
> Entra con RF-04.1, en el Sprint 3, que es la historia que obliga a que Storage
> exista. Acordado con el autor antes de empezar y registrado en
> `docs/decisions.md`, 2026-09-13. El nombre sí es editable, que es la otra
> mitad del criterio.

**Requisitos:** RF-02.1.

**Tareas técnicas.** `IProfileRepository` y casos de uso · `SupabaseProfileDataSource`
· pantalla de perfil de paciente con validación por objetos de valor · manejo de
error de red con tipo de error, no con frase.

**Las cuatro tareas están terminadas**, extendiendo la característica que HU-02
creó en lugar de abrir una nueva:

| Capa | Qué se agregó |
|---|---|
| `domain/vo/` | `BirthDate`, el primer objeto de valor del proyecto que pertenece a una sola característica |
| `domain/model/` | `UserProfile`, `PatientDetails`, `ProfileDraft`, `PatientDraft`, `ProfileUpdate`, `ProfileResult`, y tres variantes nuevas de `ProfileError` |
| `domain/usecase/` | `GetProfileUseCase`, `SaveProfileUseCase` |
| `data/model/` | `ProfileDto`, `PatientDto` y sus dos objetos de escritura |
| `data/datasource/` | Lectura y escritura de `profiles` y de `patients` |
| `data/mapper/` | `ProfileMapper` |
| `presentation/` | `ProfileScreen` y `ProfileViewModel` |
| `ui/components/` | `ProfileAvatar` |

**Ninguna migración.** `profiles` y `patients` ya tenían las siete columnas y las
políticas que esta historia necesita, desde HT-04. Es la primera historia del
proyecto que no toca el esquema, y conviene que se note: el modelo de datos se
diseñó completo por adelantado justamente para esto.

**Decisiones no evidentes, en `docs/decisions.md`, 2026-09-13.** Por qué la
fotografía se muestra y no se reemplaza · por qué el perfil se lee tal como está
guardado mientras el objeto de valor cuida la escritura · por qué el nombre sale
de «Mi cuenta» y vive en «Mi perfil» · por qué la fecha de nacimiento se elige
con el selector de Material 3.

**Los porqués menores, que no llegan a decisión pero tampoco se deducen leyendo.**

| Dónde | Por qué está así |
|---|---|
| El teléfono vacío se guarda como nulo | La columna lo admite, así que un campo en blanco es alguien que todavía no dio su número, no alguien que escribió algo inválido |
| El contacto de emergencia y las notas en blanco se guardan como nulos | Una cadena vacía en la columna se ve llena para cualquier consulta que solo pregunte si es nula |
| Tras guardar, la pantalla se redibuja con lo que devolvió el servidor | Es lo único que permite afirmar que el cambio quedó, y no solo que la petición salió del dispositivo. Se nota en el teléfono, que vuelve normalizado a `+591…` |
| Un fallo de validación conserva lo que la persona escribió | Vaciar el formulario para mostrar un error obligaría a reescribirlo entero para corregir un campo |
| La lectura del perfil son dos consultas y no una incrustada | `patients` solo se consulta cuando el rol es `PATIENT`, y una relación uno a uno incrustada devuelve objeto o arreglo según cómo PostgREST la detecte |
| `BirthDate.create` recibe la fecha de hoy como parámetro | Sin eso la prueba del borde exacto —hoy mismo— dependería del día en que se ejecute |

**Qué atrapan las pruebas nuevas.** Veinte pruebas nuevas, que llevan la suite de
77 a 97. Todas viven en `app/src/test` y corren sin emulador:

| Prueba | El error real que atrapa |
|---|---|
| `BirthDateTest` | Los cinco bordes de la regla, incluido hoy mismo, que es exactamente lo que la restricción `birth_date < current_date` rechaza |
| `SaveProfileUseCaseTest` | Un teléfono inválido viajando al servidor, un campo vaciado que llega como cadena vacía en vez de nulo, y un profesional escribiendo columnas de paciente |
| `ProfileMapperTest` | El peor de todos: leer el nombre a través de `PersonName` y devolverle un campo vacío a quien tiene un nombre que el objeto de valor rechaza. También que un rol o una fecha ilegibles cuesten ese campo y no el perfil entero |
| `ProfileViewModelTest` | Mostrar un formulario vacío tras una lectura fallida —que invita a guardarlo encima del real—, perder lo escrito al reportar un error, y afirmar que se guardó sin mirar lo que respondió el servidor |

**Verificado en el emulador el 2026-09-13**, con las dos cuentas de Google reales
del autor, que a esta altura cubren los dos roles: `chris.ledezma.s@gmail.com` es
profesional y `christian.ledezma@ucb.edu.bo` es paciente. Eso permitió recorrer
las dos formas de la pantalla sin inventar datos:

| # | Criterio | Cómo se verificó |
|---|---|---|
| 1 | Nombre y fotografía de Google, editables | «Mi perfil» abrió con el nombre ya escrito en su campo y la fotografía de la cuenta descargada de `googleusercontent.com`, distinta en cada cuenta. Es la primera petición HTTP de imagen real del proyecto |
| 2 | Teléfono inválido | Se escribió `123` y se pulsó «Guardar cambios»: apareció `error_profile_invalid_phone` bajo el campo, el `123` siguió en pantalla y no se mostró «Cambios guardados.» |
| 3 | Campos de paciente | Con la cuenta paciente la pantalla mostró fecha de nacimiento, contacto de emergencia y notas médicas; con la cuenta profesional mostró solo nombre y teléfono |
| 4 | Los cambios persisten | Se guardó teléfono en una cuenta y fecha, contacto y notas en la otra; `am force-stop` y relanzamiento devolvieron los tres valores. El teléfono volvió como `+59171234567`, normalizado por el objeto de valor y releído del servidor |
| 5 | La carga falla por conexión | Con `svc wifi disable` y `svc data disable` se abrió «Mi perfil»: apareció `error_network_unavailable` con «Reintentar». Restaurada la red, «Reintentar» cargó el perfil completo |

**El selector de fecha expresa la restricción de la columna.** En la captura del
selector, hoy y todos los días posteriores aparecen atenuados, y en la vista de
años lo están 2027 en adelante. La misma regla la vuelve a comprobar
`BirthDate.create` antes de que nada salga del dispositivo.

También se verificaron el esquema oscuro y el tamaño de fuente del sistema al
200 % sobre «Mi perfil»: las siete etiquetas y los cinco campos se leen
completos, el formulario se desplaza y el contraste es correcto. El campo del
nombre desplaza su contenido en horizontal cuando no cabe, que es el
comportamiento propio de un campo de una sola línea y no un recorte del diseño.
Ambos ajustes se devolvieron a su valor original al terminar.

**Una mezcla de idiomas que no es un defecto.** En el emulador, cuyo idioma de
sistema es inglés, el selector de fecha rotula «Select date» mientras el resto
de la pantalla está en español. Las cadenas del selector son las de Material 3 y
siguen el idioma del dispositivo; las de la aplicación salen de `values/`, que
es el idioma de reserva y hoy está en español porque `values-en/` es de una fase
posterior (HT-07). En un dispositivo en español las dos coinciden.

#### Revisión del pull request #3

El revisor automático de GitHub señaló tres observaciones. Cada una se verificó
contra el código real antes de aceptarla. **Las tres eran reales**, aunque una
describía una consecuencia peor que la verdadera. Revisando el resto del cambio
apareció una cuarta que el revisor no vio.

| # | Observación | Veredicto |
|---|---|---|
| 1 | Guardar el perfil son dos peticiones independientes y puede dejar un guardado parcial | **Real, con un matiz.** El revisor escribió que «un reintento no puede evitar dejar un guardado parcial»; al revés: el guardado es una sobrescritura completa de las dos filas, así que reintentar lo repara. El daño real es para quien no reintenta, porque la pantalla dijo que falló y en el servidor quedó la mitad escrita |
| 2 | La ruta lleva un tipo de dominio en vez de un identificador | **Real.** `.claude/rules/compose.md` dice que los argumentos son identificadores, nunca objetos serializados, y `AccountRoute(val role: UserRole)` ataba el formato de la pila de retroceso al enumerado del dominio |
| 3 | Un comentario de código en español | **Real.** Un descuido propio, en `AssignableRole.kt`. La regla de idioma de `CLAUDE.md` no admite excepciones para comentarios. Era el único del cambio: se revisaron los demás archivos nuevos |

**El hallazgo propio.** `ProfileAvatar` no tenía marcador de posición ni estado
de error. Con una dirección de fotografía válida pero lenta o caída, el círculo
quedaba vacío en vez de mostrar algo. `.claude/rules/compose.md` lo exige de toda
imagen cargada con Coil. La silueta se dibuja ahora siempre y la fotografía
encima, de modo que sirve de las dos cosas sin una rama más.

**Qué se corrigió.**

1. **La observación 1, con el mismo remedio que HU-02.** La migración
   `20260914043306_save_profile_atomically` agrega `save_my_profile`, que escribe
   `profiles` y, si el rol es `PATIENT`, `patients`, en una sola transacción. El
   cliente pasó de dos peticiones a una. La función **lee el rol en el servidor**
   en vez de confiar en lo que mande el cliente, así que un argumento de paciente
   enviado por error se ignora en lugar de escribirse. Es incoherente haber
   construido `assign_my_role` por este motivo exacto una historia antes y no
   haberlo visto aquí.
2. **La observación 2, arreglada de raíz en vez de traducida.** En lugar de pasar
   el rol como cadena, la ruta dejó de llevarlo: `AccountRoute` vuelve a ser un
   `data object` y `StartupDestination.Home` también. La etiqueta del rol se
   movió a «Mi perfil», que **ya carga el perfil completo** y por lo tanto ya
   conoce el rol sin una consulta más. Queda además más coherente con la decisión
   del 2026-09-13: «Mi cuenta» es la sesión, «Mi perfil» es lo que la aplicación
   guarda de la persona, y el rol vive en `profiles`, no en la cuenta de Google.
3. **La observación 3**, traducida al inglés.
4. **El hallazgo propio**, con la silueta como marcador de posición y estado de error.

**Verificado otra vez en el emulador**, porque el camino de guardado cambió
entero y una prueba de la máquina virtual de Java no lo alcanza:

| Qué se comprobó | Resultado |
|---|---|
| Guardar como paciente | El teléfono volvió normalizado del servidor y los tres campos de paciente siguieron intactos: la transacción escribió las dos filas |
| Guardar como profesional | El nombre se guardó sin error y la función omitió `patients`, que para esa cuenta no existe |
| El teléfono inválido sigue rechazándose | Nueve dígitos por un error de tecleo produjeron el mensaje correcto y ningún guardado |
| Persistencia | Cierre completo del proceso y relanzamiento: los dos perfiles volvieron enteros |
| Nada espurio en la base | `profiles` 2, `patients` 1, `professionals` 1 |

La suite pasa de 97 a 99 pruebas: `ProfileMapperTest` gana las dos que fijan los
cinco argumentos de la función, incluida la fecha en formato ISO —el argumento es
de tipo `date` y cualquier otro formato lo rechaza el servidor con un error de
conversión, no con un mensaje de validación—.

**El pull request arrastra HU-02.** La rama de HU-03 salió de la de HU-02 sin que
esta se fusionara, así que los doce commits del pull request cierran las dos
historias a la vez. Se anota para que el registro no dé a entender que HU-02
entró por su cuenta.

**Deuda reconocida.** Reemplazar la fotografía, que espera a que exista Storage
(Sprint 3, RF-04.1). Y la de siempre: el grafo de navegación sigue sin prueba
automática, y ahora tiene una transición más, la de «Mi cuenta» a «Mi perfil»,
que además es la primera que apila en vez de reemplazar.

## Incremento del sprint

Un usuario nuevo ingresa con Google, elige ser paciente, completa su perfil y lo
ve persistido al reabrir la aplicación. **Primera demostración a la contraparte.**

## Retrospectiva

**El incremento se demostró de punta a punta.** Una persona ingresa con Google,
elige su rol, completa su perfil y lo encuentra intacto al reabrir la
aplicación. Se recorrió entero sobre el emulador con dos cuentas reales, una por
cada rol.

### Velocidad medida: 21 puntos por sprint

Es el número que el apartado «Estimación» mandaba medir aquí, y se mide contando
lo que se cerró, no lo que se planificó: HU-01 (8) + HU-02 (5) + HU-03 (8) = 21.
Las tres cumplen la Definición de Terminado, así que las tres cuentan enteras.
Ninguna historia se trasladó al sprint siguiente.

**Cae dentro de la capacidad estimada**, que era de 20 a 25 puntos. La
estimación inicial, que era una suposición, resultó razonable. Eso es lo que
había que averiguar.

**Consecuencia para el Sprint 2, que es lo único que este número decide.** El
Sprint 2 tiene 24 puntos planificados —HU-04 (8), HU-05 (13) y HU-06 (3)—, tres
por encima de la velocidad medida. La regla del apartado «Estimación» dice qué
hacer: «si la velocidad medida resulta menor, la historia de menor prioridad se
traslada al sprint siguiente». La de menor prioridad es HU-06, administrar las
direcciones, y moverla deja el Sprint 2 en exactamente 21.

**Decisión tomada: HU-06 se traslada al Sprint 3.** El Sprint 2 queda en 21
puntos, exactamente la velocidad medida, y el Sprint 3 sube de 21 a 24. El
traslado no es un fracaso: es el mecanismo de ajuste que SCRUM prevé y que el
apartado «Estimación» de este archivo describe.

Se eligió mover en vez de sostener los 24 porque el Sprint 2 es el más caro de
los tres que quedan del objetivo de perfiles —HU-05 sola vale 13 puntos e
introduce mapa, permisos de ubicación y geocodificación, tres cosas que el
proyecto no ha tocado nunca—, y es mal sprint para descubrir que la capacidad no
alcanzaba. La alternativa era defendible: tres puntos de diferencia sobre una
sola medición no son evidencia fuerte, y una velocidad se vuelve confiable con
dos o tres sprints medidos, no con uno. Se vuelve a evaluar al cerrar el
Sprint 2, con dos mediciones en la mano.

**El traslado obligó a un ajuste en HU-05.** `search_nearby_professionals` une
con `addresses` filtrando por `is_primary`, que viene por omisión en falso, de
modo que sacar HU-06 del sprint habría dejado a los profesionales invisibles
para la búsqueda y habría incumplido el incremento que el Sprint 2 promete.
HU-05 gana un criterio: la primera dirección que alguien registra queda marcada
como principal. Elegir entre varias sigue siendo trabajo de HU-06.


> El Sprint 1 se ejecutó del **11/09/2026** al **14/09/2026**.

**Cuatro días, no dos semanas, y de ahí salió una corrección del marco.** El
apartado «Marco SCRUM aplicado» declaraba sprints de dos semanas. Medido el
tramo real, la duración declarada pasó a **una semana**, que es la cadencia que
el proyecto practica de verdad. Corregir la declaración para que coincida con lo
que se hace es preferible a sostener un número que no se cumple: SCRUM pide que
la duración sea fija y conocida, y un marco declarado que nadie sigue es más
difícil de defender que una duración corta bien registrada.

**Qué significa entonces el 21, con precisión.** Este sprint terminó cuando se
agotó el alcance, no cuando se agotó el plazo, así que los 21 puntos **no miden
la capacidad**: miden lo que se planificó. Es un piso, no un techo. La
consecuencia práctica es que el traslado de HU-06 se apoya menos en la velocidad
de lo que parecía, y más en la otra razón, que sigue en pie por sí sola: HU-05
vale 13 puntos e introduce mapa, permisos de ubicación y geocodificación, tres
cosas que el proyecto no ha tocado nunca. El Sprint 2 es la primera medición que
sí dirá algo sobre la capacidad, porque se cerrará por tiempo.


### Qué funcionó

- **Verificar contra la fuente en vez de contra la suposición.** La revisión del
  pull request #2 aceptó cuatro de cinco observaciones y descartó una, y en dos
  casos la decisión se tomó leyendo el fuente de la biblioteca, no el mensaje
  del revisor. La misma disciplina encontró dos defectos que el revisor no vio,
  uno de ellos más grave que cualquiera de los suyos.
- **Provocar el fallo de una prueba antes de confiar en ella.** Se hizo con
  `AuthErrorMapperTest` revirtiendo el mapeo, igual que en HT-08 con las reglas
  de arquitectura. Una red de seguridad que nunca se vio fallar no es una red.
- **El esquema completo desde HT-04 se pagó solo.** HU-03 no necesitó ninguna
  migración: las columnas y las políticas ya estaban. Diseñar el modelo de datos
  por adelantado fue lo contrario de una carga.

### Qué no funcionó

- **La secuencia de commits de HU-01 separó los archivos eliminados del código
  que los reemplazaba**, y un archivo quedó sin versionar hasta la sesión
  siguiente. Corregido: cada borrado se entrega con `git rm` explícito, en el
  mismo commit que su reemplazo.
- **Verificar un criterio destruyó la posibilidad de verificar otros.** Quitar
  la cuenta de Google del emulador para probar el criterio 4 de HU-01 dejó el
  dispositivo sin forma de ingresar durante una sesión entera. Y elegir el rol
  en HU-02 dejó esa pantalla inalcanzable para siempre, de modo que su esquema
  oscuro quedó sin comprobar en dispositivo.
- **El orden de verificación importa y no estaba escrito en ninguna parte.** En
  HU-02 el criterio 4 —abandonar sin elegir— hubo que probarlo antes que el 2,
  porque el 2 es irreversible.

### Qué cambiar en el Sprint 2

1. **Ordenar los criterios de aceptación por reversibilidad antes de empezar a
   verificar**, y recorrer primero los que dejan de ser alcanzables después.
2. **Mantener dos cuentas de Google en el emulador**, una por rol. Ya están, y
   fue lo que permitió verificar las dos formas de la pantalla de perfil sin
   inventar datos.
3. **Saldar la deuda de `androidTest`.** Tres correcciones del grafo de
   navegación se acumulan sin prueba automática. Sprint 2 agrega HU-05, con
   permisos y mapa, que es exactamente donde una prueba instrumentada deja de
   ser opcional.

---

# Sprint 2 — Perfiles y ubicación

**Estado:** `[x]` terminado. Las dos historias cumplen la Definición de Terminado
y sus doce criterios de aceptación están verificados en el emulador.

**Objetivo del sprint.** El profesional publica su perfil profesional completo y
ambos roles registran sus direcciones georreferenciadas.

**Objetivo específico.** 2 · Desarrollar la gestión de perfiles de usuarios.

**Puntos:** 21.

> **HU-06 se trasladó al Sprint 3 al medir la velocidad.** El sprint tenía 24
> puntos planificados contra una velocidad medida de 21, y el apartado
> «Estimación» manda mover la historia de menor prioridad. Registrado en la
> retrospectiva del Sprint 1.

### HU-04 · Publicar mi perfil profesional `[x]` — 8 puntos

> Como **profesional de salud**, quiero **declarar mi especialidad, experiencia,
> tarifa base y radio de cobertura**, para **que los pacientes sepan qué ofrezco y
> a qué precio**.

**Criterios de aceptación**

- [x] Dado que soy profesional, cuando abro mi perfil, entonces puedo declarar
      tipo, especialidad, biografía, años de experiencia, tarifa base y radio.
- [x] Dado que ingreso una tarifa negativa o cero, cuando guardo, entonces se
      rechaza con un mensaje claro.
- [x] Dado que ingreso un radio fuera del rango de 1 a 50 kilómetros, cuando
      guardo, entonces se rechaza.
- [x] Dado que guardo mi perfil, cuando lo consulto como paciente, entonces veo la
      información publicada.
- [x] Dado que activo o desactivo mi disponibilidad inmediata, cuando cambio el
      interruptor, entonces el estado se refleja de inmediato.

**Requisitos:** RF-02.2, RF-02.5, RF-02.6.

**Tareas técnicas.** Objetos de valor `AmountBob` y `CoverageRadiusKm` con sus
pruebas · repositorio y casos de uso de perfil profesional · pantalla de perfil
profesional · pantalla de perfil público.

**Las cuatro tareas están terminadas**, extendiendo otra vez la característica del
perfil en lugar de abrir una nueva. El formulario es el mismo de HU-03: muestra la
sección de paciente o la de profesional según el rol guardado.

| Capa | Qué se agregó |
|---|---|
| `core/vo/` | `AmountBob`, el primer objeto de valor monetario del proyecto |
| `core/network/` | `BigDecimalSerializer`, la forma en que un `numeric` viaja en las dos direcciones |
| `core/util/` | `formatBob`, que da formato de moneda con la configuración regional del dispositivo |
| `domain/vo/` | `CoverageRadiusKm` y `YearsOfExperience` |
| `domain/model/` | `ProfessionalType`, `ProfessionalDetails`, `ProfessionalDraft`, `ProfessionalUpdate`, `PublicProfile`, `AvailabilityResult`, `PublicProfileResult` y tres variantes nuevas de `ProfileError` |
| `domain/usecase/` | `SetAvailabilityUseCase`, `GetPublicProfileUseCase`, y `SaveProfileUseCase` extendido |
| `data/model/` | `ProfessionalDto`, `PublicProfileDto` y seis argumentos más en `SaveProfileParams` |
| `data/datasource/` | Lectura de `professionals` y de `professional_directory`, y escritura de `available_now` |
| `presentation/` | `ProfessionalProfileSection`, `PublicProfileScreen` y `PublicProfileViewModel` |
| `ui/components/` | `RadioOptionGroup`, y `FormField` con tipo de teclado |
| `navigation/` | `PublicProfileRoute`, la primera ruta con argumento |

**Una migración: `20260914054958_save_professional_profile`.** Hace dos cosas.
`save_my_profile` recibe los argumentos del profesional y escribe `professionals`
dentro de la misma transacción que `profiles`, y la firma anterior de cinco
argumentos se elimina. Y `coverage_radius_km` pasa de aceptar cualquier valor
mayor que cero a aceptar de 1 a 50 kilómetros, que es el rango que la historia
enuncia. Aplicada sobre el proyecto remoto el 2026-09-14 con `npx supabase db
push`, previo `--dry-run`, y verificada consultando el catálogo: la función queda
con once argumentos y la restricción se llama
`professionals_coverage_radius_km_range`.

**Decisiones no evidentes, en `docs/decisions.md`, 2026-09-14.** Por qué el perfil
profesional entra en `save_my_profile` y la firma anterior se elimina · por qué la
disponibilidad inmediata se escribe sola y fuera del formulario · por qué
`AmountBob` nace en `core/vo/` y `CoverageRadiusKm` en la característica · por qué
un monto viaja como el texto exacto del número · por qué el perfil público lee la
vista y su estado vacío es INV-07 a la vista.

**Los porqués menores, que no llegan a decisión pero tampoco se deducen leyendo.**

| Dónde | Por qué está así |
|---|---|
| La tarifa en blanco se guarda como nulo | `base_rate_bob` lo admite: es alguien que todavía no puso precio. Lo que no puede hacer es volverse visible, porque `professionals_approved_profile_is_complete` no deja aprobarlo sin tarifa |
| Los años y el radio en blanco se rechazan | Sus columnas son `not null` y traen un valor por omisión, así que el campo nunca aparece vacío; vaciarlo es un descuido, no una respuesta |
| El formulario muestra `8` donde la columna guarda `8.00` | La escala es de la columna, no de lo que la persona escribió. Devolvérsela haría que cada visita a la pantalla pareciera una edición pendiente |
| La coma se acepta como separador decimal | El teclado decimal de un dispositivo en español ofrece la coma. Rechazar `120,50` sería rechazar lo que el propio teclado invita a escribir |
| El tipo de profesional es un grupo de opciones y no un control segmentado | Son cuatro, y «Estudiante del área de salud» no cabe en un segmento al ancho de un teléfono (`docs/design-system.md`, sección 5) |
| El perfil público no muestra la calificación cuando no hay ninguna | Un promedio de cero sobre cero calificaciones se lee como una estrella de cinco, no como un profesional sin calificar |
| El radio de cobertura no aparece en el perfil público | RF-02.6 no lo pide: es un dato con el que trabaja la búsqueda, no algo que ayude al paciente a decidir |

**Qué atrapan las pruebas nuevas.** Cuarenta y una pruebas nuevas, que llevan la
suite de 99 a 140. Todas corren en la máquina virtual de Java, sin emulador:

| Prueba | El error real que atrapa |
|---|---|
| `AmountBobTest` | El peor: `numeric(10,2)` no rechaza un tercer decimal, lo redondea, así que una tarifa escrita como `80.999` se guardaría como `81.00` y el profesional cobraría un boliviano que nunca declaró. También el cero, el negativo, el desborde de la columna y la coma del teclado en español |
| `CoverageRadiusKmTest` | Los dos bordes exactos del rango que ahora también vigila la columna |
| `YearsOfExperienceTest` | Los dos bordes del `check` de la columna, y que un campo vaciado no se lea como un cero |
| `ProfileDtoTest` | Las dos mitades del contrato con PostgREST: que un `numeric` se lea sin perder su escala, y que un monto salga como su texto exacto y no redondeado a través de `Double` |
| `SaveProfileUseCaseTest` | Una tarifa o un radio inválidos viajando al servidor, un campo vaciado que llega como cadena vacía en vez de nulo, y cada rol escribiendo las columnas del otro |
| `ProfileMapperTest` | Que un valor que `professional_type` gane en el futuro cueste ese campo y no la tarifa ni el radio, y que el tipo salga como el nombre del enumerado y no como su etiqueta |
| `ProfileViewModelTest` | Un interruptor que se queda encendido cuando el servidor lo rechazó —diciéndole al profesional que está recibiendo trabajo cuando no—, un interruptor que se lleva por delante lo que se estaba escribiendo, y una tarifa devuelta con la escala de la columna |
| `PublicProfileViewModelTest` | Presentar INV-07 como un fallo de lectura, que mandaría a la persona a revisar una conexión que funciona |

`StringResourcesTest.everyStringKeyUsedInCodeExistsInAllLocales` se amplió a los
plurales: HU-04 es la primera historia que usa `plurals`, y sin esa ampliación la
red que exige `.claude/rules/testing.md` habría dejado de cubrir lo que se
agregaba.

**Verificado en el emulador el 2026-09-14**, con las dos cuentas de Google del
autor. Los criterios se recorrieron ordenados por reversibilidad, como pide la
retrospectiva del Sprint 1: el estado «todavía no publicado» del perfil público se
comprobó **antes** de cualquier intento de aprobación, porque aprobar lo vuelve
inalcanzable.

| # | Criterio | Cómo se verificó |
|---|---|---|
| 1 | Declarar los seis datos | Con la cuenta profesional, «Mi perfil» mostró el grupo de tipo, especialidad, biografía, años, tarifa y radio. Se eligió Enfermera, se escribieron los cinco campos y se guardó: la base quedó con `NURSE`, `Enfermeria geriatrica`, la biografía, `10`, `120.00` y `8.00` |
| 2 | Tarifa cero | Se escribió `0` y se pulsó «Guardar cambios»: el campo se marcó en rojo con «Escribe una tarifa mayor a cero, con hasta dos decimales.», no apareció «Cambios guardados.» y la consulta a `professionals` mostró la fila sin tocar |
| 3 | Radio fuera de rango | Se escribió `60`: apareció «Escribe un radio entre 1 y 50 kilómetros.», y la fila siguió intacta. Al corregir el valor el mensaje desapareció solo |
| 4 | Ver la información publicada | Primero con la verificación en `PENDING`: «Ver mi perfil público» mostró «Este perfil todavía no está publicado», que es la respuesta correcta porque la vista tiene cero filas (INV-07). Después, aprobado el profesional a mano, la misma pantalla mostró la ficha con el nombre, «Enfermera o enfermero», la especialidad, «Todavía sin calificaciones», «10 años de experiencia», la tarifa, la disponibilidad, «0 atenciones realizadas» y la biografía. Es exactamente lo que se había guardado, leído de la vista y no de la tabla |
| 5 | Disponibilidad inmediata | Se pulsó el interruptor y la captura tomada de inmediato ya lo muestra encendido, antes de que el servidor contestara. `professionals.available_now` quedó en `true` |
| — | Sin regresión en HU-03 | Se ingresó con la cuenta paciente: la pantalla mostró solo los campos de paciente, sin interruptor ni perfil público, y guardar escribió `patients` con la función nueva. Es la comprobación que importaba, porque la firma anterior de `save_my_profile` se eliminó |
| — | Persistencia | Cerrar sesión, volver a ingresar con la cuenta profesional y reabrir «Mi perfil» devolvió los seis datos y el interruptor encendido |

También se revisaron el esquema oscuro y el tamaño de fuente del sistema al 200 %
sobre las dos pantallas nuevas: las etiquetas envuelven, «Estudiante del área de
salud» ocupa dos líneas dentro de su fila sin recortarse, el interruptor sigue
alineado y el contraste es correcto. Ambos ajustes se devolvieron a su valor
original al terminar.

**Cómo se aprobó el profesional para verificar el criterio 4, y por qué se
revirtió.** Aprobar es trabajo del administrador, que llega con HU-09 en el
Sprint 3, y `professionals_guard_verification_status` lo impide incluso desde el
editor SQL porque `is_admin()` es falso sin sesión. Se desactivó ese disparador, se
escribió `APPROVED`, se volvió a activar, se abrió la ficha, y después se devolvió
la fila a `PENDING`. Comprobado al terminar: `professional_directory` con cero
filas, ningún disparador del esquema público desactivado, y el guardia rechazando
de verdad —un `update` directo responde
`verification_status_is_set_by_an_administrator` y la fila no cambia—.

El estado en el que quedó la base es el correcto: ese profesional no tiene
documentos verificados y el Sprint 3 empieza sin nadie aprobado. Un profesional
aprobado a mano habría sido un dato falso sostenido por una excepción, que es
justo lo que HU-09 existe para reemplazar.

**Otra mezcla de idiomas que tampoco es un defecto.** En el emulador, cuyo idioma
de sistema es inglés, la tarifa se lee «Tarifa base BOB120.00». El formateador de
moneda de la plataforma decide la presentación según la configuración regional del
dispositivo, que es lo que exige `.claude/rules/i18n.md`: con `es-BO` el mismo
código produce «Bs120,00». Forzar el símbolo en español sería volver a componer el
texto a mano, que es precisamente lo que la regla prohíbe.

**Deuda reconocida.** Los servicios declarados y los comentarios recibidos que
RF-02.6 también menciona no están en la ficha: llegan con HU-10 (Sprint 4) y con el
Sprint 9, y hasta entonces no hay nada que mostrar. La fotografía sigue sin poder
reemplazarse, a la espera de Storage (Sprint 3). Y el grafo de navegación sigue sin
prueba instrumentada, ahora con una transición más y la primera que lleva un
argumento.

### HU-05 · Registrar mi dirección en el mapa `[x]` — 13 puntos

> Como **usuario**, quiero **marcar mi dirección sobre un mapa**, para **que la
> atención llegue al lugar correcto**.

**Criterios de aceptación**

- [x] Dado que abro el registro de dirección, cuando concedo el permiso de
      ubicación, entonces el mapa se centra en mi posición actual.
- [x] Dado que deniego el permiso, cuando abro el mapa, entonces puedo buscar y
      marcar la dirección manualmente sin que la aplicación falle.
- [x] Dado que arrastro el marcador, cuando lo suelto, entonces la dirección
      textual se actualiza según las coordenadas.
- [x] Dado que guardo una dirección, cuando la vuelvo a abrir, entonces no se
      consulta nuevamente el servicio de geocodificación.
- [x] Dado que guardo una dirección, cuando consulto la base de datos, entonces la
      ubicación está almacenada como punto geográfico.
- [x] Dado que registro una referencia textual, cuando la contraparte vea la
      atención, entonces la referencia estará disponible.
- [x] Dado que registro mi primera dirección, cuando la guardo, entonces queda
      marcada como principal.

**Requisitos:** RF-03.1, RF-03.2, RF-03.3, RF-03.4, RF-03.6.

> **El último criterio llegó con el traslado de HU-06.** `is_primary` viene por
> omisión en falso y `search_nearby_professionals` une con `addresses` filtrando
> por esa columna, así que un profesional con una dirección que no es principal
> es invisible para la búsqueda. Sin este criterio, sacar HU-06 del sprint
> habría dejado sin cumplir el incremento que el propio sprint promete. El
> disparador que desmarca la anterior ya existe desde HT-04, de modo que elegir
> entre varias sigue siendo trabajo de HU-06.

**Tareas técnicas.** Objeto de valor `Coordinate` con pruebas de rango ·
`ILocationRepository` y casos de uso · fuente de datos con caché de geocodificación
· solicitud de permisos con justificación previa · pantalla de mapa con marcador
arrastrable.

**Las cinco tareas están escritas y probadas.** Los siete criterios están
verificados en el emulador con el mapa dibujando.

**El repositorio se llama `IAddressRepository`, no `ILocationRepository`.** La
tarea técnica traía el segundo nombre, pero `.claude/rules/arquitectura.md` nombra
el repositorio por su entidad, y la entidad que este guarda es `Address`. El
paquete sí se llama `features/location/`, que es como el sprint y RF-03 nombran el
área.

| Capa | Qué se agregó |
|---|---|
| `core/vo/` | `Coordinate`, el primer objeto de valor con dos componentes y el primero que vigila valores no numéricos |
| `domain/vo/` | `AddressAlias`, `AddressText`, `CityName` y `AddressReference`, uno por cada límite que la tabla ya imponía |
| `domain/model/` | `Address`, `Place`, `AddressDraft`, `AddressUpdate`, `AddressError`, los cuatro resultados sellados y `MapDefaults`, que reúne la posición y los dos niveles de acercamiento con que abre el mapa |
| `domain/repository/` | `IAddressRepository`, `IGeocodingRepository` e `IDeviceLocationRepository`, uno por origen real de datos |
| `domain/usecase/` | `GetMyAddressUseCase`, `SaveAddressUseCase`, `DescribePointUseCase`, `FindPlaceUseCase` y `GetCurrentPositionUseCase` |
| `data/model/` | `AddressDto`, que lee la vista, y `AddressRow`, que escribe la tabla |
| `data/mapper/` | `toEwkt()`, el único lugar del proyecto donde se escribe un punto de PostGIS |
| `data/datasource/` | `SupabaseAddressDataSource`, `PlatformGeocoderDataSource` y `DeviceLocationDataSource` |
| `data/repository/` | `AddressRepository`, `GeocodingRepository` con su memoria de consultas y `DeviceLocationRepository` |
| `presentation/` | `AddressScreen`, `AddressViewModel`, `AddressMap` y `AddressErrorMessages` |
| `ui/theme/` | `Spacing.mapHeight` |
| `navigation/` | `AddressRoute`, alcanzable desde «Mi cuenta» |
| Compilación | Se cablea la agrupación `maps`, declarada desde HT-03 y hasta ahora sin usar, y aparece `MAPS_API_KEY` como marcador de posición del manifiesto |

**Una migración: `20260915020343_capture_addresses`.** Hace tres cosas, ninguna de
ellas una tabla nueva: la tabla `addresses` existe desde HT-04 con su seguridad a
nivel de fila y sus cuatro políticas.

1. **La vista `my_addresses`.** PostgREST devuelve una columna `geography` como la
   codificación hexadecimal que PostGIS guarda en disco —`0101000020E6100000…`—,
   que el cliente tendría que decodificar antes de dibujar un marcador. La vista
   proyecta el mismo punto como dos números y no cambia nada más. Es
   `security_invoker`, de modo que `addresses_select_own` sigue decidiendo qué
   filas devuelve; `professional_directory` es deliberadamente lo contrario,
   porque esa existe para mostrar filas ajenas.
2. **El disparador `addresses_first_is_primary`.** `is_primary` viene en falso por
   omisión y la búsqueda por cercanía une por esa columna, así que la primera
   dirección de una persona la marca la base y no el formulario. El nombre importa:
   los disparadores de fila previos se ejecutan en orden alfabético, y este va
   antes que `addresses_unmark_previous_primary`.
3. **La restricción `addresses_reference_length`.** `alias` y `address_text` traían
   límite de longitud desde HT-04; `reference` no, lo que dejaba al único campo de
   texto libre del formulario como la única columna donde un cliente podía guardar
   una cantidad arbitraria de datos.

Aplicada sobre el proyecto remoto el 2026-09-15 con `npx supabase db push`, previo
`--dry-run`, y verificada consultando el catálogo: la vista existe con
`{security_invoker=true}`, `latitude` y `longitude` son `double precision`, los tres
disparadores de `addresses` están habilitados y en el orden correcto, y la
restricción responde con su nombre al intentar guardar 301 caracteres.

**Decisiones no evidentes, en `docs/decisions.md`, 2026-09-15.** Por qué la
dirección se escribe con una consulta del cliente y no con una función almacenada ·
por qué el punto viaja como texto y se lee desde una vista · por qué la
geocodificación la resuelve el geocodificador de la plataforma y no el servicio de
pago del proveedor de mapas · por qué la clave de Mapas viaja por el manifiesto y
no por `BuildConfig`.

**Los porqués menores, que no llegan a decisión pero tampoco se deducen leyendo.**

| Dónde | Por qué está así |
|---|---|
| El marcador no se dibuja hasta que hay un punto | Un marcador puesto en el centro del mapa al abrir parece un punto ya elegido, y quien escriba su dirección encima guardaría el centro de la ciudad |
| El mapa se recentra con un contador y no siguiendo al marcador | Seguir al marcador deslizaría el mapa bajo el dedo cada vez que se lo suelta |
| La cámara se asigna y no se anima | Animar exige que el mapa ya esté medido, y el primer recentrado puede ocurrir antes del primer fotograma |
| Un punto que el geocodificador no sabe nombrar no vacía la dirección escrita | En un dispositivo sin geocodificador esa línea es la dirección entera |
| La ciudad es un campo del formulario y no solo un dato del geocodificador | `city` es `not null` y el geocodificador la deja vacía en un punto que no sabe nombrar |
| El radio y la ciudad se piden, la referencia no | `reference` admite nulo; un campo vacío es alguien que no tenía nada que agregar |
| La fuente de datos filtra solo por `is_primary` | Acotar por perfil en el cliente sería la condición que INV-13 dice no usar: quien decide es la política |
| El punto se escribe con siete decimales y configuración regional raíz | Un teléfono en español formatearía la coma, y PostGIS lee la coma como el separador entre dos puntos de una geometría |
| El mapa se instancia sin identificador de estilo en la nube | `.claude/rules/compose.md`: eso reclasifica cada carga a una categoría facturable |

**Qué atrapan las pruebas nuevas.** Cuarenta y seis pruebas nuevas, que llevan la
suite de 140 a 186. Todas corren en la máquina virtual de Java, sin emulador:

| Prueba | El error real que atrapa |
|---|---|
| `AddressViewModelTest`, sobre el punto sin nombre | El defecto que apareció en el dispositivo: el geocodificador devuelve vacío de vez en cuando, y la pantalla se quedaba callada dejando el marcador en un sitio y la dirección escrita describiendo otro |
| `CoordinateTest` | El hueco de toda validación por rango: un valor no numérico responde falso a `<` y a `>` por igual, así que sin su propia rama llegaría a la columna `geography` como un punto que PostGIS no sabe leer. También los cuatro bordes exactos |
| `AddressMapperTest` | El peor error de esta historia: PostGIS lee longitud primero y latitud después. Escribir el par como se dice guarda sin error y deja la dirección a 6 780 kilómetros, donde la búsqueda por cercanía no encuentra a nadie. También la coma decimal de un teléfono en español y la notación científica de un punto cercano al origen |
| `AddressAliasTest`, `AddressTextTest`, `CityNameTest`, `AddressReferenceTest` | Los límites exactos de cada columna. Un alias de 61 caracteres llega hoy como un error del servidor que nadie puede accionar, en vez de como un mensaje bajo su campo |
| `SaveAddressUseCaseTest` | Guardar una dirección escrita sin punto —una fila que la búsqueda no puede devolver nunca—, una referencia vacía viajando como cadena vacía a una columna que empieza en un carácter, y una corrección que pierde el identificador y termina creando una segunda dirección |
| `AddressViewModelTest`, sobre el guardado en curso | Un toque en el mapa o una búsqueda mientras la petición viaja: el marcador se movía y la fila que volvía del servidor lo devolvía a su sitio, descartando el punto recién elegido sin decir nada |
| `AddressViewModelTest` | Una pantalla que vuelve a geocodificar lo que ya está guardado, un geocodificador mudo que borra la dirección escrita a mano, una búsqueda sin resultados presentada como fallo de conexión, y un guardado rechazado que se lleva por delante lo que la persona escribió |

**Verificado en el emulador el 2026-09-15**, con la cuenta de Google del autor y el
emulador situado por `adb emu geo fix`. Los criterios se recorrieron ordenados por
reversibilidad, como pide la retrospectiva del Sprint 1: primero la denegación del
permiso, que es la que deja la pantalla en el estado más pobre.

| # | Criterio | Cómo se verificó |
|---|---|---|
| — | La justificación va antes del diálogo | Al abrir «Mi dirección» apareció la tarjeta «Queremos centrar el mapa donde estás» con sus dos salidas. El diálogo del sistema solo apareció al pulsar «Usar mi ubicación» (RF-03.6) |
| 2 | Permiso denegado | Se pulsó «Don't allow»: la aplicación no falló, la tarjeta desapareció, apareció «Sin el permiso de ubicación el mapa no se centra solo…» y el formulario siguió utilizable. Se buscó «Avenida Arce 2081 La Paz» y la pantalla llenó «Montevideo, Av. Arce 2081, La Paz, Bolivia» y la ciudad «La Paz» |
| 6 | Referencia textual | Se escribió «Porton verde, timbre 2» y se guardó. La columna `reference` quedó con ese texto exacto |
| 7 | Primera dirección principal | La pantalla mostró «Esta es tu dirección principal.» y `addresses.is_primary` quedó en `true`, puesto por el disparador y no por el cliente |
| 5 | Punto geográfico | `st_geometrytype` devolvió `ST_Point`, `st_srid` devolvió `4326` y `st_astext` devolvió `POINT(-68.1285626 -16.5059644)`: longitud primero, en La Paz y no en el océano |
| 4 | Sin volver a geocodificar | Se agregó «- CASA AZUL» a la dirección, texto que ningún geocodificador devuelve, se guardó, se salió de la pantalla y se volvió a entrar: el campo seguía diciendo «Montevideo, Av. Arce 2081, La Paz, Bolivia - CASA AZUL». Si la pantalla consultara al abrir, esa marca habría desaparecido |
| — | Corregir no duplica | Ese mismo guardado dejó la tabla con una sola fila: la corrección actualizó la fila existente en vez de crear una segunda, que además no sería la principal |
| 1 | Centrado en mi posición | Con la clave de Mapas puesta y el emulador situado en Santa Cruz por `adb emu geo fix`, conceder el permiso centró el mapa en la Plaza 24 de Septiembre, con el punto azul del dispositivo bajo el marcador, y llenó «6R89+M4H, Santa Cruz de la Sierra, Bolivia». Antes de conceder, el mapa abría en la posición declarada en `MapDefaults`, que ese día era la Plaza Murillo de La Paz y después se cambió al estadio Félix Capriles de Cochabamba, comprobando que abre ahí |
| 3 | Arrastrar el marcador | Recolocar el marcador reescribe la dirección desde las coordenadas: se comprobó dos veces, «Sucre 64, Santa Cruz de la Sierra, Bolivia» y «Ayacucho 166, Santa Cruz de la Sierra, Bolivia», cada una sobre la calle que el marcador señalaba. **En el emulador el marcador se recoloca tocando y no arrastrando**: el gesto de arrastre no se pudo reproducir con `adb` —`input swipe`, `input motionevent` e `input draganddrop` acaban desplazando la página o el mapa— y el autor dio el criterio por cumplido sobre esa comprobación. Ambos gestos entran por el mismo camino, `onPointPicked`, y la diferencia queda cubierta por prueba unitaria |

También se comprobó contra la base, dentro de transacciones que nunca se
confirmaron, que la vista respeta la política: con `set local role authenticated` y
el identificador del profesional, `my_addresses` devolvió solo la dirección del
profesional; con el del paciente, solo la del paciente. La tabla quedó en cero
filas al terminar.

**Un defecto que solo apareció en el dispositivo.** Al tocar un punto sobre la
Catedral, el marcador se movió y la dirección escrita se quedó como estaba, sin
decir nada. El geocodificador de la plataforma devuelve vacío de vez en cuando —el
mismo punto, minutos después, contestó «Ayacucho 166»—, y la pantalla trataba esa
respuesta como si no hubiera pasado nada. El resultado era peor que un error: el
marcador señalaba un sitio y el texto describía otro, y quien guardara así dejaría
una dirección escrita que no corresponde a su punto. Borrar el texto tampoco sirve,
porque en un dispositivo sin geocodificador esa línea es la dirección entera. Ahora
se conserva el texto y se avisa: «No pudimos convertir ese punto en una dirección
escrita. Revisa que el texto de abajo corresponda al marcador.» Lo mismo vale para
una respuesta que trae ciudad pero no calle. Dos pruebas nuevas lo fijan.

**Lo que encontró la revisión del pull request #4.** Copilot dejó tres
observaciones y se aceptaron dos, las dos reales:

- **Una carrera en el disparador que marca la primera dirección.** La
  comprobación de existencia no estaba serializada, de modo que dos inserciones
  simultáneas del mismo perfil podían reclamar las dos el distintivo de principal
  y perder una contra `idx_addresses_profile_primary`. El índice protegía la
  invariante —nunca habría habido dos principales—, pero el guardado fallaba de
  forma intermitente y sin explicación. Corregido en la migración
  `20260915132619_serialize_first_address`, que bloquea la fila del perfil antes
  de comprobar. Se bloquea el perfil y no la dirección porque no se puede
  bloquear una fila que todavía no existe.
- **Interacciones que seguían vivas durante un guardado.** Solo el formulario
  ignoraba los cambios mientras la petición viajaba; tocar el mapa o buscar una
  dirección movía el marcador, y la fila que volvía del servidor lo devolvía a su
  sitio, descartando lo que la persona acababa de elegir. Ahora las tres puertas
  se cierran igual, y una descripción que llega tarde ya no apaga el indicador de
  guardado. Dos pruebas nuevas lo fijan.

La tercera se descartó: decía que una previsualización pasaba ocho argumentos a
`AddressContent`, que declara siete antes de su `modifier`, y que por eso el
archivo no compilaba. Pasa siete, contadas una por una en las siete
previsualizaciones, y el octavo argumento se habría tipado contra `Modifier` y
habría roto la compilación, que pasa tanto aquí como en integración continua.

**La clave de Mapas.** Se habilitó «Maps SDK for Android» en el proyecto
`salud-en-casa-508102` y `MAPS_API_KEY` vive en `local.properties`, con una clave
propia restringida al paquete y a la huella SHA-1 de depuración. La clave que trae
`app/google-services.json` no sirve para esto: el registro responde
`Google Android Maps SDK: Authorization failure`, y así quedó anotado en
`README.md` junto con el comando que lo revela.

**Dónde se configura la vista inicial del mapa.** Los tres valores viven juntos en
`MapDefaults`, dentro del dominio de la característica, porque responden a la misma
pregunta de producto y son números sin nada de plataforma detrás. La posición de
apertura es el estadio Félix Capriles de Cochabamba, y sus coordenadas no se
escribieron a ojo: se buscó el estadio por su nombre desde la propia aplicación y se
leyó el punto que el geocodificador devolvió, `-17.379268, -66.161794`. El zoom se
separó en dos, porque el mapa tiene dos momentos con exigencias opuestas: abre
alejado, en 15, cuando todavía no sabe dónde vive la persona y lo que hace falta es
reconocer la zona, y se acerca a 17 en cuanto hay un punto concreto que señalar,
que es cuando importa distinguir una puerta de la siguiente.

**Deuda reconocida.** Elegir entre varias direcciones, editarlas y eliminarlas es
HU-06, que llega en el Sprint 3: hoy la pantalla trabaja siempre sobre la dirección
principal. El grafo de navegación sigue sin prueba instrumentada, ahora con una
transición más. Y la comprobación de que una persona no lee las filas de otra se
hizo a mano contra la base: `.claude/rules/testing.md` la pide como prueba
transversal y el proyecto todavía no tiene dónde ejecutarla de forma automática.

**Un hallazgo que no es de esta historia.** Al cablear la agrupación `maps` se
comparó el grafo de dependencias resuelto antes y después: los mapas agregan seis
artefactos, todos estables. Pero el grafo ya traía cinco artefactos que resuelven a
una versión inestable, y ninguno viene de aquí:
`org.jetbrains.androidx.lifecycle:*` en `2.11.0-beta01`, que arrastra Koin 4.2.2, y
`com.google.android.gms:play-services-identity-credentials:16.0.0-alpha08`, que
arrastra `androidx.credentials:credentials-play-services-auth:1.6.0`. Las
dependencias declaradas son estables; las inestables son resoluciones transitivas.
La afirmación de HT-03 —«111 artefactos, ninguno alpha, beta, rc ni SNAPSHOT»— era
cierta en su momento y hoy ya no lo es. Corregirlo es trabajo de HT-03, no de
HU-05, y se anota aquí para que no se pierda.

## Incremento del sprint

Un profesional publica su perfil con tarifa y radio, y registra su domicilio sobre
el mapa. La consulta de cercanía en la base de datos ya lo encuentra.

## Retrospectiva

**El incremento se demostró, pero no entero, y la parte que falta no es código.**
Un profesional declara tipo, especialidad, biografía, experiencia, tarifa y radio,
publica su ficha, enciende su disponibilidad y registra su domicilio sobre el mapa.
Todo eso se recorrió en el emulador. La segunda frase del incremento —«la consulta
de cercanía en la base de datos ya lo encuentra»— **no se puede demostrar
todavía**, y no por una deuda de este sprint: `search_nearby_professionals` lee
`professional_directory`, que exige `verification_status = 'APPROVED'` (INV-07), y
aprobar es trabajo del administrador, que llega con HU-09 en el Sprint 3. El
incremento estaba mal enunciado desde la planificación: prometía algo que depende
de un sprint posterior. Queda anotado para no repetirlo al redactar los incrementos
que faltan.

### Velocidad medida: 21 puntos, otra vez

Se cuenta lo que se cerró: HU-04 (8) + HU-05 (13) = 21. Las dos cumplen la
Definición de Terminado y ninguna se trasladó. Con el Sprint 1 son dos mediciones
seguidas de 21, lo que parece una velocidad estable.

> El Sprint 2 se ejecutó del **14/09/2026** al **15/09/2026**.

**Dos días, y ahí se cae la lectura fácil del número.** La retrospectiva del
Sprint 1 anunciaba que «el Sprint 2 es la primera medición que sí dirá algo sobre
la capacidad, porque se cerrará por tiempo». No ocurrió: este sprint también
terminó cuando se agotó el alcance, no cuando se agotó el plazo. Los 21 puntos
vuelven a medir lo que se planificó, no lo que cabe en una semana. Dos mediciones
idénticas no confirman una velocidad cuando las dos miden la misma cosa equivocada:
confirman que se planificaron 21 puntos dos veces.

**Qué decide esto, y qué queda por decidir.** El Sprint 3 tiene 24 puntos
planificados —HU-06 (3), HU-07 (8), HU-08 (3) y HU-09 (10)—, tres por encima de la
velocidad registrada. La regla del apartado «Estimación» mandaría trasladar la
historia de menor prioridad. Aplicarla aquí sería usar el 21 para algo que el 21 no
mide, y además HU-06 ya se trasladó una vez: moverla de nuevo la convertiría en la
historia que nunca se hace. Hay dos caminos defendibles y la elección es del autor:

1. **Sostener los 24 puntos del Sprint 3** y cerrarlo por plazo, ejecutando hasta
   la fecha de corte y contando lo cerrado. Es la única forma de obtener por fin
   una medición de capacidad, que es el dato que el proyecto lleva dos sprints sin
   tener. Recomendado.
2. **Trasladar HU-08 (3 puntos)** al Sprint 4 y dejar el Sprint 3 en 21. Conserva
   la coherencia con la regla escrita, a costa de seguir sin medir la capacidad y
   de cargar el Sprint 4, que ya tiene 23.

### Qué funcionó

- **Ordenar los criterios por reversibilidad**, que era el primer cambio que el
  Sprint 1 mandó aplicar. Funcionó dos veces: en HU-04 el estado «todavía no
  publicado» del perfil público se comprobó antes de cualquier intento de
  aprobación, porque aprobar lo vuelve inalcanzable; y en HU-05 se denegó el
  permiso de ubicación antes de concederlo, porque conceder deja la pantalla en el
  estado más rico y esconde el más pobre.
- **Verificar el contrato contra la fuente antes de construir sobre él.** Antes de
  escribir una línea del cliente de direcciones se comprobó, consultando
  `json_populate_record` sobre el tipo de la tabla, que PostgREST acepta un punto
  de PostGIS como texto. Esa es exactamente la ruta que PostgREST usa, así que la
  respuesta valía. La alternativa —escribir la capa entera y descubrirlo al
  probarla— habría costado el diseño completo.
- **Ensayar contra la base dentro de transacciones que nunca se confirman.** El
  disparador que marca la primera dirección, la vista que proyecta el punto y las
  políticas que la filtran se comprobaron con filas reales de dos perfiles
  distintos, y la tabla quedó en cero filas. Verificación sin residuo.
- **Medir en vez de afirmar.** Al cablear la agrupación de mapas se comparó el
  grafo de dependencias resuelto antes y después, y el dato desmintió una
  afirmación que el propio `plan.md` sostenía desde HT-03: hay cinco artefactos
  transitivos en versión inestable, y ninguno viene de los mapas.
- **La verificación en dispositivo encontró lo que las pruebas no podían.** El
  defecto del punto que el geocodificador no sabe nombrar solo apareció con el mapa
  dibujando, porque hasta entonces no había coordenadas reales que consultar.
- **La revisión automática del pull request #4 encontró dos defectos reales** que
  ni las pruebas ni la verificación manual habían visto: una condición de carrera
  en el disparador de la primera dirección y un guardado al que se le podía mover
  el marcador por debajo. Se descartó una tercera observación comprobándola contra
  el código, igual que en el Sprint 1 con el pull request #2. La revisión vale por
  lo que encuentra, no por lo que afirma: las tres se verificaron antes de tocar
  nada.

### Qué no funcionó

- **Dos sprints seguidos terminaron esperando algo que no es código.** En HU-04 fue
  la aprobación de un profesional, que exige un administrador que todavía no
  existe; en HU-05, una clave del proveedor de mapas que solo el autor puede crear.
  En ambos casos el trabajo estaba hecho y el criterio quedó abierto por una
  dependencia externa que se descubrió al final.
- **La automatización del emulador no reproduce gestos complejos.** El arrastre del
  marcador no se pudo sintetizar con `adb` por tres vías distintas —`input swipe`,
  `input motionevent` e `input draganddrop`—: todas acaban desplazando la página o
  el mapa. El criterio se aceptó sobre la recolocación por toque, que recorre el
  mismo camino de código.
- **El editor mostró errores que la compilación desmintió.** Android Studio señaló
  `Cannot access class ComposableFunction1` en varias pantallas mientras
  `./gradlew build` pasaba limpio. El analizador del editor trae un Kotlin
  empaquetado distinto del que usa Gradle. No hay nada que corregir en el proyecto,
  pero cuesta tiempo y confianza cada vez que aparece.
- **Editar un documento de dos mil cuatrocientas líneas con un guion es
  arriesgado.** Un ancla de búsqueda que no era única duplicó ochocientas líneas de
  `plan.md`. Se detectó y se revirtió en la misma sesión, pero un documento que es
  evidencia académica no debería depender de que el error se note.

### Qué cambiar en el Sprint 3

1. **Identificar al planificar qué criterios dependen de algo externo** —una
   credencial, una aprobación, una cuenta, un dispositivo— y resolverlos al
   principio del sprint, no al llegar a ellos. Es el mismo tropiezo dos veces
   seguidas.
2. **Saldar la deuda de `androidTest`.** Se arrastra desde el Sprint 1 y ya son
   cinco transiciones de navegación sin prueba automática, una de ellas con
   argumento. El Sprint 3 agrega Storage y el panel del administrador, que es más
   superficie sobre la misma red ausente.
3. **Cerrar el sprint por plazo al menos una vez**, para que la velocidad deje de
   ser el eco del alcance planificado. Sin eso, el número seguirá siendo 21 diga lo
   que diga la capacidad real.
4. **Revisar la afirmación de HT-03 sobre versiones estables** a la luz de los
   cinco artefactos transitivos medidos en este sprint, y decidir si se documenta
   la excepción o se fija una versión.

---

# Sprint 2.5 — Rol múltiple con rol activo

**Estado:** `[x]` cerrado el 2026-10-04. Iniciado el 2026-10-01. Las cuatro
historias terminadas y los dieciséis criterios verificados.

**Objetivo del sprint.** Una misma persona tiene el rol de paciente y el de
profesional, y cambia entre ellos con un control en «Mi cuenta».

**Objetivo específico.** 2 · Desarrollar la gestión de perfiles de usuarios.

**Puntos:** 23.

> **Por qué este sprint existe y por qué interrumpe al Sprint 3.** El 2026-10-01,
> con el Sprint 3 ya en curso y HU-06 a falta de su verificación en dispositivo,
> una reunión con la contraparte del negocio cambió un supuesto que el proyecto
> daba por cerrado desde el Sprint 1: **el rol deja de ser único por persona.** Lo
> pedido es que alguien sea paciente y profesional a la vez y alterne entre ambos,
> como un conductor de inDrive o Uber alterna entre conducir y viajar.
>
> SCRUM admite repriorizar el Product Backlog en cualquier momento; lo que no
> admite es cambiar el alcance del sprint en curso sin hacerlo explícito. Por eso
> el Sprint 3 queda **suspendido** con HU-06 en `[~]`, y se reanuda al cerrar este.

> **Por qué se hace ahora y no después.** Dos razones medidas, no supuestas.
>
> La primera: el costo crece con cada sprint. Las historias de verificación del
> Sprint 3 ramifican por rol —HU-07 pide un conjunto de documentos al paciente y
> otro al profesional— y las de los Sprints 5 a 9 construyen encima de las
> políticas de rol. Hacerlo después es rehacer todo lo que se haya apoyado en el
> supuesto viejo.
>
> La segunda: hoy es barato, y eso se comprobó antes de decidir. Solo **dos**
> políticas leen `profiles.role` de forma directa —`patients_insert_own` y
> `professionals_insert_own`— más el ayudante `is_admin()`. Todo el resto del
> esquema resuelve por pertenencia (`patient_id = auth.uid()`,
> `professional_id = auth.uid()`, o la existencia de una fila en `professionals`),
> de modo que las políticas de `services`, `payments`, `messages`, `reviews` y
> `request_offers` sobreviven intactas. La estimación inicial —«hay que reescribir
> las políticas de las quince tablas»— era pesimista y se descartó al leer el
> esquema efectivo.

> **Los números de historia no se reasignan.** Este sprint usa HU-34 a HU-36 y
> HT-16, que son los siguientes libres, en lugar de insertar HU-06a o renumerar las
> existentes. Renumerar rompería todas las referencias cruzadas de
> `docs/decisions.md`, que es evidencia ya entregada. El número es un
> identificador, no un orden de ejecución.

## Decisión de diseño que gobierna el sprint

**`profile_roles` dice qué roles tiene una persona. `profiles.active_role` dice en
cuál está. La seguridad se decide por posesión, nunca por rol activo.**

La segunda mitad es la parte no evidente. Filtrar las políticas por el rol activo
sería teatro de seguridad: quien tiene ambos roles cambia de rol cuando quiere, de
modo que una política que mire el rol activo no impide nada, solo se la saltan
cambiando. El rol activo es **estado de presentación**, y vive en el servidor por
continuidad —sobrevive a una reinstalación y no depende de `DataStore`—, no porque
alguna política lo necesite.

El invariante «el rol activo es uno de los que tengo» no lo valida la aplicación:
lo impone el motor con una clave foránea compuesta contra la clave primaria de
`profile_roles`. Es el mismo criterio que INV-10 aplica a los pagos, una
restricción del motor y no una validación de la aplicación. Registrado en
`docs/decisions.md`, 2026-10-01.

## Orden de ejecución

**HU-34 → HT-16 → HU-35 → HU-36.** El orden no es el de la lista y la razón
importa: HU-34 es exactamente lo que elimina la condición que hoy impide la
autonegociación, así que HT-16 va inmediatamente después para no dejar una ventana
con el hueco vivo en el esquema aplicado. HU-35 llega tercera porque es la primera
que se puede demostrar a mano con los dos roles. HU-36 queda al final porque es la
única que toca historias ya cerradas —HU-05 y HU-06— y por tanto la de mayor
riesgo para la evidencia existente.

## Estado al 2026-10-02

**El esquema está aplicado y verificado. Lo que falta es la verificación en
dispositivo.**

| Qué | Estado |
|---|---|
| Las migraciones | **Cinco aplicadas** sobre el proyecto remoto: las tres planificadas y dos correcciones que los experimentos destaparon |
| El código Kotlin de las cuatro historias | Escrito |
| Pruebas unitarias | **223 pruebas, cero fallos.** La suite pasó de 194 a 223: se retiraron seis y se agregaron treinta y cinco |
| `./gradlew build` completo | Concluye sin error |
| `ktlintCheck` y `staticAnalysis` | Sin hallazgos |
| Pruebas obligatorias de política | **Las cuatro verificadas** con experimento SQL. Ver abajo |
| INV-02 sobre las dieciséis tablas | **Verificado:** cero tablas descubiertas |
| Verificación en dispositivo | **Recorrida el 2026-10-02, ampliada el 2026-10-03 y cerrada el 2026-10-04.** Los dieciséis criterios verificados. Encontró dos defectos de interfaz, ya corregidos |

### El proyecto estaba pausado, y la restauración lo resolvió

`lckgbklfmkjpuvmebuha.supabase.co` respondía `NXDOMAIN`: el plan libre de Supabase
pausa un proyecto tras unos días sin actividad y le retira el registro DNS. La
última actividad era del 2026-09-16, quince días antes. El autor lo restauró desde
el panel.

**Un detalle que conviene tener anotado:** inmediatamente después de restaurar, el
CLI respondió `relation "public.profiles" does not exist`. No era pérdida de datos
sino que la restauración no había terminado. Un minuto más tarde las dieciséis
tablas y los cinco perfiles estaban ahí. Conviene no interpretar ese error como
pérdida de esquema y volver a consultar antes de concluir nada.

### Estado antes y después de aplicar

Se comprobó antes de empujar, porque una migración aplicada no se edita nunca.

| Dato | Antes | Después |
|---|---|---|
| Perfiles | 5 — 2 `PATIENT`, 3 `PROFESSIONAL`, ninguno sin rol | 5, con su rol en `profile_roles` y como `active_role` |
| Tablas del esquema público | 15 | 16 |
| Vistas | 2 | 3, con `my_roles` |
| Columnas `role`, `average_rating`, `total_reviews` en `profiles` | 3 | 0 |
| Profesionales aprobados | 0, los tres en `PENDING` | 0 |
| Direcciones | 3 | 3 |
| Servicios y reseñas | 0 y 0 | 0 y 0 |

**El relleno salió exacto:** los cinco perfiles conservan el rol que tenían, como
rol que tienen y como rol activo, con su fila de `patients` o de `professionals`
intacta y sin solapamiento.

**Por qué el riesgo del relleno de HU-36 resultó nulo.** Marca como base
profesional la dirección principal de quien tiene el rol profesional, y la
preocupación era que un profesional aprobado desapareciera de las búsquedas. No
podía ocurrir: los tres profesionales están en `PENDING`, así que ninguno figuraba
en `professional_directory`, y además ninguno tenía dirección principal. El relleno
marcó cero filas, que es lo correcto para ese estado.

**Se comprobó también que nada dependía de `profiles.role` fuera de lo previsto.**
El catálogo del sistema devolvió exactamente tres dependencias —`idx_profiles_role`
y las dos políticas de inserción—, las tres tratadas por la migración. Las cuatro
funciones que la leen en su cuerpo no aparecen, porque PostgreSQL no rastrea
referencias de columna dentro de un cuerpo; la migración las reemplaza de forma
explícita.

### Las cuatro pruebas obligatorias de política, verificadas

Ninguna puede ser de JUnit: lo que verifican se ejecuta dentro de PostgreSQL.
`.claude/rules/testing.md` recoge desde este sprint que esa clase de prueba se
verifica con un experimento SQL simulando al usuario autenticado, **dentro de una
transacción que termina en `rollback`**. Es el método con el que se verificaron los
disparadores de HU-05 y se encontró la causa del defecto de HU-06.

| Prueba obligatoria | Resultado |
|---|---|
| `aDualRoleUserNeverSeesTheirOwnRequestInTheProfessionalInbox` | **Pasa.** Con los dos roles, aprobado, con base y servicio activo: la bandeja devuelve la solicitud ajena y no la propia |
| `aDualRoleUserCannotOfferOnTheirOwnRequest` | **Pasa.** Ofertar sobre la propia solicitud da `42501`, rechazo de política; sobre una ajena, se acepta. Que discrimine es lo que vale |
| `reputationAsAProfessionalExcludesRatingsReceivedAsAPatient` | **Pasa.** Calificado con 2 en el servicio donde fue paciente y con 5 donde fue profesional: `patients.average_rating` da 2.00 y `professionals.average_rating` da 5.00 |
| `ratingsReceivedAsAPatientNeverBecomePublic` | **Pasa.** Un tercero ve una sola reseña, la recibida como profesional |

Se verificaron además `nearbySearchUsesTheProfessionalBaseAndNotThePrimaryAddress`
—encontrado a 5 km de la base, no encontrado a 5 km del domicilio, con los dos
puntos a unos 40 km uno del otro—,
`markingAddressAsProfessionalBaseUnmarksThePreviousOne`, y que `my_roles` devuelve
solo la fila propia con su rol activo y su conjunto.

**Al terminar, los datos reales quedaron idénticos:** 5 perfiles, 5 filas de rol,
2 `patients`, 3 `professionals` en `PENDING`, 3 direcciones sin marcar, y cero
solicitudes, ofertas, servicios y reseñas. Verificación sin residuo.

### Dos defectos que los experimentos destaparon

Ninguno lo habría encontrado una prueba de JUnit, y ninguno estaba en el alcance
planificado del sprint. Las dos correcciones son migraciones nuevas, porque las
que las contenían ya estaban aplicadas.

**1. `request_offers_insert_participants` recursaba sobre su propia tabla. Era
preexistente.** Su `with check` validaba el hilo de la contraoferta con
`exists (select 1 from public.request_offers parent ...)`, y una expresión de
política que consulta su propia relación hace que PostgreSQL levante `42P17`,
«infinite recursion detected in policy». La política estaba **rota para toda
inserción**, no solo para la autonegociación: la primera oferta de un profesional
igual que la contraoferta de un paciente.

El defecto llegó con la migración correctiva del 2026-09-12 y sobrevivió quince
días sin que nada lo notara, porque emitir una oferta es HU-17, del Sprint 6, y
nunca se había insertado ninguna. **Lo que lo delató fue que las dos inserciones
del experimento —la ilegítima y la legítima— fallaran con el mismo `SQLSTATE`.** Un
rechazo de política y un error de recursión no son el mismo resultado, y solo uno
estaba previsto. Si el experimento hubiera comprobado únicamente que la inserción
ilegítima fallaba, habría dado por buena una política inservible.

Corregido en `20261002135541_fix_offer_policy_recursion`: la consulta del hilo pasa
a la función `offer_continues_thread`, `security definer`, que corre fuera de las
políticas de la tabla que lee. Es el remedio habitual de una autorreferencia de
RLS.

**2. `reviews_select_visible` no podía mostrar una reseña a un tercero. Lo
introduje yo en este sprint.** Al estrechar la visibilidad pública a las reseñas
recibidas como profesional —que es lo que RF-12.4 pide— escribí la condición como
`exists (select 1 from public.services s where ...)`. Esa subconsulta corre bajo
las políticas de `services`, y `services_select_participants` solo muestra un
servicio a su paciente o a su profesional. Para cualquier otro —que es
exactamente el público de una reseña pública— la subconsulta no devuelve nada, de
modo que **ninguna reseña era pública en absoluto.** La corrección de la fuga había
cerrado la puerta entera.

La causa se aisló evaluando las tres condiciones de la política por separado como
ese tercero: veía al destinatario en `professional_directory` pero no la fila de
`services`. El directorio se salva porque es `security_invoker = false` y corre con
los privilegios de su dueño; la subconsulta sobre `services` no tenía esa exención.

Corregido en `20261002135908_fix_public_review_visibility`, con el mismo remedio:
la función `was_the_professional_of`, `security definer`.

**La lección, que conviene aplicar al resto del proyecto.** Una subconsulta dentro
de una política queda sujeta a las políticas de la tabla que consulta. Eso vale
para toda política futura que necesite mirar otra tabla, y hay dos salidas: una
vista `security_invoker = false`, o una función `security definer` que devuelva un
booleano y nada más. Las políticas vigentes que ya consultan otra tabla
—`professional_services_select_public`, `availability_slots_select_public`,
`messages_*`, `payments_*`, `services_*`— deben revisarse con este criterio al
llegar a su sprint.

### La verificación en dispositivo, y los dos defectos de interfaz que encontró

Recorrida el 2026-10-02 en el emulador `Pixel_9_Pro`, Android 17, con la cuenta
real `chris.ledezma.s@gmail.com`, dirigiendo el dispositivo por `adb` y capturando
pantalla en cada paso. Se eligió esa cuenta porque llegaba al sprint con un solo
rol y con dos direcciones, de modo que recorre las tres historias de una pasada.

**Se respetó el orden por reversibilidad** que el Sprint 1 mandó aplicar: el
criterio de HU-35 que exige ver «Mi cuenta» **sin** el control segmentado se
verificó antes de activar el segundo rol, porque activarlo lo vuelve inalcanzable
para siempre. `profile_roles` es de solo agregar (FA-09).

Cada paso se contrastó contra la base de datos, no contra lo que la pantalla
decía.

**Defecto 1: «Editar» y «Eliminar» desaparecieron de la lista de direcciones.**
La fila de acciones era un `Row`, que no envuelve. Al agregar «Usar como base
profesional» como cuarta acción, las dos últimas dejaron de componerse —no
quedaron recortadas fuera de pantalla: desaparecieron del árbol de vistas, lo que
se comprobó con `uiautomator dump`—. El resultado era una dirección imposible de
editar o eliminar, es decir, dos criterios de HU-06 rotos por una historia
distinta. Corregido con `FlowRow`, que además es lo que mantiene las acciones
alcanzables al 200 %.

**Defecto 2: el control segmentado se rompía al 200 % de tamaño de fuente.**
«Profesional de salud» desbordaba su segmento y se salía de la píldora, lo que
incumple el punto 9 de la Definición de Terminado. `docs/design-system.md` ya lo
advertía en su apartado 5: el control segmentado no admite etiquetas largas al
ancho de un teléfono, y es exactamente la razón por la que HU-04 usó un grupo de
opciones para el tipo de profesional. Corregido con etiquetas cortas propias del
control —«Paciente» y «Profesional»—, que no son ambiguas bajo un encabezado que
ya dice «Estás usando la aplicación como».

**Ninguno de los dos lo podía atrapar una prueba de JUnit.** Son defectos de
disposición, y el proyecto no tiene pruebas de interfaz: la deuda de `androidTest`
se arrastra desde el Sprint 1 y ya se señaló en la retrospectiva del Sprint 2.
Este sprint agrega dos casos reales a esa deuda, que ahora tiene ejemplos
concretos de lo que deja pasar.

### Lo que queda: nada

Los dieciséis criterios están verificados. Conviene dejar anotado por qué cuatro
de ellos llegaron a darse por imposibles, porque los cuatro motivos resultaron
equivocados y de maneras distintas.

| Criterio | Por qué se dijo «no verificable» | Qué resultó |
|---|---|---|
| El cambio de rol que falla (HU-35) | «Exige provocar una caída de red a mitad de la petición» | `adb shell cmd connectivity airplane-mode enable` la provoca. El entorno siempre lo permitió; faltaba intentarlo |
| La elección de rol en el primer ingreso (HU-34) | «Exige una cuenta de Google nueva» | El emulador ya tenía una segunda, `ledezma.aramayo.73@gmail.com`, que nunca había ingresado. No hacía falta crear nada; hacía falta mirar `dumpsys account` |
| La lista de direcciones de un paciente puro (HU-36) | «La cuenta usada tiene los dos roles» | Cierto de esa cuenta, no del proyecto. La cuenta nueva, eligiendo «Paciente», lo demuestra |
| Agregar dos veces el mismo rol (HU-34) | «No hay forma de pedirlo desde la aplicación» | Cierto, y por eso mismo no era un criterio de interfaz sino de motor. `supabase db query --linked` lo resuelve en dos comandos |

La lección vale para los sprints que vienen: **«no verificable» es una afirmación
sobre el entorno, y hay que comprobarla igual que cualquier otra.** Las cuatro se
escribieron por inspección del código y de los datos, sin probar si el entorno
daba el camino. Antes de anotar un criterio como no demostrable, el paso que falta
es intentarlo una vez.

El cuarto agrega un matiz propio: el camino existía en el CLI desde siempre
—`supabase db query`— y se había dado por ausente al leer una salida de ayuda
truncada. El procedimiento quedó anotado en `.claude/rules/testing.md` para que la
próxima prueba de política no vuelva a buscarlo.

**`ledezma.aramayo.73@gmail.com` pasa a ser la cuenta de un solo rol del
proyecto.** Tiene el rol de paciente y una dirección en Cochabamba. Conviene no
activarle el rol profesional: `profile_roles` es de solo agregar (FA-09), de modo
que es la única cuenta con la que una historia futura podrá demostrar algo que un
doble rol ya no puede.

**Y una limitación heredada:** ningún profesional está aprobado, así que la base
profesional se puede declarar y marcar, pero su efecto en la búsqueda no se puede
demostrar dentro de la aplicación. Depende de HU-09, igual que ya ocurrió en el
Sprint 2. Sí se demostró con experimento SQL.

### Un hallazgo aparte, anterior a este sprint

**Las tres direcciones del autor están sin marcar como principal.** Ninguna tiene
`is_primary`, cuando `addresses_first_is_primary` debería haber forzado la primera
de cada persona. La explicación más probable es que la que era principal se
eliminó: ese disparador solo actúa al insertar, de modo que borrar la dirección
principal deja a la persona sin ninguna y nada promueve otra.

No lo causó este sprint y no se corrigió aquí. Importa más ahora, porque
`is_professional_base` tiene exactamente la misma forma: eliminar tu base te saca
de las búsquedas en silencio. Queda anotado para HU-06, que es la historia dueña de
la eliminación de direcciones y sigue abierta en el Sprint 3.

### HU-34 · Tener más de un rol `[x]` — 8 puntos

> Como **persona que usa la aplicación**, quiero **tener el rol de paciente y el de
> profesional a la vez**, para **pedir atención y prestarla sin crear dos cuentas**.

**Criterios de aceptación**

- [x] Dado que ingreso por primera vez, cuando la sesión se establece, entonces se
      me pide elegir con cuál de los dos roles empiezo. **Verificado el 2026-10-03**
      con `ledezma.aramayo.73@gmail.com`, una segunda cuenta de Google del
      emulador que nunca había ingresado. Tras `pm clear` y el selector nativo,
      apareció «¿Cómo vas a usar Salud en Casa?» con exactamente dos opciones.
- [x] Dado que ya tengo un rol, cuando abro «Mi cuenta», entonces se me ofrece
      activar el otro. **Verificado el 2026-10-02.**
- [x] Dado que activo el segundo rol, cuando la operación concluye, entonces se
      crea su registro específico y ese rol queda como mi rol activo.
      **Verificado el 2026-10-02**, y contrastado contra la base: la fila de
      `patients` existe y `active_role` quedó en `PATIENT`.
- [x] Dado que ya tengo un rol, cuando intento agregarlo otra vez, entonces la
      operación se rechaza y nada se escribe. **No tiene gesto en la aplicación**
      —la pantalla deja de ofrecer el rol que ya se tiene—, así que se verificó
      el 2026-10-04 con un experimento SQL sobre `ledezma.aramayo.73@gmail.com`,
      en dos mitades y dentro de transacciones con `rollback`:

      | Mitad | Resultado |
      |---|---|
      | `select public.add_my_role('PATIENT')` | **`P0001: role_already_held`**, levantado en la línea 30 de la función. Es el error que `AddRoleUseCase` traduce a `ProfileError.RoleAlreadyHeld` |
      | `insert into public.profile_roles ...`, saltándose la función | **`23505`**, violación de `profile_roles_pkey` |

      «Y nada se escribe» se comprobó aparte: antes y después, `profile_roles`
      devuelve una sola fila para esa persona, con el mismo `created_at`.

      **Las dos mitades hacían falta.** La función rechaza antes de llegar a la
      tabla, de modo que llamarla sola prueba el guardia y no la clave primaria.
      Si solo hubiera corrido la primera, el rol sería único por cortesía del
      código, y PostgREST expone la tabla a cualquiera con sesión. Lo cubren
      además `rejectsAddingARoleThePersonAlreadyHolds` y la clave primaria.
- [x] El rol de administrador nunca aparece como opción ni puede autoasignarse.
      **Verificado el 2026-10-02:** la pantalla ofrece exactamente dos opciones.
      No es una condición de la interfaz: `AssignableRole` no tiene constante para
      `ADMIN` y el `with check` de `profile_roles_insert_own` lo rechaza sin
      excepción, ni siquiera para un administrador.
- [x] Dado que tengo un rol activo, cuando se lee, entonces es necesariamente uno
      de los roles que tengo. No existe forma de activar un rol ausente.
      **Verificado el 2026-10-02** por la clave foránea compuesta y por
      `activeRoleMustBeOneOfTheHeldRoles`.

**Requisitos:** RF-01.4, RF-01.5, RF-01.8.

**Tareas técnicas.** Migración `multi_role_identity` · `ProfileRoles` en dominio
con su fábrica · `AddRoleUseCase` en lugar de `ChooseRoleUseCase` ·
`GetRoleUseCase` devuelve el conjunto y el rol activo · vista `my_roles` y su
objeto de transporte · `ProfileRepository.getProfile()` trae los dos lados que la
persona tenga · invitación a activar el otro rol en «Mi cuenta».

**La migración, en este orden exacto**, porque cada paso depende del anterior:

| # | Paso | Por qué va aquí |
|---|---|---|
| 1 | `create table profile_roles` con RLS y tres políticas | La inserción propia lleva `role <> 'ADMIN'` en su `with check`: ahí muere la autoasignación de administrador, en la política y no en una condición de pantalla |
| 2 | Relleno desde `profiles.role` | Antes de que exista la clave foránea que lo exige |
| 3 | `add column active_role` y la clave foránea compuesta | La clave solo puede apuntar a una tabla que ya tiene filas |
| 4 | Relleno de `active_role` | — |
| 5 | `is_admin()` pasa a leer `profile_roles` | Sigue `security definer`, así que no recursiona sobre la tabla nueva |
| 6 | `patients_insert_own` y `professionals_insert_own` pasan a `exists` | Son las dos únicas políticas que leen `profiles.role` |
| 7 | `add_my_role()` reemplaza a `assign_my_role()` | Inserta el rol, crea la fila del rol, y deja el rol nuevo como activo |
| 8 | `save_my_profile()` despacha por `active_role` | Conserva el espíritu de la decisión del 2026-09-14 —el servidor decide, un argumento enviado por error se ignora— y además resuelve el doble rol: se edita el rol en el que se está |
| 9 | `create view my_roles` | Una consulta en vez de dos, con `security_invoker = true` para que las políticas propias sigan aplicando. Mismo patrón que `my_addresses` |
| 10 | Baja de lo que queda sin dueño | `profiles.role`, `idx_profiles_role`, el disparador `profiles_guard_role` con su función, y `assign_my_role` |

**Por qué se elimina `profiles.role` y no se conserva como «rol principal».** Dos
fuentes de verdad para el mismo hecho divergen, y la copia que nadie lee se queda
obsoleta sin que nada falle. Es exactamente el «campo suelto para salir del paso»
que `docs/decisions.md` prohíbe el 2026-09-08.

**`UserRole` y `AssignableRole` sobreviven sin cambios**, y con ellos la prueba
obligatoria `adminRoleIsNeverSelfAssignable`. La decisión del 2026-09-13 que
separó los dos tipos sigue siendo correcta: lo que cambia es la premisa de su
contexto —que `profiles.role` era un valor único—, no su conclusión.

### HT-16 · Cerrar los huecos que el rol múltiple destapa `[x]` — 5 puntos

Historia técnica habilitadora. No entrega pantalla: cierra tres huecos que el rol
único mantenía inalcanzables y que HU-34 vuelve alcanzables.

**Los tres huecos, y qué los contiene hoy**

| Hueco | Estado antes de HU-34 | Qué lo contenía |
|---|---|---|
| Autonegociación | Abierto y latente | La regla de rol único. Sin dos roles, la condición previa no existe |
| Reseñas de paciente visibles en público | Abierto e inalcanzable | `services` no tiene política de inserción, así que no puede existir un servicio ni, por tanto, una reseña |
| Reputación mezclada entre roles | Abierto e inalcanzable | Lo mismo: `recalculate_reputation()` solo se dispara sobre `reviews` |

**«No hay código cliente» no contiene nada.** Conviene dejarlo escrito porque es
el razonamiento equivocado más tentador: PostgREST expone todas las tablas, así que
cualquiera con una sesión consulta `service_requests` aunque la aplicación no tenga
esa pantalla. Lo que de verdad contiene la autonegociación es que exige ser
profesional con `verification_status = 'APPROVED'`, y eso solo lo pone un
administrador, que llega con HU-09.

**Criterios de aceptación**

Los cinco se verificaron el 2026-10-02 con experimentos SQL dentro de
transacciones con `rollback`, sobre el proyecto remoto. Los datos reales quedaron
idénticos al terminar.

- [x] Dado que tengo los dos roles, cuando publico una solicitud como paciente,
      entonces no aparece en mi propia bandeja de profesional.
- [x] Dado que tengo los dos roles, cuando intento ofertar sobre mi propia
      solicitud, entonces la base de datos lo rechaza. **Rechazo con `42501`,
      política**, y la oferta sobre una solicitud ajena se acepta. Que discrimine
      es lo que vale, y es lo que destapó el defecto de recursión.
- [x] Dado que tengo los dos roles, cuando busco profesionales cerca, entonces no
      aparezco entre los resultados.
- [x] Dado que recibí una calificación como paciente, cuando se me aprueba como
      profesional, entonces esa calificación no se vuelve pública. **Un tercero ve
      una sola reseña, la recibida como profesional.**
- [x] Dado que tengo calificaciones en ambos roles, cuando se consulta mi
      reputación como profesional, entonces no incluye las que recibí como
      paciente. **2.00 como paciente y 5.00 como profesional**, calculadas por
      separado sobre la misma persona.

**Requisitos:** RF-12.3, RF-12.4, RN-01, RN-10, INV-07, INV-11.

**Tareas técnicas.** Migración `close_dual_role_gaps`: `patient_id <> auth.uid()`
en `service_requests_select_inbox` y en la rama profesional de
`request_offers_insert_participants` · `pro.id <> auth.uid()` en
`search_nearby_professionals` · `reviews_select_visible` pasa a exigir que el
destinatario haya sido el profesional **de ese servicio** · `average_rating` y
`total_reviews` bajan de `profiles` a `patients` y `professionals` ·
`recalculate_reputation()` deriva el lado desde `services` · los guardias de
columnas gestionadas por el servidor se reparten entre las tres tablas ·
`professional_directory` lee la reputación del profesional.

**Esto supera parcialmente la decisión del 2026-09-11**, que fijó la reputación en
`profiles` con el argumento de que es un atributo de la persona y no del rol. Era
correcto mientras una persona tuviera un solo rol. Con dos, el promedio del
profesional arrastraría las calificaciones que recibió como paciente, y
`reviews_select_visible` haría públicas unas reseñas que RF-12.4 solo hace públicas
para el profesional.

### HU-35 · Cambiar de rol con un switch `[x]` — 5 puntos

> Como **persona con los dos roles**, quiero **cambiar de rol con un control**,
> para **usar la aplicación como paciente o como profesional sin volver a
> configurar nada**.

**Criterios de aceptación**

- [x] Dado que tengo los dos roles, cuando abro «Mi cuenta», entonces veo un
      control con ambos y el activo marcado. **Verificado el 2026-10-02.**
- [x] Dado que cambio de rol, cuando la operación concluye, entonces «Mi perfil»
      muestra las secciones del rol nuevo. **Verificado el 2026-10-02 en los dos
      sentidos**, y con el detalle que importa: como paciente **no aparece el
      interruptor de disponibilidad**, aunque esa persona tiene fila en
      `professionals`. Es el defecto que el refactor corrige.
- [x] Dado que tengo un solo rol, cuando abro «Mi cuenta», entonces el control no
      aparece y en su lugar se ofrece activar el otro rol. **Verificado el
      2026-10-02, antes de activar el segundo rol**, porque después deja de ser
      alcanzable. **Reconfirmado el 2026-10-03** con `ledezma.aramayo.73@gmail.com`:
      «Mi cuenta» muestra «Activar mi perfil profesional» y ningún control
      segmentado. Esta vez la comprobación es repetible, porque esa cuenta sigue
      teniendo un solo rol.
- [x] Dado que el cambio falla, cuando vuelvo a la pantalla, entonces sigo en el
      rol anterior y se me explica qué pasó. **Verificado el 2026-10-03** en el
      emulador `Pixel_9_Pro`, provocando la caída de red con
      `adb shell cmd connectivity airplane-mode enable` y
      `adb shell cmd wifi set-wifi-enabled disabled` antes de tocar el otro
      segmento. El control quedó en «Profesional», apareció «No hay conexión.
      Revisa tu red e inténtalo de nuevo.», y tras restaurar la red, cerrar la
      aplicación con `am force-stop` y volver a abrirla, el rol activo que el
      servidor devolvió seguía siendo el profesional: la escritura no llegó a
      ocurrir. Lo cubre además `switchFailureKeepsThePreviousActiveRole`.
- [x] Dado que cambié de rol, cuando cierro la aplicación por completo y la vuelvo
      a abrir, entonces sigo en el rol que dejé activo. **Verificado el 2026-10-02**
      con `am force-stop` y relanzamiento.

**Requisitos:** RF-01.8, RF-02.1, RF-02.2.

**Tareas técnicas.** `SwitchActiveRoleUseCase` · `AccountViewModel` y
`AccountScreen` con el control segmentado · `ProfileViewModel` y `ProfileScreen`
ramifican por rol activo en lugar de por rol único · cadenas nuevas en ambos
idiomas.

**El cambio de rol se escribe con una consulta del cliente, no con una función
almacenada.** Es una sola columna y la clave foránea compuesta ya garantiza lo
único que había que garantizar, así que no hay atomicidad que proteger. Es la misma
razón por la que la dirección se guarda con una consulta y no con una función
almacenada, registrada el 2026-09-15.

**Y se escribe con `update { set(...) }`, nunca con un objeto serializable.** El
proyecto ya pagó este defecto en HU-06: `install(Postgrest)` no recibe
`encodeDefaults = true`, de modo que un campo cuyo valor coincide con su valor por
omisión se omite del cuerpo y el `PATCH` que viaja es `{}`. Ver «Marcar como
principal no escribía nada», `docs/decisions.md`, 2026-09-16.

**El control segmentado ya existe y nadie lo consumía.** `SegmentedControl` se
construyó en HT-06 y hasta hoy solo vivía en sus previsualizaciones. Esta historia
es su primer consumidor real.

### HU-36 · Separar mi domicilio de mi base profesional `[x]` — 5 puntos

> Como **profesional que también es paciente**, quiero **declarar desde qué
> dirección cubro mi zona**, para **que las búsquedas me encuentren donde trabajo y
> no donde quiero ser atendido**.

**Criterios de aceptación**

- [x] Dado que tengo el rol profesional, cuando abro mis direcciones, entonces
      puedo marcar una como mi base profesional. **Verificado el 2026-10-02.**
- [x] Dado que marco una base profesional, cuando marco otra, entonces la anterior
      deja de serlo. **Verificado el 2026-10-02** y contrastado contra la base:
      marcar «trabajo» dejó «casa» en falso.
- [x] Dado que mi domicilio y mi base profesional son distintos, cuando un paciente
      busca cerca de mi base, entonces aparezco; cuando busca cerca de mi
      domicilio, no. **Verificado el 2026-10-02 con experimento SQL**, con los dos
      puntos a unos 40 km y un radio de 5 km.
- [x] Dado que soy profesional sin base declarada, cuando un paciente busca,
      entonces no aparezco en los resultados. **Lo garantiza el esquema sin código
      nuevo:** la unión con `addresses` en `search_nearby_professionals` es interna,
      así que sin base no hay fila de resultado. Es el mismo mecanismo que HU-06
      verificó para la dirección principal.
- [x] Dado que solo tengo el rol de paciente, cuando abro mis direcciones, entonces
      la marca de base profesional no aparece. **Verificado el 2026-10-03** con
      `ledezma.aramayo.73@gmail.com`, que eligió «Paciente» en su primer ingreso.
      Con dos direcciones registradas, la lista ofrece «Marcar como principal»,
      «Editar» y «Eliminar», y en ningún momento «Usar como base profesional»:
      cero ocurrencias en `uiautomator dump`. **La ausencia es por rol y no por
      disposición**, y eso se comprobó de dos maneras, porque el defecto del
      2026-10-02 fue exactamente una acción que desaparecía del árbol de vistas
      sin quedar recortada. Primero, la segunda dirección compone tres acciones
      sin perder ninguna, de modo que el `FlowRow` no se queda corto. Segundo, en
      la misma compilación y el mismo emulador, la cuenta de doble rol sí la
      ofrece. Lo cubre además
      `a patient is never offered the professional base action`.

**Requisitos:** RF-03.4, RF-03.5, RF-06.1, RN-02.

**Tareas técnicas.** Migración `professional_base_address`:
`addresses.is_professional_base` con índice único parcial y disparador de
desmarcado, espejo de los de `is_primary` · relleno que marca como base la
dirección principal de quien tiene el rol profesional · `professional_covers()` y
`search_nearby_professionals()` pasan a usar la columna nueva · `AddressListScreen`
ofrece la marca solo a quien tiene el rol profesional.

**El relleno preserva el comportamiento actual.** Sin él, todo profesional ya
aprobado desaparecería de las búsquedas en el momento de aplicar la migración,
porque la columna nueva nacería vacía y la unión que lo encuentra es interna.

**Esto cambia el enunciado del cuarto criterio de HU-06.** «Un profesional sin
dirección principal no aparece en búsquedas» pasa a ser «sin base profesional». Lo
sigue garantizando el mismo `join` interno de `search_nearby_professionals`, sin
código nuevo, igual que cuando se verificó en el Sprint 3. RN-02 se reescribe en
`docs/requirements.md` por el mismo motivo.

## Verificación pendiente del autor: ninguna

El último paso que quedaba —el experimento SQL de «agregar dos veces el mismo
rol»— se corrió el 2026-10-04 y su resultado está en el criterio de HU-34.

## Qué cambia de lo ya cerrado

Este sprint toca evidencia entregada, así que conviene tenerlo enumerado en un
solo lugar en vez de repartido por las historias.

| Qué | Cómo queda |
|---|---|
| HU-02, «Elegir mi rol» | **Sigue siendo válida.** Su pantalla, sus cinco criterios y su verificación en emulador se conservan. Lo que cambia es la lectura del segundo criterio: «no se me vuelve a preguntar» pasa a significar que no se repite la pregunta inicial, no que el rol sea inmutable |
| HU-06, criterio 4 | Cambia de «dirección principal» a «base profesional». El mecanismo que lo garantiza es el mismo |
| RF-01.4 | Se reescribe: la elección inicial sigue siendo obligatoria, pero deja de ser excluyente y definitiva |
| RN-02 | Se reescribe: la búsqueda considera la base profesional, no la dirección principal |
| `assignsRoleOnlyOnceAndRejectsSecondAssignment` | **Prueba obligatoria contradicha de frente.** Se reemplaza por `rejectsAddingARoleThePersonAlreadyHolds` en `.claude/rules/testing.md`. No se apaga: se corrige, porque la regla que expresaba dejó de ser la regla |
| Decisión del 2026-09-11 sobre la reputación | Superada en parte por HT-16 |
| Decisiones del 2026-09-13 y 2026-09-14 sobre el rol | Tres superadas en parte. Ver `docs/decisions.md` |

## Pruebas obligatorias que agrega este sprint

Se suman a `.claude/rules/testing.md`, sección «Autenticación y perfiles».

| Prueba | El error real que atrapa |
|---|---|
| `activeRoleMustBeOneOfTheHeldRoles` | Un rol activo que la persona no tiene, que dejaría la aplicación en un modo sin datos detrás |
| `rejectsAddingARoleThePersonAlreadyHolds` | Reemplaza a `assignsRoleOnlyOnceAndRejectsSecondAssignment` |
| `rejectsSwitchingToARoleThePersonDoesNotHold` | Un switch que activa un rol inexistente |
| `switchFailureKeepsThePreviousActiveRole` | La actualización optimista exacta que `AddressListViewModelTest` ya atrapó una vez en otra pantalla |
| `aPersonWithBothRolesEditingAsPatientWritesNoProfessionalData` | La más importante del sprint: el formulario escribiendo el lado equivocado de una persona que tiene los dos |
| `theSwitchIsHiddenForSomeoneWithASingleRole` | Un control que ofrece cambiar a nada |
| `adminRoleIsNeverSelfAssignable` | Se conserva intacta |

## Incremento del sprint

Una persona con una sola cuenta de Google pide atención como paciente, cambia de
rol con un control en «Mi cuenta», y queda registrada como profesional con su
propia base de cobertura. **Enunciado con cuidado para no repetir el error del
Sprint 2:** no promete que las búsquedas la encuentren, porque eso sigue
dependiendo de la aprobación del administrador, que llega con HU-09.

## Retrospectiva

**El incremento se demostró entero, y esta vez el enunciado no prometía de más.**
Una persona con una sola cuenta de Google pide atención como paciente, cambia de
rol con el control de «Mi cuenta» y queda registrada como profesional con su base
de cobertura propia. Todo eso se recorrió en el emulador. El enunciado se había
escrito con cuidado de no repetir el error del Sprint 2 —no promete que las
búsquedas la encuentren, porque eso depende de HU-09— y la precaución se pagó
sola: se pudo dar el incremento por demostrado sin asteriscos.

### Velocidad medida: 23 puntos, los 23 planificados

HU-34 (8) + HT-16 (5) + HU-35 (5) + HU-36 (5) = 23. Las cuatro historias cumplen
la Definición de Terminado y ninguna se trasladó.

> El Sprint 2.5 se ejecutó del **01/10/2026** al **04/10/2026**.

**Cuatro días, y la tercera medición seguida que no mide capacidad.** Igual que los
dos anteriores, este sprint terminó cuando se agotó el alcance y no cuando se
agotó el plazo. La retrospectiva del Sprint 2 ya había dejado la pregunta
planteada y ofrecía dos caminos para el Sprint 3; la interrupción la dejó sin
responder. **Sigue sin responderse, y ahora con un sprint más de evidencia de que
el 21 no es una velocidad sino una cifra de planificación repetida.** El Sprint 3
tiene 24 puntos y es la oportunidad que queda de cerrarlo por fecha de corte y
obtener por fin el dato.

**Un matiz que este sprint sí aporta:** 23 puntos no planificados entraron y
salieron en cuatro días, con cinco migraciones aplicadas y treinta y cinco pruebas
nuevas. Eso no es una velocidad —el trabajo era de una característica ya conocida
y sin pantallas nuevas de peso—, pero sí descarta que la estimación esté
sistemáticamente corta.

### Qué funcionó

- **Medir el costo antes de decidir, en lugar de estimarlo.** La reacción natural
  al cambio de modelo fue «hay que reescribir las políticas de las quince tablas».
  Leer el esquema efectivo mostró que solo **dos** políticas leían `profiles.role`
  de forma directa, más `is_admin()`: todo lo demás resuelve por pertenencia. Esa
  lectura, de minutos, es lo que convirtió un sprint que parecía inasumible en uno
  de tres días. La decisión de interrumpir el Sprint 3 se tomó sobre el número
  medido, no sobre el temido.
- **El experimento que discrimina, no el que confirma.** El defecto de recursión
  de `request_offers_insert_participants` se destapó porque el experimento
  insertaba **dos** ofertas, la ilegítima y la legítima, y las dos fallaron con el
  mismo `SQLSTATE`. Un experimento que solo hubiera comprobado que la ilegítima
  falla habría dado por buena una política rota para toda inserción, que llevaba
  quince días así. **Toda prueba de política debe incluir el caso que sí debe
  pasar**, porque es el único que distingue «deniega bien» de «no funciona».
- **Ordenar por reversibilidad**, por tercer sprint consecutivo. El criterio de
  HU-35 que exige ver «Mi cuenta» sin control segmentado se verificó antes de
  activar el segundo rol, porque `profile_roles` es de solo agregar y activarlo lo
  vuelve inalcanzable para siempre.
- **Contrastar cada paso contra la base y no contra la pantalla.** Es lo que hizo
  comprobable que el relleno de las migraciones salió exacto, y lo que permitió
  afirmar que la verificación no dejó residuo.

### Qué no funcionó

- **«No verificable» se escribió cuatro veces sin comprobar que lo fuera.** Los
  cuatro criterios que el 2026-10-02 quedaron fuera por imposibles se cerraron
  entre el 2026-10-03 y el 2026-10-04 sin escribir una línea de código: la caída
  de red la provoca `adb`, la cuenta «nueva» ya estaba en el emulador desde antes,
  el paciente puro salía de esa misma cuenta, y el rol repetido lo demuestra
  `supabase db query --linked` en dos comandos. Los cuatro motivos se dedujeron
  del código y de los datos, que era el sitio equivocado donde mirar: eran
  afirmaciones sobre el **entorno**. La regla que queda: **antes de anotar un
  criterio como no demostrable, intentarlo una vez.** Un `dumpsys account`, un
  `cmd connectivity airplane-mode enable` y un `--help` leído entero habrían
  ahorrado el párrafo completo.
- **Una subconsulta dentro de una política obedece a las políticas de la tabla que
  consulta**, y eso costó un defecto propio, introducido en este mismo sprint:
  `reviews_select_visible` dejó de mostrar **ninguna** reseña a un tercero, que es
  justo el público de una reseña pública. La corrección de una fuga cerró la
  puerta entera. Está registrado en `docs/decisions.md` del 2026-10-02, con la
  lista de políticas vigentes que consultan otra tabla y deben revisarse con este
  criterio al llegar a su sprint.
- **La deuda de `androidTest` cobró dos veces y casi una tercera.** Los dos
  defectos de interfaz del 2026-10-02 —las acciones que desaparecían del `Row` y
  el control segmentado desbordado al 200 %— no los podía atrapar ninguna prueba
  de JUnit. Y al verificar el criterio del paciente puro hubo que demostrar **a
  mano**, con una segunda dirección y con la cuenta de doble rol como contraste,
  que la acción ausente faltaba por rol y no por disposición: exactamente la
  pregunta que una prueba de interfaz respondería sola. La deuda se arrastra desde
  el Sprint 1 y ya tiene tres ejemplos concretos de lo que deja pasar.
- **Un sprint no planificado interrumpió a otro ya en curso**, y aunque la decisión
  fue correcta y está justificada, el costo real es que HU-06 lleva tres sprints
  abierta. El Sprint 3 se reanuda con ella y no se vuelve a mover.

### Qué cambiar en el Sprint 3

1. **Cerrar el Sprint 3 por fecha de corte**, no por alcance, y contar lo cerrado.
   Es la tercera vez que se plantea y la primera en que no hay excusa: con 24
   puntos planificados contra 21 de referencia, es la medición que el proyecto
   lleva tres sprints sin obtener.
2. **HU-06 primero y hasta cerrarla.** Está escrita, probada y le falta solo la
   verificación en dispositivo, que ahora sí tiene emulador. Su cuarto criterio
   cambió de «dirección principal» a «base profesional» por HU-36.
3. **Resolver el hallazgo de las direcciones sin principal** dentro de HU-06, que
   es su historia dueña. Eliminar la dirección principal deja a la persona sin
   ninguna, porque `addresses_first_is_primary` solo actúa al insertar. Importa
   más desde este sprint, porque `is_professional_base` tiene la misma forma:
   eliminar tu base te saca de las búsquedas en silencio.
4. **Revisar con el criterio del 2026-10-02 las políticas de HU-07 y HU-09 antes
   de escribirlas.** Las de `verification_documents` van a necesitar mirar
   `profiles` y `profile_roles`, que es justo la forma que produjo los dos
   defectos de este sprint.
5. **Usar `ledezma.aramayo.73@gmail.com` como la cuenta de un solo rol** y no
   activarle el profesional. Es la única con la que se podrá demostrar lo que un
   doble rol ya no distingue.
6. **Verificar las pruebas de política con `supabase db query --linked`**, que es
   lo que destrabó el último criterio de este sprint y el camino que
   `.claude/rules/testing.md` ahora recoge. Los Sprints 3 a 9 agregan políticas en
   casi todas sus historias, así que la diferencia se acumula.

---

# Sprint 3 — Verificación de usuarios

**Estado:** `[x]` cerrado el 2026-10-06. Las cuatro historias terminadas: HU-06,
HU-07, HU-08 y HU-09. Suspendido el 2026-10-01 con HU-06 en `[~]` a falta de
su verificación en dispositivo, y **reanudado el 2026-10-04** al cerrar el
Sprint 2.5. **HU-06 cerrada el 2026-10-04**, con tres sprints de retraso: su
verificación se recorrió en el emulador el 2026-10-03, destapó un cuarto defecto
de integridad, y la migración que lo corrige quedó aplicada y ejercitada al día
siguiente. **HU-07 cerrada el 2026-10-05.** **HU-08 cerrada el 2026-10-05.**
**HU-09 cerrada el 2026-10-06.** Los 24 puntos están entregados.

> **La deuda que este sprint arrastraba quedó cerrada el 2026-10-08**, con HT-18.
>
> Era anterior a HU-06 y llegó a tener cinco ejemplos: **`app/src/androidTest` estaba
> vacío** aunque sus dependencias llevaban declaradas desde el Sprint 0. Las pruebas
> de interfaz existen ahora, pero **en la JVM y no en `androidTest`**, porque la
> integración continua no levanta ningún emulador. Cubren dos de las tres formas que
> tomaron esos defectos; la tercera, el texto recortado dentro de su contenedor, sigue
> dependiendo de mirar las previsualizaciones. Ver HT-18 y `docs/decisions.md`, 2026-10-08.

> **La siguiente es HU-07.** Antes de escribir sus políticas conviene aplicarles
> el criterio del 2026-10-02: las de `verification_documents` van a necesitar
> mirar `profiles` y `profile_roles`, que es exactamente la forma que produjo los
> dos defectos del Sprint 2.5. Una subconsulta directa dentro de la política
> obedece a las políticas de la tabla que consulta; la salida es una función
> `security definer` que devuelva un booleano.

> **Por qué se suspende.** La reunión del 2026-10-01 con la contraparte del
> negocio cambió el modelo de rol, y las tres historias que quedan en este sprint
> lo presuponen: HU-07 pide un conjunto de documentos al paciente y otro al
> profesional, y con el rol múltiple ese conjunto pasa a ser la unión de los roles
> que la persona tenga, no una rama excluyente. Seguir aquí significaría escribir
> dos veces la misma historia. El Sprint 2.5 resuelve el modelo primero.
>
> HU-06 no se traslada otra vez. Está escrita, probada y con un criterio verificado
> por el autor; lo que le falta es verificación en dispositivo, no código. Moverla
> por tercera vez la convertiría en la historia que nunca se cierra.

**Objetivo del sprint.** Ambos roles someten sus documentos a verificación y el
administrador los aprueba o rechaza.

**Objetivo específico.** 2 · Desarrollar la gestión de perfiles de usuarios.

**Puntos:** 24.

> **HU-06 llega del Sprint 2** y se trabaja primero, como corresponde a una
> historia ya comprometida. Encaja con el objetivo mejor de lo que su origen
> sugiere: su último criterio —un profesional sin dirección principal no aparece
> en búsquedas— es la misma condición de visibilidad que este sprint cierra por
> el lado de la verificación.

### HU-06 · Administrar mis direcciones `[x]` — 3 puntos

> Como **usuario**, quiero **tener varias direcciones y marcar una como
> principal**, para **solicitar atención en distintos lugares**.

**Criterios de aceptación**

- [x] Dado que tengo varias direcciones, cuando abro la lista, entonces las veo
      con su alias y su referencia. **Verificado el 2026-10-03** con
      `ledezma.aramayo.73@gmail.com`: «Casa» con «Porton verde frente al estadio»
      y «Trabajo» con «Oficina 3, segundo piso», los dos alias y las dos
      referencias a la vista.
- [x] Dado que marco una como principal, cuando marco otra, entonces la anterior
      deja de serlo automáticamente. **El autor probó esto en su dispositivo,
      encontró que no funcionaba, se corrigió — ver «Defecto encontrado por el
      autor probando la historia» — y el autor confirmó que ahora sí cambia la
      dirección principal.** **Reconfirmado el 2026-10-03:** marcar «Trabajo»
      dejó a «Casa» sin la insignia, y la lista se redibujó desde el servidor sin
      recargar la pantalla a mano.
- [x] Dado que elimino una dirección, cuando confirmo, entonces desaparece de la
      lista. **Verificado el 2026-10-03**, con las dos salidas del diálogo: al
      pulsar «Cancelar» la dirección sigue en la lista, y al confirmar
      desaparece.
- [x] Dado que soy profesional, cuando no tengo **base profesional declarada**,
      entonces no aparezco en búsquedas. **Reescrito por HU-36 el 2026-10-02:**
      antes decía «dirección principal», y la búsqueda dejó de mirar esa columna.
      El mecanismo que lo garantiza no cambia —sigue siendo la unión interna de
      `search_nearby_professionals` con `addresses`, ahora sobre
      `is_professional_base`—, de modo que sigue sin agregar código: ver «Por qué
      el cuarto criterio no agrega código». **Verificado el 2026-10-02** con el
      experimento SQL de HU-36, cuyo cuarto criterio enuncia exactamente esta
      misma garantía sobre la misma unión interna.

**Requisitos:** RF-03.4, RF-03.5, RN-02.

**Tareas técnicas.** Extender `IAddressRepository` con listar, marcar principal
y eliminar · casos de uso `GetMyAddressesUseCase`, `SetPrimaryAddressUseCase`,
`DeleteAddressUseCase`, y `GetAddressUseCase` en lugar del antiguo
`GetMyAddressUseCase` · pantalla de lista con confirmación de eliminación ·
`AddressScreen` acepta un identificador opcional para editar cualquier
dirección, no solo la principal.

**Las cuatro tareas están escritas y probadas; falta la verificación manual.**
HU-05 dejó anotado como deuda que la pantalla de dirección «trabaja siempre
sobre la dirección principal»; esa es exactamente la deuda que esta historia
salda.

| Capa | Qué se agregó |
|---|---|
| `domain/model/` | `AddressListResult`, `SetPrimaryAddressResult`, `DeleteAddressResult` |
| `domain/usecase/` | `GetAddressUseCase` (reemplaza a `GetMyAddressUseCase`), `GetMyAddressesUseCase`, `SetPrimaryAddressUseCase`, `DeleteAddressUseCase` |
| `data/datasource/` | `findAllAddresses()` reemplaza a `findPrimaryAddress()`; `setPrimary()` y `deleteAddress()` nuevos |
| `data/repository/` | `AddressRepository` implementa los tres métodos nuevos de la interfaz |
| `presentation/` | `AddressListScreen`, `AddressListViewModel`; `AddressScreen` y `AddressViewModel` reciben un `addressId` opcional |
| `navigation/` | `AddressListRoute` nueva; `AddressRoute` pasa de objeto a clase con `addressId: String? = null` |

**Una migración nueva, agregada el 2026-10-03:**
`20261003120000_preserve_marked_addresses_on_delete`, con los dos disparadores de
eliminación y el relleno. Hasta esa fecha la historia no necesitaba ninguna: las
cuatro políticas de `addresses`
—`addresses_select_own`, `addresses_insert_own`, `addresses_update_own`,
`addresses_delete_own`— y el disparador `addresses_unmark_previous_primary`
existen desde HT-04 y ya cubren listar, marcar como principal y eliminar. Esta
historia es la primera en consumir `addresses_delete_own` y en escribir
`is_primary` por sí misma desde el cliente en lugar de dejar que el disparador
`addresses_first_is_primary` la ponga.

**Por qué marcar como principal vuelve a leer la lista en vez de calcular el
resultado en el cliente.** Quién deja de ser principal lo decide
`addresses_unmark_previous_primary`, un disparador de la base, no una regla que
el cliente pueda repetir sin arriesgarse a que las dos copias diverjan. Después
de un `PATCH` exitoso, `AddressListViewModel` vuelve a pedir la lista completa
en vez de suponer cuál fila cambió; es el mismo principio que ya regía en
`AddressRepository.saveAddress`, que redibuja desde la fila que devolvió el
servidor y no desde el texto que se escribió.

**Por qué el cuarto criterio no agrega código.** `search_nearby_professionals`
—la función de HT-04, todavía sin consumidor porque HU-11 llega en el
Sprint 4— une `professionals` con `addresses` mediante
`join public.addresses a on a.profile_id = pro.id and a.is_primary`. Es una
unión interna: un profesional sin ninguna fila con `is_primary = true`
simplemente no genera fila de resultado, sin necesidad de un `where` que lo
excluya. El criterio ya estaba satisfecho por el esquema desde que HT-04 se
aplicó; esta historia lo hereda en lugar de implementarlo. Verificado leyendo
la migración `20260911120600_functions.sql` (líneas 88-107), no con un
experimento nuevo contra la base remota: las consultas de solo lectura contra
el proyecto remoto quedaron bloqueadas por el clasificador de modo automático
de esta sesión a mitad de la verificación —ver «Deuda reconocida»—, después de
confirmar sin problema que `addresses` seguía en cero filas tras el Sprint 2.

**Qué atrapan las pruebas nuevas.** Ocho pruebas nuevas en
`AddressListViewModelTest`, que llevan la suite de 186 a 194:

| Prueba | El error real que atrapa |
|---|---|
| `no registered addresses shows the empty state` | Una pantalla en blanco la primera vez que alguien la abre, sin invitación a agregar una dirección |
| `the list carries the alias and the reference of every address` | Que la pantalla muestre algo distinto de lo que el repositorio devolvió |
| `marking an address as primary reloads the list instead of guessing the result` | Una insignia de «Principal» que se queda en la fila vieja porque el cliente calculó el cambio en lugar de leerlo de vuelta |
| `a refused primary change reports the error and keeps the previous list` | Una actualización optimista que le hace creer a un profesional que cambió su dirección principal cuando el servidor nunca lo aceptó — el hueco exacto del que depende RN-02 |
| `deleting an address asks for confirmation before touching the repository` | Un toque de más que borra una dirección sin paso atrás |
| `dismissing the delete confirmation leaves the address on the list` | Un diálogo que cancela en la pantalla pero borra igual en el servidor |
| `confirming the deletion removes the address once the server confirms it` | Una fila que desaparece de la lista antes de que el servidor confirme que la eliminó |
| `a refused deletion reports the error and keeps the address on the list` | Una eliminación fallida que igual desaparece de la lista, dejando a la persona sin saber que la dirección sigue existiendo |

**Trece pruebas más el 2026-10-03**, que llevan la suite de 223 a 236. Diez en
`DeleteAddressUseCaseTest`, nueva, y tres en `AddressListViewModelTest`:

| Prueba | El error real que atrapa |
|---|---|
| `deleting the primary address with a single survivor lets the database promote it` | Que el cliente repita la regla del disparador y las dos copias diverjan |
| `deleting the primary address with two survivors asks who inherits before writing` | El defecto exacto del 2026-10-03: la persona se queda con direcciones y ninguna principal |
| `the chosen successor takes the primary mark before the address is deleted` | Un traspaso que ocurre después de eliminar, dejando una ventana sin ninguna marcada |
| `an address that is both primary and base hands over both marks` | Conservar la principal y perder la base, que es la que decide si apareces en búsquedas |
| `a failed handover stops before deleting, so no mark is lost` | Eliminar igual cuando el traspaso no llegó a escribirse |
| `a successor that is not one of my addresses is refused` | Un identificador ajeno colándose como heredero |
| `deleting the professional base with two survivors asks who inherits it` | Que la regla valga solo para la principal y la base siga cayendo en silencio |
| `a list that cannot be read reports the error instead of deleting blind` | Eliminar sin saber qué marcas llevaba la fila |
| `dismissing the successor choice cancels the deletion and frees the screen` | Una fila que queda en estado pendiente para siempre y congela la pantalla |

La suite de `AddressViewModelTest` se actualizó para el nuevo parámetro
`addressId`, sin perder ninguna de sus catorce pruebas: por omisión es `null`
—la pantalla en blanco para registrar una dirección nueva—, y las dos pruebas
sobre una dirección ya guardada ahora lo pasan de forma explícita.

**Defecto encontrado por el autor probando la historia: marcar como principal
no escribía nada.** El autor reportó que el botón no funcionaba ni desde la
aplicación ni comprobándolo por SQL, y pidió reproducirlo y llegar a la raíz
antes de corregir.

*Reproducción.* Con el CLI de Supabase ya vinculado al proyecto remoto
(`npx supabase db query --linked`), se leyeron los disparadores reales de
`addresses` —los tres estaban bien definidos, en el orden correcto— y se
probó un `update ... set is_primary = true` directo, primero con privilegios
completos y después simulando al usuario autenticado con
`set local role authenticated` y `set local request.jwt.claims`, sobre las
propias direcciones de prueba que el autor ya había creado («trabajo» y
«casa»). Las dos formas funcionaron: el disparador y la política de seguridad
estaban correctos. Todas las pruebas se hicieron dentro de transacciones con
`rollback`, así que las direcciones reales del autor no se tocaron; se
verificó al terminar que seguían exactamente como estaban.

*Causa.* El problema estaba en el cliente, no en la base. `install(Postgrest)`
en `CoreModule` no recibe el `Json { encodeDefaults = true }` que ese mismo
archivo construye para `DataStoreSessionManager`: sin configuración explícita,
el complemento cae al serializador propio de supabase-kt
(`KotlinXSerializer(Json { ignoreUnknownKeys = true })`), que deja
`encodeDefaults` en su valor de la biblioteca, `false`. `PrimaryAddressRow`
declaraba `isPrimary: Boolean = true`; como el valor coincidía con su valor
por omisión, kotlinx.serialization lo omitía del cuerpo de la petición. El
`PATCH` que la aplicación mandaba era `{}`: ninguna columna cambiaba, y el
disparador `addresses_unmark_previous_primary` —que solo se dispara en
`update of is_primary`— nunca llegaba a evaluarse.

Se confirmó con una prueba JVM desechable, antes de corregir nada: codificar
`PrimaryAddressRow(isPrimary = true)` con el mismo `Json` que usa producción
producía `{}`, no `{"is_primary":true}`. La prueba falló como se esperaba y
se retiró una vez aplicada la corrección, porque la clase que probaba dejó de
existir.

*Corrección.* `SupabaseAddressDataSource.setPrimary` dejó de mandar un objeto
serializable y pasó a construir el cuerpo con el operador `update({ set(...) })`
del SDK, que escribe directo a un mapa de `JsonElement` y nunca pasa por
`encodeDefaults`. Es el mismo patrón que `SupabaseProfileDataSource
.setAvailableNow` ya usaba desde HU-04, verificado entonces contra la base
remota. `PrimaryAddressRow` se eliminó: sin ese patrón, no hacía falta.

*Consecuencia para el resto del proyecto.* Ninguna otra escritura actual
depende de un valor por omisión que coincida con el que kotlinx.serialization
podría omitir —`AddressRow.reference` y los campos de `SaveProfileParams` no
tienen ese problema porque, o no declaran valor por omisión, o se usan como
argumentos de una función remota con su propio valor por omisión en SQL, no
como columnas de una fila—, pero cualquier característica futura que escriba
una fila parcial desde un objeto serializable debe evitar declarar un valor
por omisión en la propiedad que va a cambiar, o preferir directamente
`update({ set(...) })`. Registrado en `docs/decisions.md`, 2026-09-16.

**Segundo ajuste pedido por el autor: guardar una dirección vuelve a la
lista.** Antes, `AddressScreen` se quedaba en el formulario tras guardar,
mostrando «Dirección guardada.» El autor pidió que el flujo fuera «se guarda
la ubicación y se dirige a la pantalla de direcciones». `AddressScreen` ahora
recibe `onSaved: () -> Unit` y lo dispara con un `LaunchedEffect(uiState)` en
cuanto el estado llega a `SaveStatus.Saved` —el mismo patrón que
`WelcomeScreen.onSignedIn` y `RoleSelectionScreen.onRoleAssigned` ya usan para
navegar tras un resultado asíncrono—.

**Tercer defecto encontrado por el autor probando el ajuste anterior: la lista
volvía sin la dirección recién guardada.** El primer intento conectó
`onSaved` con `navController.popBackStack()`. El autor probó guardar una
tercera dirección con dos ya registradas y, al volver, la lista seguía
mostrando solo las dos anteriores; señaló que lo mismo debía pasar al editar.

*Causa.* `popBackStack()` vuelve a la **misma** entrada de `AddressListRoute`
que ya estaba en la pila desde antes de abrir el formulario, y Navigation
Compose conserva el `AddressListViewModel` de esa entrada mientras no se
destruye. Ese modelo de vista solo carga la lista una vez, en su `init`; nada
lo avisa de que hay una dirección nueva que leer, así que sigue mostrando la
foto de cuando se abrió.

*Corrección.* `onSaved` pasó a `navController.navigate(AddressListRoute) {
popUpTo(AddressListRoute) { inclusive = true } }`. Esto saca de la pila la
entrada vieja de `AddressListRoute` —con su modelo de vista y su lista
obsoleta— y empuja una entrada nueva, cuyo `AddressListViewModel` recién
creado carga la lista completa por primera vez, ya con la dirección que se
acaba de guardar. Vale tanto para agregar como para editar, porque las dos
pasan por el mismo `onSaved`.

**Deuda reconocida el 2026-09-16, saldada el 2026-10-03.** La sesión que escribió
esta historia no pudo verificarla: el entorno no tenía `adb` ni emulador, y las
consultas de solo lectura contra el proyecto remoto quedaron bloqueadas por el
clasificador de modo automático a mitad de una comprobación (motivo: «Production
Reads»). Por eso los cuatro criterios quedaron sin marcar y la historia en `[~]`.
La verificación se recorrió el 2026-10-03 sobre el emulador `Pixel_9_Pro`, y está
más abajo.

**Cuarto defecto, reproducido el 2026-10-03: eliminar la dirección principal no
promueve ninguna otra.** El hallazgo venía anotado desde el Sprint 2.5 como
sospecha —las tres direcciones del autor estaban sin marcar como principal, sin
causa conocida— y esta historia es su dueña, porque es la que elimina direcciones.

*Reproducción*, con `ledezma.aramayo.73@gmail.com` en el emulador. Con dos
direcciones, «Trabajo» principal y «Consultorio» no, se eliminó «Trabajo».
«Consultorio» quedó como única dirección de la persona y **sin** la insignia
«Principal»: la fila sigue ofreciendo «Marcar como principal». La persona queda
con direcciones y sin ninguna principal, que es el estado exacto en el que
estaban las del autor.

*Causa.* `addresses_first_is_primary` se dispara **al insertar**, y solo marca la
primera fila de cada persona. Ningún disparador actúa al eliminar, de modo que
borrar la principal no promueve a nadie. No hay nada que reparar el estado
después.

*Por qué importa más desde el Sprint 2.5.* `is_professional_base` tiene
exactamente la misma forma, con su propio disparador de desmarcado al actualizar y
ninguno al eliminar. Las consecuencias no son iguales: quedarse sin dirección
principal es visible —la lista no muestra la insignia—, mientras que quedarse sin
base profesional **saca al profesional de los resultados de búsqueda en silencio**,
porque la unión de `search_nearby_professionals` con `addresses` es interna. El
profesional no recibe solicitudes y nada en la aplicación le dice por qué.

*Corrección, decidida con el autor el 2026-10-03.* Las dos formas que se
plantearon —promover siempre, u obligar siempre a elegir— se combinan según
cuántas direcciones sobrevivan, que es lo que decide si hay algo que elegir:

- **Queda exactamente una:** la hereda, y lo hace el motor. Preguntar algo cuya
  respuesta es única es un paso de más.
- **Quedan dos o más:** la persona elige cuál ocupa el lugar, antes de eliminar.

`20261003120000_preserve_marked_addresses_on_delete` lo implementa con dos
disparadores, `addresses_promote_last_address` (`after delete`) y
`addresses_guard_marked_delete` (`before delete`, que levanta
`address_needs_successor`). Del lado del cliente, `DeleteAddressUseCase` lee la
lista y devuelve `SuccessorRequired` sin escribir nada cuando hay que elegir; la
pantalla pregunta y, con la respuesta, traslada las marcas y recién entonces
elimina.

**Por qué no se promueve automáticamente la base profesional.** La dirección
principal es dónde te atienden y cualquiera de las tuyas es candidata; la base
profesional es desde dónde cubres tu zona, y elegirla por ti te pone en búsquedas
centradas en una dirección que nunca declaraste para eso. Es un defecto peor que
el que se corrige, porque llega hasta el paciente. Razonado en
`docs/decisions.md`, 2026-10-03.

**El relleno corrige las filas ya afectadas**, pero solo `is_primary`: marca la
dirección más antigua de quien tenga direcciones y ninguna principal, que es la
que `addresses_first_is_primary` habría marcado. Para la base no hay equivalente,
por la misma razón de arriba.

**Un caso que la corrección tuvo que contemplar.** `addresses.profile_id`
referencia a `profiles` con `on delete cascade`, y la acción referencial corre
después de que la fila padre desaparece. Sin cuidado, eliminar una cuenta con tres
direcciones abortaría al llegar a la principal. Los dos disparadores comprueban
que el perfil siga existiendo, y esa ausencia es lo que distingue una cascada de
una eliminación normal. RF-01.7 pide esa eliminación de cuenta.

*Migración aplicada y verificada el 2026-10-04*, con `supabase db push --linked`.
La comprobación fue en cinco partes, las tres últimas dentro de transacciones con
`rollback`:

| Qué se comprobó | Resultado |
|---|---|
| La migración quedó registrada, con sus dos disparadores y sus dos funciones | `1, 2, 2` |
| Nadie conserva direcciones sin una principal | **Cero personas.** Es el invariante que el defecto rompía |
| El relleno sobre los datos reales | Las dos direcciones del autor tienen principal otra vez. **Ninguna base profesional se inventó:** la única marcada sigue siendo la que él ya había declarado |
| `addresses_promote_last_address`, con un superviviente | La dirección que se insertó **sin** la marca quedó con `is_primary = true` tras eliminarse la principal |
| `addresses_guard_marked_delete`, con dos supervivientes | **`P0001: address_needs_successor`**, línea 23 de la función |
| El borrado en cascada de un perfil con tres direcciones | **Pasa sin error**, y deja cero perfiles y cero direcciones. Es el caso que la corrección tuvo que contemplar para no romper RF-01.7 |
| Residuo | **Ninguno.** Al terminar, las cuatro direcciones reales están idénticas y el perfil de prueba sigue en su sitio |

**Que las dos últimas discriminen es lo que vale.** Con un superviviente promueve,
con dos exige elegir, y con el perfil en vías de desaparecer no hace ninguna de las
dos. Un experimento que solo hubiera comprobado que el guardia levanta la excepción
habría dado por buena una corrección que rompe la eliminación de cuentas.

**Una cuenta que el plan no tenía anotada.** La consulta destapó una tercera,
`christian.ledezma@ucb.edu.bo`, con una dirección «Trabajo» y sin base
profesional. No altera nada de lo verificado —su dirección también quedó con
principal— pero conviene tenerla registrada junto a las otras dos al preparar los
datos de prueba del Sprint 3.

**Un efecto del relleno que conviene saber.** La principal del autor quedó en
«trabajo» y no en «casa», porque el relleno marca la más antigua, que es la que
`addresses_first_is_primary` habría marcado. No tenía forma de saber cuál es el
domicilio. Se corrige desde la aplicación con «Marcar como principal», y entonces
«trabajo» conserva la base profesional: es justo la separación que HU-36 permite.

**Verificación en dispositivo, recorrida el 2026-10-03.** Los cuatro criterios
quedan marcados. Se usó `ledezma.aramayo.73@gmail.com` y no la cuenta del autor,
para no mutar sus datos reales: la historia dice «Como usuario», sin exigir rol, y
esa cuenta es de prueba.

| Paso | Resultado |
|---|---|
| Estado vacío | «Todavía no registraste ninguna dirección» con «Agregar dirección». Es lo primero que ve alguien nuevo |
| Registrar dos direcciones | Cada una vuelve sola a la lista al guardar y aparece de inmediato. La primera se marcó principal sola |
| Editar «Trabajo» | El formulario abrió con sus datos cargados —la deuda de HU-05, que trabajaba siempre sobre la principal, queda saldada— y el cambio se vio al volver, sin salir y entrar |
| **Criterio 1** | Alias y referencia visibles en las dos filas |
| **Criterio 2** | Marcar «Trabajo» dejó a «Casa» sin la insignia, sin recargar a mano |
| Cancelar una eliminación | La dirección sigue en la lista |
| **Criterio 3** | Al confirmar, desaparece |
| **Criterio 4** | Verificado el 2026-10-02 por el experimento SQL de HU-36, que enuncia la misma garantía sobre la misma unión interna |

**La historia se cierra el 2026-10-04 por decisión del autor, con una deuda
declarada.** Los cuatro criterios de aceptación están verificados y la migración
aplicada. Lo que **no** se cumplió son los puntos 9, 10 y 11 de la Definición de
Terminado sobre una pantalla nueva: el diálogo que pide cuál dirección hereda las
marcas no se demostró en un dispositivo, ni en esquema oscuro, ni con el tamaño de
fuente al 200 %.

**Por qué no se demostró.** El emulador `Pixel_9_Pro` falló cuatro veces seguidas
el 2026-10-04: dos caídas con SIGSEGV, un cuelgue del hilo principal de QEMU que
nunca llegó a conectarse por `adb`, y una cuarta caída al reintentar con
renderizado por software y arranque en frío. Cada caída se llevó la cuenta de
Google del emulador, porque su instantánea guardada es del 2 de octubre y se
restaura en cada arranque. El escenario llegó a quedar montado —tres direcciones
de `salud.en.casa.73@gmail.com`, «Casa» principal más «Trabajo» y
«Consultorio»— y el emulador se cayó en el toque siguiente, que era el que abre el
diálogo. **Esas tres direcciones siguen en la base**, así que retomarlo es un
toque, no un montaje.

**Qué respalda al diálogo mientras tanto, y qué no.** Lo cubren las pruebas
unitarias del caso de uso y del modelo de vista, tres revisiones en paralelo que
encontraron y corrigieron el recorte al 200 %, y previsualizaciones nuevas al
200 % y en oscuro. Lo que ninguna de esas cosas sustituye es ver la pantalla
compuesta en un dispositivo: el Sprint 2.5 encontró dos defectos de disposición
que solo aparecieron ahí. **Queda como deuda explícita, no como criterio
relajado.**

**El autor cerró el punto el 2026-10-04 sin exigir esa demostración**, y la
historia queda terminada con esto escrito. La decisión es defendible por lo que sí
respalda al diálogo —las pruebas, las revisiones y las previsualizaciones de
arriba— y porque el defecto que una demostración habría buscado, el recorte al
200 %, ya se encontró y se corrigió por revisión antes de llegar al dispositivo.
Lo que no cubre ninguna de esas cosas es ver la pantalla compuesta, y por eso
queda dicho aquí en vez de darse por hecho.

**Revisión del diálogo, 2026-10-04.** Antes de llevarlo al dispositivo se revisó
con el agente revisor del complemento `feature-dev` y se contrastó el
comportamiento de `AlertDialog` contra su documentación. Tres hallazgos reales:

| Hallazgo | Qué se hizo |
|---|---|
| **El diálogo recortaba sus candidatas.** `AlertDialog` acota la altura de su ranura de texto y corta lo que no entra, sin indicar que hay más. Con el tamaño de fuente al 200 % la segunda candidata en adelante quedaba inalcanzable, y `findAllAddresses` devuelve hasta cincuenta filas | Pasa a `LazyColumn` con `key` estable, que además es lo que `.claude/rules/compose.md` exige para toda lista. Se agregan previsualizaciones al 200 % y en oscuro |
| **Dos pruebas no podían probar lo que decían.** Afirmar sobre `setPrimaryAttempts` y `deleteAttempts` por separado no distingue el orden, y el orden **es** la regla: un traspaso posterior al borrado deja una ventana sin nada marcado | `FakeAddressRepository` registra un único historial ordenado de escrituras, y las aserciones son sobre esa lista. Se agregan tres casos: base sin principal, fallo del traspaso de base, y rechazo del motor |
| **Comentarios que repetían el razonamiento**, contra la regla de `CLAUDE.md` | Recortados a una línea que cita la entrada de `docs/decisions.md`, o eliminados |

Una sospecha propia resultó infundada y conviene dejarla anotada para no volver a
perseguirla: los `TextButton` **sí** cumplen los 48 dp, porque Material 3 les
aplica `minimumInteractiveComponentSize` y el tema no lo desactiva.

**Y un defecto de concurrencia, que es el mismo que el proyecto ya corrigió una
vez.** El guardia de eliminación no estaba serializado contra
`first_address_is_primary`. Con una sola dirección marcada, eliminarla y crear
otra a la vez dejaba a la persona con la nueva y sin ninguna principal: el
guardia contaba cero supervivientes mientras la inserción todavía veía la vieja.
Es exactamente lo que `20260915132619_serialize_first_address` corrigió por el
lado de la inserción, y vuelve por el del borrado. Corregido en
`20261004090000_serialize_marked_address_delete`, con el mismo bloqueo sobre la
fila del perfil. **Aplicada el 2026-10-04.**

**Esa corrección no se puede verificar con el método habitual**, y conviene
decirlo: las pruebas de disparador del proyecto corren dentro de una transacción
que termina en `rollback`, y una carrera entre dos transacciones no se reproduce
dentro de una sola. Se sostiene por lectura del código y por revisión.

**Segunda ronda de revisión, sobre las correcciones mismas.** Se revisó con tres
agentes en paralelo —convenciones, defectos y el SQL— y encontraron cuatro cosas
que el primer pase no tenía, dos de ellas **introducidas por las correcciones del
primer pase**:

| Hallazgo | Qué se hizo |
|---|---|
| El refresco de candidatas leía el estado **antes** de suspender y lo reescribía después, pisando lo que hubiera cambiado | Se lee después, y `onDeleteClick` ya no acepta un segundo borrado con uno en curso |
| Si otro dispositivo eliminaba esa dirección durante el refresco, la pantalla la buscaba con `first` y se caía al componer | El modelo de vista recarga en vez de abrir el diálogo, y la pantalla usa `firstOrNull` |
| Una prueba prometía un orden en su nombre que sus aserciones no comprobaban | Afirma sobre el historial ordenado |
| Los comentarios de la migración estaban **en español**, contra la regla de idioma | Traducidos. La regla no exceptúa comentarios, y el precedente en español de `serialize_first_address` no la deroga |

Y un ajuste que no era un defecto pero sí una mejora medible: el bloqueo pasa de
`for update` a `for no key update`, que serializa igual y deja de detener las
inserciones con clave foránea contra `profiles` de esa persona.

Dos observaciones preexistentes quedan anotadas sin corregir, porque no las
introduce este cambio y ninguna es alcanzable desde la aplicación: un interbloqueo
posible entre un borrado y un marcado simultáneos sobre la misma persona, y que un
borrado masivo por PostgREST dependa del orden en que se procesen las filas.

**Lo que falta para cerrarla: el diálogo de sucesora, en el dispositivo.** Los dos
caminos, con `ledezma.aramayo.73@gmail.com`:

- Con **dos** direcciones, eliminar la principal. La otra debe quedar «Principal»
  sola, sin preguntar nada: ahí decide `addresses_promote_last_address`.
- Con **tres**, eliminar la principal. Debe aparecer el diálogo que pide cuál
  ocupa su lugar; al elegir una, esa queda «Principal» y la otra desaparece. Al
  pulsar «Cancelar», nada cambia y la pantalla vuelve a responder.

Y sobre el diálogo mismo, lo que la Definición de Terminado pide y los
disparadores no cubren: esquema claro y oscuro, y tamaño de fuente al 200 % sin
recortar.

**No recorrido el 2026-10-04**, por las cuatro caídas del emulador descritas
arriba, y cerrado así por decisión del autor.

**Y un hallazgo de método que conviene no repetir.** Un volcado de `uiautomator`
falló en silencio y `adb pull` trajo un archivo de una sesión anterior que seguía
en `/sdcard` con el mismo nombre, de modo que durante unos minutos se leyeron
direcciones de otra cuenta creyendo que eran las nuevas. Se detectó porque las
referencias no coincidían con lo escrito. **Toda verificación por `adb` debe usar
nombres de archivo únicos y comprobar que el volcado tuvo éxito antes de leerlo**,
o puede estar reportando datos viejos sin que nada falle a la vista.

**La deuda de `androidTest` suma su tercer ejemplo.** `app/src/androidTest` está
vacío aunque sus dependencias llevan declaradas desde el Sprint 0, y el recorte
del diálogo es justo lo que una prueba de interfaz al 200 % habría atrapado sin
emulador ni cuenta. Los dos defectos de disposición del Sprint 2.5 eran de la
misma clase. Merece su propia historia técnica; no se abre dentro de HU-06.

### HU-07 · Cargar mis documentos de verificación `[x]` — 8 puntos

> Como **usuario**, quiero **subir mis documentos**, para **acreditar mi identidad
> y generar confianza en la contraparte**.

**Criterios de aceptación**

- [x] Dado que soy paciente, cuando abro la verificación, entonces se me piden
      documento de identidad y fotografía de rostro. **Verificado por
      `VerificationChecklistTest`:** un paciente a solas recibe la lista
      `[ID_FRONT, ID_BACK, SELFIE]`.
- [x] Dado que soy profesional, cuando abro la verificación, entonces se me piden
      además título profesional y matrícula o carnet de estudiante. **Verificado
      por `VerificationChecklistTest`:** un profesional `STUDENT` recibe
      `STUDENT_CARD`; un profesional titulado recibe `LICENSE`; un profesional
      sin tipo declarado recibe ambos como defensa en profundidad. El conjunto
      es la unión de los roles poseídos (criterio del 2026-10-01).
- [x] Dado que capturo una imagen, cuando la envío, entonces se comprime antes de
      subirse. **Compresión implementada en `DocumentImageCompressor`:** JPEG
      calidad 80 con escalera adaptativa hasta 50, lado largo 1600 px, rotación
      EXIF aplicada antes de recomprimir. `DocumentImage` rechaza bytes vacíos
      o mayores a 2 MiB antes de llegar al repositorio
      (`UploadVerificationDocumentUseCaseTest`).
- [x] Dado que subo un documento, cuando otro usuario consulta mi perfil, entonces
      no puede acceder al archivo. **Verificado el 2026-10-05 con el experimento
      SQL `userCannotReadVerificationDocumentsOfAnotherUser` contra el proyecto
      remoto**, tras aplicar `20261005120000_verification_documents_storage.sql`.
      Diez consultas en transacciones con `rollback`:
      | Qué se comprobó | Resultado |
      |---|---|
      | B inserta una fila, A simulado la consulta | **0 filas visibles**. Discrimina: con A simulado consultando sus propias filas, **1 visible** |
      | Insertar con `status = 'APPROVED'` simulando ser el dueño | **`42501: new row violates row-level security policy`**, hueco cerrado por la nueva `insert_own` |
      | Insertar con `status = 'PENDING'` y los campos de revisión nulos | **Pasa**, con status=PENDING, reviewed_by=null, reviewed_at=null |
      | Insertar `OTHER` sin `caption` | **`23514: verification_documents_caption_matches_type`** |
      | Insertar `OTHER` con `caption` | **Pasa**, caption persistido |
      | Insertar un tipo fijo con `caption` | **`23514: verification_documents_caption_matches_type`** (el biconditional discrimina en los dos sentidos) |
      | Insertar una fila con `storage_path` que apunta a la carpeta de otro perfil | **`23514: verification_documents_storage_path_matches_owner`**, confused deputy cerrado |
      | Insertar dos filas (de tipos distintos) con el mismo `storage_path` | **`23505: duplicate key value violates unique constraint "verification_documents_storage_path_unique"`**, review-freeze bypass cerrado |
      | Fila `APPROVED`: `exists` del `update_own` de `storage.objects` cuenta los archivos que el dueño podría sobrescribir | **0**, review-freeze respetado |
      Residuo tras los diez experimentos: **cero filas** en `verification_documents`.
- [x] Dado que la carga falla por conexión, cuando recupero la señal, entonces
      puedo reintentar sin volver a capturar. **Verificado por
      `VerificationViewModelTest`:** un fallo deja los bytes en `stagedBytes`,
      y `onRetry` reutiliza los mismos bytes sin volver a invocar al
      compresor.

**Requisitos:** RF-04.1, RF-04.2, RF-04.3.

**Tareas técnicas.** Contenedor privado con políticas · `IVerificationRepository`
y casos de uso · captura desde cámara y galería · compresión previa · pantalla de
carga con estado por documento.

**Archivos creados o modificados el 2026-10-05.**

| Capa | Archivos |
|---|---|
| Migración | `supabase/migrations/20261005120000_verification_documents_storage.sql` |
| Dominio | `features/verification/domain/model/{DocumentType, ReviewStatus, VerificationDocument, VerificationChecklist, VerificationError, VerificationResult}.kt` · `features/verification/domain/vo/{DocumentCaption, DocumentImage}.kt` · `features/verification/domain/repository/IVerificationRepository.kt` · `features/verification/domain/usecase/{GetMyVerificationChecklistUseCase, UploadVerificationDocumentUseCase}.kt` |
| Datos | `features/verification/data/model/VerificationDocumentDto.kt` · `features/verification/data/mapper/{VerificationDocumentMapper, VerificationErrorMapper}.kt` · `features/verification/data/datasource/SupabaseVerificationDataSource.kt` · `features/verification/data/repository/VerificationRepository.kt` |
| Presentación | `features/verification/presentation/{VerificationViewModel, VerificationScreen, VerificationLabels, DocumentImageCompressor}.kt` |
| Inyección y navegación | `di/VerificationModule.kt` · `SaludEnCasaApplication.kt` · `navigation/Routes.kt` · `navigation/SaludEnCasaNavHost.kt` · `features/auth/presentation/AccountScreen.kt` (nuevo parámetro `onOpenVerification` y botón) |
| Pruebas | `test/features/verification/FakeVerificationRepository.kt` · `test/features/verification/domain/model/VerificationChecklistTest.kt` · `test/features/verification/domain/vo/{DocumentCaptionTest, DocumentImageTest}.kt` · `test/features/verification/domain/usecase/{GetMyVerificationChecklistUseCaseTest, UploadVerificationDocumentUseCaseTest}.kt` · `test/features/verification/data/mapper/VerificationErrorMapperTest.kt` · `test/features/verification/presentation/VerificationViewModelTest.kt` |
| Recursos y manifiesto | `res/values/strings.xml` · `res/values-es/strings.xml` (claves `verification_*`, `error_verification_*`, `auth_account_open_verification`) · `res/xml/file_paths.xml` · `AndroidManifest.xml` (permiso `CAMERA`, `<uses-feature>` opcional y `FileProvider`) |
| Catálogo y compilación | `gradle/libs.versions.toml` (versión `androidxExifInterface` + librería `androidx-exifinterface`) · `app/build.gradle.kts` (dependencia nueva) |
| Reglas y documentos | `.claude/rules/glosario.md` (valor `OTHER`, filas `caption`/`DocumentCaption`) · `docs/decisions.md` (cuatro entradas del 2026-10-05) · `plan.md` (esta sección) |

**Revisión de código, 2026-10-05.** Tres revisores en paralelo
(corrección/bugs, convenciones + CLAUDE.md, simplificación/DRY) produjeron
once correcciones aplicadas en el mismo día, además del hallazgo de
`storage_path` no atado al dueño y la unicidad por `storage_path` que
destapó el revisor automático de seguridad:

1. `DocumentImageCompressor.compress` corre en `Dispatchers.Default`
   (antes bloqueaba el hilo de UI; ANR potencial en gama baja).
2. `VerificationRepository.uploadDocument` escribe primero la fila y
   después el objeto. El orden inverso dejaba objetos huérfanos cuando la
   escritura de la fila fallaba, y las políticas `update_own`/`delete_own`
   de `storage.objects` bloqueaban cualquier reintento sobre ese tipo.
3. `DocumentRow` ahora oculta las acciones de reemplazo también para
   `REJECTED`, no solo `APPROVED`. Permitirlo antes producía un
   «error inesperado» tras pulsar el botón, porque las políticas lo
   deniegan hasta HU-08.
4. `VerificationViewModel.load()` preserva los `stagedBytes` de los demás
   slots tras una subida exitosa; antes los descartaba y rompía el
   criterio 5.
5. `DocumentImageCompressor` reescala con `createScaledBitmap` después de
   `inSampleSize`, para lograr 1600 px reales de lado largo en vez de los
   1000 px que daba el muestreo potencia-de-dos sobre una foto 4000×3000.
6. Se elimina la cadena de URL firmada (`GetSignedDocumentUrlUseCase`,
   `SignedDocumentUrlResult`, el método del repositorio y el del
   datasource). Ningún código la consumía; reaparece cuando HU-08 u HU-09
   la pidan.
7. Se retira `public.has_role` y la cláusula `has_role('PATIENT') or
   has_role('PROFESSIONAL')` de la política de insert sobre
   `storage.objects`. La función existía solo para justificarse (ver
   `docs/decisions.md`, 2026-10-05, «El molde se difiere a su primer
   consumidor real»).
8. `UploadDocumentResult.Success` pasa a `data object`. Se elimina
   `findMyDocument` del datasource, lo que ahorra una consulta por
   documento subido.
9. `VerificationChecklistResult.Loaded` y `VerificationUiState.Content`
   dejan de arrastrar `roles` y `professionalType`; nadie los consumía.
10. Se elimina `VerificationChecklist.optional`: era una constante
    disfrazada de propiedad; la pantalla usa `DocumentType.OTHER`
    directamente.
11. `GetMyVerificationChecklistUseCase` mapea `ProfileError.NetworkUnavailable`
    y `NotSignedIn` a sus equivalentes de `VerificationError`, para que un
    fallo de red leyendo roles o perfil no llegue como «error inesperado».

**Verificaciones del cierre.**

- **Migración aplicada el 2026-10-05** sobre `salud-en-casa` (`sa-east-1`,
  PostgreSQL 17.6) con `npx supabase db push --linked`, previo `--dry-run`.
- **Suite verificada el 2026-10-05**: `./gradlew ktlintCheck`, `./gradlew test`,
  `./gradlew staticAnalysis` y `./gradlew assembleDebug` concluyen sin error.
- **Verificación en dispositivo físico, 2026-10-05**, con la cuenta
  `ledezma.aramayo.73@gmail.com` sobre un `Z2577` (Android, conexión inalámbrica
  por `adb pair`/`connect`). Se recorrió el flujo completo del rol paciente,
  incluido el adjunto opcional y el reintento tras caída de red, y se
  cotejaron las cuatro filas con los objetos reales en Storage:
  | document_type | status | caption | mime | bytes |
  |---|---|---|---|---|
  | `ID_FRONT` | `PENDING` | null | `image/jpeg` | 34 431 |
  | `ID_BACK` | `PENDING` | null | `image/jpeg` | 120 127 |
  | `SELFIE` | `PENDING` | null | `image/jpeg` | 67 180 |
  | `OTHER` | `PENDING` | `otro diploma` | `image/jpeg` | 31 763 |
  Los cuatro archivos pesan muy por debajo del límite de 2 MiB; el `caption`
  solo aparece en `OTHER`, como pide el `check` biconditional; la ruta es
  `<profile_id>/<document_type>.jpg` en todos. El criterio 5 (reintento tras
  pérdida de red) se verificó durante el recorrido.
- **Defecto encontrado y corregido durante la verificación.** La primera
  compilación mostraba el chip de estado «Pendiente de revisión» apilado
  letra por letra junto a un título largo. Se corrigió con dos cambios en
  `VerificationScreen.kt`: la fila del título pasa de `Row(SpaceBetween)` a
  `FlowRow`, de modo que el chip cae a la siguiente línea cuando el título
  no deja espacio, y el texto del chip se acorta a una sola palabra
  («Pendiente», «Aprobado», «Rechazado»). La deuda de `app/src/androidTest`
  suma su cuarto ejemplo: una prueba de interfaz a `fontScale = 2f` lo habría
  atrapado sin dispositivo.

### HU-08 · Conocer el estado de mi verificación `[x]` — 3 puntos

> Como **usuario**, quiero **ver si mis documentos fueron aprobados o rechazados**,
> para **saber si puedo operar y qué debo corregir**.

**Criterios de aceptación**

- [x] Dado que subí mis documentos, cuando abro la verificación, entonces veo el
      estado de cada uno. **Ya lo daba HU-07;** lo cubre
      `VerificationDocumentMapperTest` (el estado llega íntegro) y las
      previsualizaciones de la pantalla.
- [x] Dado que un documento fue rechazado, cuando lo consulto, entonces veo el
      motivo y puedo volver a subirlo. **Verificado** por
      `VerificationDocumentMapperTest.rejectedDocumentExposesReasonToItsOwner` y
      por `VerificationDocumentRowTest`, que fija que el reenvío limpia los
      campos de revisión. La reapertura se comprobó en el proyecto remoto con
      tres experimentos (abajo).
- [x] Dado que mi verificación fue aprobada, cuando la contraparte ve mi perfil,
      entonces aparece el distintivo de verificado. **Alcance acordado el
      2026-10-05:** el distintivo aparece en el perfil propio, y la vista de
      contraparte pasa a HU-11. Verificado por
      `ProfileMapperTest.professionalIsVerifiedOnlyWhenTheStoredStatusIsApproved`.
- [x] Dado que soy profesional sin verificación aprobada, cuando un paciente busca,
      entonces no aparezco en los resultados. **Verificado el 2026-10-05 con
      experimento SQL** contra el proyecto remoto, dentro de transacciones con
      `rollback`: un profesional `PENDING` tiene **0** filas visibles en
      `professional_directory` para otra cuenta, y el mismo perfil `APPROVED`
      tiene **1**. El control discrimina.

**Requisitos:** RF-04.5, RF-04.6, RN-01, INV-07.

**Qué cambió al cerrarla.**

- **Migración `20261005130000_resubmit_documents_and_pending_professional.sql`**,
  aplicada con `db push --linked` tras `--dry-run`. Dos cambios:
  - Un profesional solo se inserta como `PENDING`. **Hueco encontrado en la
    revisión y confirmado en el remoto antes de cerrarlo:** la política de
    inserción no comprobaba el estado, y una cuenta con rol profesional podía
    insertarse `APPROVED` sin revisión. Así entraba a búsquedas sin pasar por el
    administrador, lo que rompe INV-07.
  - El dueño puede reabrir un documento `REJECTED` a `PENDING`, con los campos
    de revisión en nulo. El disparador deja pasar esa única transición.
- **Experimentos SQL sobre el proyecto remoto** (cuenta de prueba
  `ledezma.aramayo.73`, todos dentro de `rollback`, sin residuo verificado al
  terminar):

  | Qué se comprobó | Resultado |
  |---|---|
  | Reabrir un `REJECTED` con los campos de revisión limpios | **Pasa**, queda `PENDING` sin motivo |
  | Aprobarse desde `REJECTED` | **Rechazado por el disparador**, `document_review_is_written_by_an_administrator` |
  | Reabrir dejando el motivo | **Rechazado por la política**, `42501` |
  | Insertar profesional `PENDING` | **Pasa** |
  | Insertar profesional `APPROVED` (antes de la migración **pasaba**) | **Rechazado**, `42501` |
  | Visibilidad en el directorio: `PENDING` / `APPROVED` | **0 / 1** |

- **Cliente.** La fila de subida envía `status = PENDING` y los campos de revisión
  en nulo, para que el upsert reabra la fila. La pantalla oculta «Reemplazar» solo
  para `APPROVED`, y para `REJECTED` muestra «Volver a subir».
- **Perfil propio.** `ProfessionalDetails.isVerified` se deriva de
  `verification_status = 'APPROVED'`, y el encabezado lo muestra solo cuando la
  persona actúa como profesional.

**Deuda declarada.**

- **Vista de contraparte del distintivo**: pasa a HU-11, por decisión del autor.
- **Exposición del estado a la contraparte.** `professionals_select_counterpart`
  expone la fila completa de `professionals`, incluido `PENDING` o `REJECTED`, a
  quien tenga un servicio compartido. Contradice la intención de RF-04.6 para la
  vista de contraparte. Se corrige cuando HU-11 defina qué ve la contraparte.
  Registrado en `docs/decisions.md`, 2026-10-05.
- **Verificación en dispositivo: no se hizo.** Por decisión del autor, HU-08 se
  cierra sin dispositivo. Cubren la historia las pruebas, los experimentos y las
  previsualizaciones en claro, oscuro y al 200 %.

**Verificaciones del cierre, 2026-10-05.** `./gradlew ktlintCheck
testDebugUnitTest staticAnalysis assembleDebug` concluye sin error. Las 49
suites de prueba pasan, y las tres nuevas (`VerificationDocumentRowTest`,
`VerificationDocumentMapperTest` y el caso de `ProfileMapperTest`) se
ejecutaron.

**Archivos creados o modificados el 2026-10-05.**

| Capa | Archivos |
|---|---|
| Migración | `supabase/migrations/20261005130000_resubmit_documents_and_pending_professional.sql` (nueva) |
| Datos | `features/verification/data/model/VerificationDocumentDto.kt` · `features/verification/data/repository/VerificationRepository.kt` · `features/profile/data/model/ProfileDto.kt` · `features/profile/data/mapper/ProfileMapper.kt` |
| Dominio | `features/profile/domain/model/UserProfile.kt` |
| Presentación | `features/verification/presentation/VerificationScreen.kt` · `features/profile/presentation/ProfileViewModel.kt` · `features/profile/presentation/ProfileScreen.kt` |
| Recursos | `res/values/strings.xml` · `res/values-es/strings.xml` (`verification_action_resubmit`, `profile_verified_badge`) |
| Pruebas | `test/features/verification/data/model/VerificationDocumentRowTest.kt` (nueva) · `test/features/verification/data/mapper/VerificationDocumentMapperTest.kt` (nueva) · `test/features/profile/data/mapper/ProfileMapperTest.kt` · `test/features/profile/FakeProfileRepository.kt` |
| Documentos | `docs/decisions.md` (cuatro entradas del 2026-10-05) · `plan.md` (esta sección) |

### HU-09 · Revisar documentos pendientes `[x]` — 10 puntos

> Como **administrador**, quiero **revisar los documentos cargados y aprobarlos o
> rechazarlos**, para **garantizar que solo participen personas acreditadas**.

**Criterios de aceptación**

- [x] Dado que hay documentos pendientes, cuando abro el panel, entonces los veo
      ordenados por antigüedad. **Verificado el 2026-10-06 con experimento SQL**
      contra el proyecto remoto: la consulta de la cola, por `created_at, id`
      ascendente, devuelve primero el documento más antiguo de cinco insertados
      con minutos distintos. El administrador ve los cinco y un tercero ve **cero**.
      La paginación la cubren `GetPendingDocumentReviewsUseCaseTest` (página corta
      contra página llena, el borde exacto) y `DocumentReviewQueueViewModelTest`.
- [x] Dado que abro un documento, cuando lo visualizo, entonces veo la imagen y los
      datos del usuario. **Los datos** salen de `document_review_profiles` (nombre,
      correo, roles, tipo y estado de verificación) y los cubre
      `DocumentReviewViewModelTest`. **La imagen** depende de que la política de
      Storage deje al administrador leer el objeto, y eso se verificó contra los
      **cuatro archivos reales de HU-07**: el administrador lee 4, un tercero lee 0.
      **No se comprobó el dibujo real de la imagen con Coil** (ver «Deuda declarada»).
- [x] Dado que apruebo todos los documentos de un usuario, cuando confirmo,
      entonces su perfil pasa a verificado. **Verificado con experimento SQL:**
      `approve_professional_verification` se rechaza con
      `required_documents_not_approved` mientras falte uno, también con la licencia
      como único pendiente, y pasa al aprobarse el último; el estado almacenado
      queda `APPROVED`. Un paciente no tiene estado de perfil (decisión del autor).
      `DocumentReviewDossierTest` fija cuándo se ofrece el botón.
- [x] Dado que rechazo un documento, cuando indico el motivo, entonces el usuario lo
      ve en su aplicación. **Verificado con experimento SQL:** el rechazo sin motivo
      y con motivo vacío o de 301 caracteres lo rechaza el motor; con motivo real
      pasa y **el dueño lo lee**. La pantalla del dueño ya lo mostraba desde HU-08.
- [x] Dado que soy administrador, cuando intento leer una conversación, entonces el
      acceso se deniega. **Prueba obligatoria `adminCannotReadMessages`, verificada
      el 2026-10-06** con una conversación real (oferta aceptada y `services`
      incluidos, porque desde el 2026-09-12 la política exige un servicio): el
      paciente y el profesional ven **1**, un tercero y el administrador ven **0**, y
      el administrador tampoco puede escribir ni marcar como leído. El primer
      intento de este experimento daba 0 a todos, porque la conversación de prueba
      no era legible ni para su paciente: **un 0 sin el 1 no distingue «deniega» de
      «no hay filas»**, y se corrigió antes de darlo por válido.
      `SchemaRulesTest.noMigrationGrantsTheAdministratorAccessToMessages` vigila en
      la JVM que ninguna migración agregue una política de administrador sobre `messages`.

**Requisitos:** RF-04.4, RN-11, INV-12.

**Tareas técnicas.** Pantallas de revisión para el administrador **dentro de la
aplicación** · función que custodia la promoción a `APPROVED` · exclusión explícita
de `messages` · prueba automatizada que verifique esa exclusión.

> **Cambio de alcance registrado.** Esta tarea decía «panel administrativo web
> mínimo». Contradecía «el producto es exclusivamente Android», y se resolvió a favor
> de la aplicación. Ver `docs/decisions.md`, 2026-10-06.

**Decisiones del autor que gobiernan la historia, tomadas el 2026-10-06.**

1. El panel vive en la aplicación Android; no hay nada web.
2. El profesional llega a `APPROVED` por un acto explícito del administrador, y lo
   custodia el motor.
3. Un paciente no tiene estado de verificación de perfil.
4. **El rol de administrador es exclusivo**: quien lo tiene no tiene otro rol. Por eso
   no se agregó ninguna cláusula contra la autorrevisión.
5. El super administrador que concede `ADMIN` es una historia aparte, HT-17.

**Qué cambió al cerrarla.**

- **Migración `20261006120000_admin_document_review.sql`**, aplicada con `db push
  --linked` tras `--dry-run`: exclusividad del rol `ADMIN`; `required_document_types`;
  `approve_professional_verification`; sello de la revisión por el motor; degradación
  del profesional cuando un documento requerido deja de estar aprobado; límite de 300
  caracteres del motivo; dos vistas de lectura con `security_invoker`.
- **Migración `20261006130000_harden_document_review.sql`**, que corrige lo que halló
  la revisión de código (abajo), con **un hueco crítico** entre ellos.
- **Cliente.** Todo dentro de `features/verification`: cola paginada, detalle con
  imagen por URL firmada de cinco minutos, veredicto por documento y confirmación de la
  verificación del profesional. Entrada desde «Mi cuenta», solo para el rol `ADMIN`.

**Experimentos SQL contra el proyecto remoto, 2026-10-06.** Todos dentro de
transacciones con `rollback`; residuo comprobado al terminar: los 4 documentos de
HU-07 intactos, y cero mensajes, solicitudes, ofertas y servicios. **Cada rechazo lleva
su control al lado.**

| Qué se comprobó | Resultado |
|---|---|
| El administrador ve la cola ajena, un tercero ve 0, el dueño ve la suya | **5 / 0 / 5** |
| Orden por antigüedad | Primero el más antiguo |
| Promover siendo un usuario cualquiera, o el propio profesional | **`not_authorized`** |
| Promover con todo pendiente / con un requerido pendiente | **`required_documents_not_approved`** |
| Promover con todo aprobado | **Pasa**, estado `APPROVED` |
| Un administrador escribe `verification_status` directo | **0 filas** (no hay política) |
| Promover una cuenta administradora | **`profile_is_not_a_professional`** |
| El motor sella quién y cuándo | `reviewed_by` y `reviewed_at` quedan firmados por el administrador que escribe, sin que el cliente los mande |
| Rechazo sin motivo / vacío / 301 caracteres | **`23514`** las tres veces |
| Rechazar un documento **requerido** de un `APPROVED` | Baja a **`PENDING`** |
| Control: rechazar el adjunto opcional `OTHER` | Se queda en `APPROVED` |
| Aprobar de nuevo un documento rechazado | El motivo queda **nulo** |
| Un no administrador escribe un veredicto ajeno | **0 filas** |
| Agregar `PATIENT` a un administrador / `ADMIN` a un paciente | **`admin_role_is_exclusive`** las dos |
| Control: agregar `PROFESSIONAL` a un paciente | **Pasa** |
| Administrador y mensajes (arriba) | **1 / 1 / 0 / 0 / 0** |
| Storage: administrador lee los objetos de HU-07 / un tercero | **4 / 0** |
| **Después de la revisión:** cambiar el tipo siendo `APPROVED` | Estado **`PENDING`** y **0** filas en el directorio (antes: `APPROVED` y 1) |
| Control: editar el perfil sin cambiar el tipo | Conserva `APPROVED` |
| El administrador reescribe `reviewed_by` sin veredicto | La fila acepta la sentencia y **conserva** el revisor y la fecha |
| Un administrador inserta un rol a otra persona | **`42501`** |
| Control: una persona se agrega un rol a sí misma | **Pasa** |
| Las vistas ya no exponen `storage_path` ni `photo_url` | **`42703`** las dos |
| `required_document_types` desde un usuario autenticado | **`42501`** |

Transversales: `everyTableHasRowLevelSecurityEnabledAndAtLeastOnePolicy`, **16 tablas,
0 sin cubrir**; **12 funciones `security definer`, 0 sin `search_path` fijo**;
**0 políticas de administrador sobre `messages`**.

**Revisión de código, 2026-10-06.** Tres revisores en paralelo (corrección y SQL;
convenciones y `CLAUDE.md`; seguridad y simplicidad). Dos de ellos hallaron, por caminos
independientes, **el mismo hueco crítico**, que se reprodujo en el remoto antes de
corregirlo:

1. **Un profesional `APPROVED` cambiaba su tipo desde su perfil y conservaba el estado.**
   Un estudiante aprobado pasaba a médico sin matrícula y seguía en el directorio
   público. Rompía INV-07. Lo corrige `guard_verification_status`, que ahora degrada.
2. **`approve_professional_verification` no bloqueaba la fila.** Un rechazo concurrente
   podía quedar pisado por un `APPROVED`. Ahora toma `for update` antes de leer.
3. **El sello de la revisión solo se disparaba ante un cambio de estado**, y el comentario
   afirmaba lo contrario. Ahora cubre toda actualización de un administrador.
4. **`profile_roles_insert_admin`** dejaba a una cuenta de administrador robada fabricar
   otro administrador. Se eliminó; nada del cliente la usaba.
5. **Las imágenes de los documentos quedaban en el caché de disco** del teléfono del
   administrador. Se desactivó.
6. `settle` dejaba el diálogo de rechazo abierto si fallaba la relectura tras una
   escritura exitosa, e invitaba a repetirla; y pisaba una imagen que llegó mientras tanto.
7. **La cola ocultaba filas** si un `refresh` y la página siguiente se cruzaban.
8. La pantalla decidía qué documentos son revisables (`reviewableTypes` pasó al dominio),
   el separador de roles estaba escrito a mano (ahora `ListFormatter`), «promover» y
   «aprobar al profesional» eran dos nombres de lo mismo, y varios comentarios
   incumplían la regla de `CLAUDE.md`.
9. Simplificación: dos columnas de las vistas que nadie leía, una comprobación de rol
   redundante y un valor de retorno constante.

Las correcciones 6 y 7 se comprobaron **por mutación**: sin el arreglo, sus pruebas
fallan.

**No se aplicó**, con su razón: los comentarios largos de las migraciones (la migración
`20261005130000` tiene el mismo estilo y es el precedente); las previsualizaciones
oscuras de los estados vacío, fallo y cargando (el precedente de `VerificationScreen`
las deja solo en claro; sí están en claro, oscuro y al 200 % las de contenido).

**Deuda declarada.**

- **El dibujo real de la imagen firmada con Coil no se comprobó.** Es la primera imagen
  del proyecto que viene de un bucket privado. Lo cubren la política de Storage verificada
  contra los objetos reales y los estados de la pantalla (cargando, error con reintento,
  sin cargar), pero no el dibujo. No lo exige la Definición de Terminado para esta historia.
- **La carrera de `for update` se razonó, no se ejercitó**: un experimento de una sola
  conexión no puede intercalar dos transacciones.
- **`supabase db reset`: resuelto el 2026-10-08**, con el entorno local de HT-18. Las 26
  migraciones reconstruyen la base desde cero sin error y `db diff --linked` no encuentra
  deriva contra el remoto.
- **Las previsualizaciones: resuelto el 2026-10-08.** Al cerrar la historia solo
  compilaban y nadie las había abierto, y el punto 9 de la Definición de Terminado pide
  que la pantalla «se vea correcta» en claro, oscuro y al 200 %. **El autor las revisó
  en Android Studio el 2026-10-08 y se vieron correctas** («de momento», dijo, y se
  anota tal cual: es una revisión de una persona sobre la versión de ese día). El punto
  9 queda cumplido.
- La paginación es por desplazamiento; por cursor sobre `(created_at, id)` si la cola crece.
- El detalle no tiene estado «vacío»: una persona con documentos pendientes siempre tiene
  contenido. Los otros tres estados sí están resueltos.
- Lo diferido, con su requisito: notificar el veredicto (FA-11), rechazar la postulación
  completa (FA-12), historial de veredictos de un documento reabierto (FA-13).

**Paso manual del autor: ninguno pendiente para cerrar HU-09.** La cuenta administradora
de pruebas ya está preparada y verificada (solo tiene el rol `ADMIN`). Si algún día hace
falta otra, este es el procedimiento, con los comandos ya ejecutados el 2026-10-06:

1. Que la persona ingrese una vez con Google, **sin elegir ningún rol**. Si ya eligió uno,
   el motor rechaza `ADMIN` con `admin_role_is_exclusive`: hay que limpiarla primero.
2. Averiguar su identificador:
   `npx supabase db query --linked "select id, email from public.profiles order by created_at;"`
3. Concederlo, en una sola transacción:
   `npx supabase db query --linked "insert into public.profile_roles (profile_id, role) values ('<uuid>', 'ADMIN'); update public.profiles set active_role = 'ADMIN' where id = '<uuid>';"`
4. Debe ocurrir: al ingresar con esa cuenta, «Mi cuenta» muestra solo **Revisar documentos**
   y **Cerrar sesión**.
5. Si falla: `admin_role_is_exclusive` indica que la cuenta ya tiene otro rol; no se arregla
   con la aplicación, porque `profile_roles` es de solo agregar (FA-09).

> **Advertencia sobre los datos de prueba.** La cola real contiene hoy los 4 documentos
> `PENDING` de la verificación en dispositivo de HU-07. **Aprobarlos o rechazarlos desde la
> aplicación los convierte en otra cosa** y esa evidencia ya no coincide con lo anotado en
> HU-07. Para recorrer el panel conviene subir documentos con otra cuenta.

**Verificaciones del cierre, 2026-10-06.** `./gradlew ktlintCheck staticAnalysis
assembleDebug` concluyen sin error. **59 suites, 331 pruebas, 0 fallos, 0 omitidas**, las
nuevas incluidas. Las dos migraciones están aplicadas en `salud-en-casa`.
**Previsualizaciones revisadas por el autor en Android Studio el 2026-10-08: correctas.**

**Revisión de `plan.md` al inicio y al final: realizadas las dos.**

**Archivos creados o modificados el 2026-10-06.**

| Capa | Archivos |
|---|---|
| Migraciones | `supabase/migrations/20261006120000_admin_document_review.sql` · `supabase/migrations/20261006130000_harden_document_review.sql` (nuevas) |
| Dominio | `features/verification/domain/model/{PendingDocumentReview, DocumentReviewSubject, DocumentReviewDossier, DocumentReviewResult}.kt` · `domain/vo/RejectionReason.kt` · `domain/repository/IDocumentReviewRepository.kt` · `domain/usecase/{GetPendingDocumentReviews, GetDocumentReviewDossier, ApproveDocument, RejectDocument, ApproveProfessionalVerification, GetSignedDocumentUrl}UseCase.kt` (nuevos) · `domain/model/{VerificationChecklist, VerificationError}.kt` · `domain/usecase/GetMyVerificationChecklistUseCase.kt` (modificados) |
| Datos | `data/model/DocumentReviewDto.kt` · `data/mapper/DocumentReviewMapper.kt` · `data/repository/DocumentReviewRepository.kt` (nuevos) · `data/datasource/SupabaseVerificationDataSource.kt` · `data/mapper/VerificationErrorMapper.kt` · `data/repository/VerificationRepository.kt` (modificados) |
| Presentación | `presentation/{DocumentReviewQueueViewModel, DocumentReviewQueueScreen, DocumentReviewViewModel, DocumentReviewScreen}.kt` (nuevos) · `presentation/{VerificationLabels, VerificationScreen}.kt` (modificados: etiquetas de los cuatro errores nuevos y dos ayudantes pasan a `internal`) |
| Inyección, navegación y cuenta | `di/VerificationModule.kt` · `navigation/{Routes, SaludEnCasaNavHost}.kt` · `features/auth/presentation/AccountScreen.kt` · `features/profile/domain/model/ProfileRoles.kt` (`isAdmin`, `addable` vacío para un administrador) |
| Recursos | `res/values/strings.xml` · `res/values-es/strings.xml` (claves `review_*`, `error_verification_*` nuevas, `cd_document_image`, `auth_account_open_document_review`) |
| Pruebas nuevas | `test/.../verification/FakeDocumentReviewRepository.kt` · `domain/vo/RejectionReasonTest.kt` · `domain/model/{DocumentReviewDossierTest, RequiredDocumentsParityTest}.kt` · `domain/usecase/{RejectDocumentUseCaseTest, GetPendingDocumentReviewsUseCaseTest, GetDocumentReviewDossierUseCaseTest}.kt` · `data/mapper/DocumentReviewMapperTest.kt` · `presentation/{DocumentReviewQueueViewModelTest, DocumentReviewViewModelTest}.kt` · `test/.../schema/SchemaRulesTest.kt` |
| Pruebas modificadas | `ProjectSources.kt` (`migrationFiles()`) · `VerificationChecklistTest.kt` · `VerificationErrorMapperTest.kt` · `ProfileRolesTest.kt` |
| Reglas y documentos | `.claude/rules/glosario.md` · `docs/decisions.md` (doce entradas del 2026-10-06) · `docs/requirements.md` (RF-01.9, RF-01.10, FA-11 a FA-13 y la fila de super administrador) · `docs/design-system.md` (visor de documento) · `plan.md` (esta sección y HT-17) |

## Incremento del sprint

Un profesional carga su título, el administrador lo aprueba, y el profesional
pasa a ser visible en las búsquedas. **El circuito de confianza queda cerrado.**

> **Qué se demostró de ese enunciado y qué no, dicho antes de que lo cuestionen.**
> Se demostró **contra la base**, con experimentos SQL en el remoto: el administrador
> aprueba los documentos, promueve, y el profesional aparece en `professional_directory`;
> si se le rechaza un documento requerido o cambia su tipo, desaparece. **No se
> demostró en pantalla de punta a punta**: la pantalla de búsqueda llega con HU-11
> (Sprint 4), y la revisión del administrador no se recorrió en un dispositivo. La
> misma cautela que el Sprint 2.5 aprendió a escribir en su enunciado.

## Retrospectiva

**El circuito de confianza quedó cerrado en la base, no todavía en pantalla.** Un
administrador revisa documentos, aprueba o rechaza con motivo, y promueve al
profesional; el motor decide quién es visible y quién deja de serlo. Eso se demostró
con experimentos contra el proyecto remoto. Lo que falta para decir «el profesional
aparece en las búsquedas» es la pantalla de búsqueda, que es HU-11.

### Velocidad medida: 24 puntos, los 24 planificados

HU-06 (3) + HU-07 (8) + HU-08 (3) + HU-09 (10) = 24. Las cuatro cumplen la Definición
de Terminado y ninguna se trasladó. Seis migraciones aplicadas y 331 pruebas en 59
suites al cierre, todas en verde.

> El Sprint 3 se ejecutó del **01/10/2026** al **06/10/2026**.

**Seis días de calendario, y la cuarta medición seguida que no mide capacidad.** El
sprint terminó porque se agotó el alcance y no el plazo, otra vez. Hay un matiz que
conviene escribir y que el tramo de fechas esconde: **ese tramo se solapa con el del
Sprint 2.5** (01/10 al 04/10), porque el Sprint 3 estuvo suspendido en medio. El trabajo
efectivo del Sprint 3 son la verificación de HU-06 del 03/10 y su cierre del 04/10, y
luego HU-07, HU-08 y HU-09 entre el 05/10 y el 06/10. **Veintiún puntos en dos días**,
después de 21 puntos en cuatro días (Sprint 1), 21 en dos (Sprint 2) y 23 en cuatro
(Sprint 2.5). Los puntos de este proyecto miden alcance planificado; no hay en ellos una
velocidad que proyectar, y la retrospectiva del Sprint 2.5 ya lo había sospechado.

**Lo que el número sí dice:** la estimación no está sistemáticamente corta, porque
trabajo estimado en 10 puntos (HU-09) entró entero, con dos migraciones, doce entradas
de decisión y una revisión de código que movió el diseño, y aun así cupo. **Lo que no
dice** es cuánto cabe en una semana sin tope de alcance, que es lo que se quería saber.

### Qué se cumplió de lo propuesto para este sprint

De los seis cambios que la retrospectiva del Sprint 2.5 pedía:

1. **Cerrar por fecha de corte: no se hizo.** Se cerró por alcance, igual que los tres
   anteriores. Es la cuarta vez que se plantea y la cuarta que no ocurre.
2. **HU-06 primero y hasta cerrarla: sí**, el 2026-10-04.
3. **Direcciones sin principal dentro de HU-06: sí**, con dos migraciones.
4. **Revisar con el criterio del 2026-10-02 las políticas de HU-07 y HU-09: sí**, y rindió:
   ninguna política de `verification_documents` hizo una subconsulta directa a
   `profiles`; la función auxiliar `has_role` se retiró por no tener consumidor, y las
   lecturas del administrador pasaron por vistas `security_invoker`. **Ninguna recursión
   de políticas en el sprint.**
5. **Cuenta de un solo rol: sí**, `ledezma.aramayo.73` sostuvo la verificación de HU-07.
6. **Pruebas de política con `supabase db query --linked`: sí**, en las cuatro historias.

### Qué funcionó

- **La revisión de código encontró el defecto más grave de cada historia, y ningún
  criterio de aceptación lo habría encontrado.** HU-07: una ruta de archivo que apuntaba
  a la carpeta de otro perfil, con el administrador como «diputado confundido», y la
  unicidad por ruta. HU-08: un profesional podía insertarse ya `APPROVED`. HU-09: un
  profesional aprobado cambiaba su tipo y seguía verificado y público. **Los tres son la
  misma clase de hueco:** el criterio describe el camino que se espera, y el defecto
  vive en *otro escritor* de la misma columna. `professionals.verification_status` tiene
  hoy cuatro caminos de escritura —el alta como `PENDING`, el guardia de actualización,
  la función de promoción y la degradación por disparadores—, y los dos huecos que tuvo
  (el alta ya `APPROVED` y el cambio de tipo) aparecieron por revisión, no por criterio.
- **Reproducir antes de corregir, y reproducir después.** El hueco del tipo se corrió
  contra el remoto antes de la migración (`APPROVED` y 1 fila en el directorio) y se
  repitió después (`PENDING` y 0 filas). Con el control al lado: editar el perfil sin
  cambiar el tipo conserva `APPROVED`.
- **El 0 sin el 1 no prueba nada, y esta vez se atrapó antes de afirmarlo.** El primer
  experimento de `adminCannotReadMessages` daba 0 a todos, el participante incluido,
  porque desde el 2026-09-12 la política exige un servicio y la conversación de prueba
  no lo tenía. La segunda versión inserta la oferta aceptada y el servicio, y recién ahí
  distingue.
- **Comprobar las pruebas por mutación.** Mi primera prueba de la carrera de la cola
  pasaba con o sin el arreglo: el fake devolvía siempre las mismas filas y el
  `distinctBy` la salvaba. Se rehízo con una respuesta que se suspende, y se verificó
  quitando la cancelación: falla. Una prueba que no puede fallar es la que
  `testing.md` pide no escribir.
- **Preguntar la decisión que cambia el diseño, y solo esa.** La exclusividad del rol
  `ADMIN`, que decidió el autor, hizo imposible la autorrevisión por construcción y
  evitó tapar el mismo caso en la política, la función y las vistas. Separar HT-17
  mantuvo HU-09 en sus 10 puntos.
- **El esquema del Sprint 0 pagó.** Las políticas `select_admin` y `update_admin` de
  `verification_documents`, y la lectura del administrador sobre el bucket, existían
  desde HT-04: el veredicto por documento fue un `PATCH` sin migración, y la historia se
  redujo a lo que de verdad faltaba.

### Qué no funcionó

- **Las previsualizaciones, el control que sustituyó al recorrido a mano, no se habían
  mirado al cerrar la historia.** HU-09 agrega siete previsualizaciones por pantalla y
  solo compilaban. **El autor las revisó el 2026-10-08 y se vieron correctas**, de modo
  que esta vez el control no encontró nada porque no había nada; pero el cierre del
  2026-10-06 declaró cumplido un punto que nadie había comprobado, y eso no cambia. El
  proyecto cambió el recorrido de cada pantalla por esas tres vistas «porque ahí se
  atrapan los recortes», y el único defecto de esa clase de este sprint (el chip de
  HU-07 apilado letra por letra) lo encontró un dispositivo, no una previsualización. **La deuda de `app/src/androidTest` sigue vacía**: el chip de HU-07
  fue su cuarto ejemplo, y HU-09 agrega dos pantallas sin ella. Se aceptó de forma
  consciente el 2026-10-04 y hoy no hay nada en su lugar.
- **Los comentarios incumplieron la regla de `CLAUDE.md` en el primer borrador.** El
  revisor de convenciones encontró comentarios que repetían el razonamiento junto a su
  cita, otros que explicaban una decisión sin citarla y otros que describían qué hace el
  código. La regla pide que la decisión exista en `decisions.md` **antes** de escribir el
  comentario; se hizo al revés, y hubo que registrar entradas después para poder citarlas.
- **Un nombre para dos cosas.** «Promover» y «aprobar al profesional» convivieron en la
  pantalla, el modelo de vista y las claves de cadena mientras el caso de uso, el
  repositorio y la función de la base decían lo segundo. Lo señaló el revisor; se unificó.
- **Una afirmación de mi propio comentario era falsa.** El disparador del sello decía que
  la revisión «no se puede falsificar», pero disparaba solo ante un cambio de estado.
  Lo encontró el revisor de seguridad. Un comentario que afirma una garantía hay que
  probarlo como se prueba la garantía.
- **`supabase db reset` no se corrió dentro del sprint**, aunque `CLAUDE.md` lo pide al
  cerrar todo sprint que toque el esquema. Se corrió **el 2026-10-08, después del cierre**,
  al levantar el entorno local de HT-18: las 26 migraciones se reconstruyen sin error y el
  remoto no tiene deriva. Que la verificación llegara dos días tarde es el defecto; el
  resultado salió limpio, pero eso no se sabía al cerrar.
- **Un riesgo de concurrencia razonado y no ejercitado**: el bloqueo `for update` de la
  promoción. Un experimento de una sola conexión no puede intercalar dos transacciones.

### Qué cambiar en el Sprint 4

1. **Cerrar por fecha de corte, con la fecha ya escrita. Decidido el 2026-10-08:** corte a
   los **7 días corridos** desde el primer día del Sprint 4; lo que no esté cerrado se
   traslada y se cuenta. Era la cuarta vez que se planteaba sin cumplirse porque nunca se
   fijó la fecha. Ya está escrita en el Sprint 4 y en `docs/decisions.md`.
2. **Decidir qué hacer con `androidTest` antes de agregar pantallas. Resuelto el
   2026-10-08 con HT-18:** se eligió escribir la prueba, y vive en la JVM. El Sprint 4
   agrega la búsqueda sobre un mapa, que es la pantalla que sostiene el producto, y
   ahora llega con un control automático contra los controles que se salen de la
   pantalla. **Lo que no cubre** —el texto recortado dentro de su contenedor— obliga a
   seguir abriendo las previsualizaciones, y el mapa es justo donde más importa.
3. **Antes de cerrar una historia, enumerar todos los escritores de las columnas que su
   invariante protege.** Es lo que habría atrapado los tres huecos de este sprint sin
   revisión. Para INV-07 la lista está arriba; para INV-09 (pago `BOTH_CONFIRMED`) y
   INV-11 (calificación única) conviene hacerla al empezar el Sprint 8 y el 9.
4. **HU-11 debe definir qué ve la contraparte de la verificación.** Hereda dos deudas:
   `professionals_select_counterpart` expone la fila completa de `professionals`, incluido
   el estado `PENDING` o `REJECTED` (registrado el 2026-10-05), y el distintivo de
   «verificado» para la contraparte no existe todavía (RF-04.6).
5. **Escribir primero la entrada de `decisions.md` y después el comentario que la cita.**
   Es la regla de `CLAUDE.md`; este sprint la violó por orden de trabajo.
6. **Programar HT-17 y la limpieza de `salud.en.casa.73`.** Sin super administrador, cada
   nuevo administrador exige la clave de servicio. No bloquea el Sprint 4, que necesita un
   profesional `APPROVED` y ya puede obtenerlo con la cuenta administradora.
7. **Decidir qué hacer con `supabase db reset`. Resuelto el 2026-10-08:** se levantó el
   entorno local con Docker. Las 26 migraciones se reconstruyen desde cero sin error, el
   catálogo del esquema local coincide con el remoto, y `db diff --linked` no encuentra
   deriva. Desde el Sprint 4 las políticas se pueden ejercitar en local antes de tocar el
   remoto.

### Acción pendiente del autor: ninguna

La única que quedaba, abrir las previsualizaciones de HU-09 en Android Studio y mirarlas,
la hizo el autor el 2026-10-08: se vieron correctas. Con eso el punto 9 de la Definición de
Terminado de HU-09 se cumple de verdad y no solo en el papel.

## Historia técnica planificada, fuera del Sprint 3

### HT-17 · Super administrador que concede el rol de administrador `[ ]`

> Como **autor del sistema**, quiero **un super administrador que sea el único que
> concede el rol de administrador**, para **que un administrador no pueda fabricar otro
> ni haya que tocar la base con la clave de servicio cada vez**.

**Estimación.** Por hacer en el Sprint Planning del sprint en que entre; no se estima
aquí porque no es parte de los 24 puntos del Sprint 3.

**Requisitos nuevos:** RF-01.10 (RF-01.9 ya rige desde HU-09).

**Decisiones ya tomadas el 2026-10-06** (razonamiento en `docs/decisions.md`):

- **`SUPER_ADMIN` es un cuarto valor de `user_role`**, agregado renombrando y recreando el
  tipo, como se hizo con `document_type`. No hereda nada del administrador: no lee
  documentos ni conversaciones. Es exclusivo, igual que `ADMIN`.
- **Flujo por invitación de correo, no por promoción directa.** El super administrador
  invita una dirección; `handle_new_user`, que ya es `security definer` y ya conoce el
  correo, consume la invitación al primer ingreso e inserta el rol. Así la cuenta nunca
  pasa por la pantalla de elección de rol, que es la trampa: `profile_roles` es de solo
  agregar y quien elija «paciente» queda inelegible para siempre.
- **La identidad no se versiona.** La migración crea el mecanismo y ningún dato personal.
  Quién es super administrador se instala una vez con la clave de servicio, tecleando el
  valor en la terminal, con un procedimiento documentado con marcadores.
- **No hay revocación**: conceder `ADMIN` es irreversible sin la clave de servicio (FA-09).

**Hecho ya en HU-09 y que esta historia no repite:** la exclusividad del rol `ADMIN` y la
eliminación de `profile_roles_insert_admin`.

**Tareas técnicas previstas.** Valor `SUPER_ADMIN` · tabla de invitaciones de solo agregar,
con seguridad por fila solo para el super administrador · extensión de `handle_new_user` ·
pantalla de invitación para ese rol · experimentos de política con su control · prueba de
que el super administrador no lee documentos ni mensajes.

**Dato para quien la tome:** la cuenta reservada para este rol tiene hoy 3 direcciones de
prueba y rol de paciente, así que hay que limpiarla en la misma sesión en que se le
concede el rol. Una cuenta con perfil y sin rol queda atrapada en la pantalla de elección
de rol hasta que se le concede otro.

### HT-18 · Primera prueba de interfaz y entorno local reproducible `[x]` — 2026-10-08

> Como **autor del sistema**, quiero **una prueba automática que verifique la disposición
> al 200 % de fuente y un entorno local que reconstruya la base desde cero**, para **dejar
> de depender de que alguien se acuerde de mirar, y de afirmar sin respaldo que las
> migraciones son reproducibles**.

Cierra las dos deudas que la retrospectiva del Sprint 3 dejó abiertas, los cambios 2 y 7.
Razonamiento completo en `docs/decisions.md`, 2026-10-08, dos entradas.

**Requisitos:** RNF-07 (la interfaz responde al ajuste de tamaño de fuente), y la
exigencia de `CLAUDE.md` de que `supabase db reset` reconstruya la base al cerrar un
sprint que toque el esquema. Recupera además el criterio aplazado de HT-04.

**Lo hecho, con su evidencia.**

| Qué | Resultado |
|---|---|
| Robolectric 4.17, estable, con `ui-test-junit4` en `app/src/test` | Una prueba de Compose corre en la JVM, sin emulador |
| `graphicsMode=NATIVE` en `app/src/test/resources/robolectric.properties` | Sin él cada `Text` medía 3 dp y ninguna aserción valía; con él las medidas son reales |
| Siete pruebas en cuatro pantallas, a 320 dp y `fontScale = 2f` | Cada control se alcanza, se muestra y no se sale de los bordes |
| **Mutación:** los botones de veredicto en un `Row` en vez de un `FlowRow` | **Falla**: «Rechazar documento» queda fuera de la pantalla |
| **Mutación:** se quita el desplazamiento vertical del detalle | **Fallan dos**: el pie se vuelve inalcanzable |
| `supabase start` con `[analytics]` desactivado | El entorno local arranca sano; antes los contenedores de analítica y `vector` lo hacían fallar |
| `supabase db reset` | **26 migraciones aplicadas desde cero, sin error** |
| Catálogo local reconstruido contra el remoto | **Idénticos**: 16 tablas, 0 sin RLS, 67 políticas, 12 funciones `definer` con `search_path` fijo, 0 políticas de administrador sobre `messages`, 12 tipos de servicio |
| `supabase db diff --linked --schema public` | **«No schema changes found»**: el remoto no tiene deriva |

**Dos pruebas escritas y descartadas.** Cubrían la forma del defecto de HU-07 —el chip
apilado letra por letra— y **pasaban con y sin el defecto**, también en el ancho donde se
reprodujo. Se eliminaron en vez de dejarlas: una prueba que no puede fallar es la que
`testing.md` manda no escribir. Lo que esa forma necesita está abajo.

**Deuda declarada, y es importante.** **El texto recortado dentro de su propio contenedor
no lo atrapa ninguna de estas pruebas.** Compose mide ese `Text` al ancho que el
contenedor le da: se forzó un texto de veinte caracteres a 60 dp y tanto los límites
recortados como los sin recortar devolvieron 60 dp. Dos de los cinco defectos históricos
—el rótulo del control segmentado del Sprint 2.5 y el chip de HU-07— son de esa forma, de
modo que **mirar las previsualizaciones sigue siendo obligatorio**. La única vía
automática sería comparar capturas de pantalla (Roborazzi), al precio de imágenes de
referencia versionadas que alguien aprueba a ojo y regenera en cada cambio intencional de
interfaz: queda nombrada, no adoptada, para que el autor decida.

**`androidTest` sigue vacío, a propósito.** La integración continua corre solo tareas de
JVM, así que una prueba ahí no se ejecutaría nunca. `connectedAndroidTest` no ejecuta nada
hoy; `README.md` y `.claude/rules/testing.md` lo dicen.

**Archivos.** `gradle/libs.versions.toml` y `app/build.gradle.kts` (Robolectric y
`isIncludeAndroidResources`) · `app/src/test/resources/robolectric.properties` (nuevo) ·
`app/src/test/java/bo/saludencasa/ui/LargeFont.kt` (nuevo, el ayudante) · tres archivos de
prueba nuevos · las cuatro funciones de contenido pasan a `internal` ·
`supabase/config.toml` · `README.md` · `.claude/rules/testing.md` · `docs/decisions.md` ·
`plan.md`.

**Verificaciones del cierre, 2026-10-08.** `./gradlew ktlintCheck staticAnalysis
assembleDebug` sin error. **62 suites, 338 pruebas, 0 fallos, 0 omitidas.**

---

# Sprint 4 — Catálogo y búsqueda por cercanía

**Objetivo del sprint.** El paciente encuentra profesionales verificados cerca de
su domicilio, filtrados por el tipo de atención que necesita.

**Objetivo específico.** 3 · Comunicación, coordinación y agenda.

**Puntos:** 23.

**Fecha de corte: 7 días corridos desde el primer día del sprint** (decidido el
2026-10-08). El sprint se cierra ese día aunque queden historias abiertas: lo que no esté
cerrado se traslada, y lo cerrado se cuenta. Es la primera vez que un sprint de este
proyecto cierra por plazo y no por alcance, y por eso es la primera medición de velocidad
que mide capacidad. **Primer día del sprint:** _anotar al empezar_, y con él la fecha de
corte (primer día + 6 días, ambos incluidos).

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

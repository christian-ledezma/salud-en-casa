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
- **Al cerrar un sprint, su sección se mueve completa a `docs/sprints/sprint-NN.md`**
  y aquí queda su resumen en «Sprints cerrados»: fechas, incremento, historias y el
  enlace. Se mueve sin cambiar el texto; solo se agrega el encabezado del archivo.

**Qué contiene este archivo y qué no.** Aquí vive lo que rige el trabajo de hoy: el
marco, la Definición de Terminado, el índice, el sprint activo y los que faltan. El
detalle de un sprint cerrado —criterios marcados, experimentos SQL, retrospectiva—
está en su archivo de `docs/sprints/`, y se abre solo cuando una tarea lo cita. Ese
corte se hizo el 2026-11-22, cuando este archivo superaba los 500 KB y ya no se podía
leer al inicio de una sesión.

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
- Entrega efectiva de notificaciones push (HU-20). Solo HU-20: las historias
  que después agregan un evento al mismo canal —HU-17, HU-18, HU-19, HU-21, HU-23,
  HU-24, HU-25, HU-26, HU-28 y HU-37— verifican destinatarios y contenido con
  experimentos SQL y pruebas, y no repiten la demostración en el teléfono.
- La validación con usuarios reales del Sprint 10, que por definición ocurre ahí.
- La compra de la suscripción con Google Play (HU-39): la hoja de compra la dibuja la
  tienda, y solo aparece en una versión instalada desde un canal de Play Console.

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

> **Contraejemplo del 2026-10-09, anotado porque el tribunal puede preguntarlo.**
> HU-09 **no** está en ninguno de los dos niveles de arriba: el panel del administrador
> no rompe el flujo central del paciente ni mueve dinero. El autor lo recorrió de todos
> modos con las dos cuentas, y el recorrido encontró un defecto que **ninguna prueba
> podía ver**: el botón que verifica al profesional quedaba sin puerta en cuanto se
> aprobaba el último documento, porque la cola no listaba a nadie con todo aprobado. No
> era un defecto de disposición —era de recorrido—, y no se agrega HU-09 a la tabla por
> esto: lo que el caso muestra es que las dos categorías de arriba cubren «qué pantallas
> hay que mirar» y no cubren «qué caminos hay que caminar». La respuesta a eso no es una
> tabla más larga, es recorrer el incremento de cada sprint de punta a punta antes de
> cerrarlo. Ver «El hueco del 09/10» en el Sprint 3.

---

## Índice de sprints

| # | Sprint | Objetivo específico | Puntos | Estado | Detalle |
|---|---|---|---|---|---|
| 0 | Fundación técnica | 1 · Arquitectura | — | `[x]` | [`sprint-00.md`](docs/sprints/sprint-00.md) |
| 1 | Ingreso e identidad | 2 · Perfiles | 21 | `[x]` | [`sprint-01.md`](docs/sprints/sprint-01.md) |
| 2 | Perfiles y ubicación | 2 · Perfiles | 21 | `[x]` | [`sprint-02.md`](docs/sprints/sprint-02.md) |
| 2.5 | Rol múltiple con rol activo | 2 · Perfiles | 23 | `[x]` | [`sprint-02-5.md`](docs/sprints/sprint-02-5.md) |
| 3 | Verificación de usuarios | 2 · Perfiles | 24 | `[x]` | [`sprint-03.md`](docs/sprints/sprint-03.md) |
| 4 | Catálogo y búsqueda por cercanía | 3 · Coordinación | 23 | `[x]` | [`sprint-04.md`](docs/sprints/sprint-04.md) |
| 5 | Solicitudes de atención | 3 · Coordinación | 24 | `[x]` | [`sprint-05.md`](docs/sprints/sprint-05.md) |
| 6 | Avisos y negociación de tarifas | 3 · Coordinación | 42 | `[x]` | [`sprint-06.md`](docs/sprints/sprint-06.md) |
| 7 | Comunicación y ciclo del servicio | 3 · Coordinación | 56 | `[x]` | [`sprint-07.md`](docs/sprints/sprint-07.md) |
| 8 | Pago de la atención y vigencia del profesional | 4 · Pagos | 29 | `[ ]` | Abajo, sprint activo |
| 9 | Calificación e historial | 5 · Evaluación | 22 | `[ ]` | Abajo |
| 10 | Validación con usuarios reales | 6 · Pruebas | — | `[ ]` | Abajo |
| 11 | Publicación y suscripción en Google Play | 4 · Pagos | Por estimar | `[ ]` | Abajo |

**El modelo de negocio cambió el 2026-11-22.** La plataforma dejó la comisión por
atención y se financia con una suscripción que paga solo el profesional, cobrada con
la facturación de Google Play (`docs/decisions.md`, esa fecha). El Sprint 8 se
replanificó antes de empezar, y el Sprint 11 se agregó porque la compra exige
publicar en la tienda, que es lo último antes de producción.

**El Sprint 0 cerró el 2026-10-15** con sus doce historias técnicas terminadas.
Fue el sprint que recibió el trabajo de fundación que apareció más tarde —HT-19,
HT-20 y HT-21 entraron ahí en octubre—; cerrado el sprint, una historia técnica
nueva entra en el sprint en curso y con sus puntos, que es lo que pide la
retrospectiva del Sprint 5.

**Primer incremento demostrable:** Sprint 1. **Ciclo completo de negocio de punta a
punta:** Sprint 8. **Sistema completo:** Sprint 9. **Suscripción cobrada en la
tienda:** Sprint 11.

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

- **Sin pasarela para el pago de la atención.** Se paga fuera de la aplicación y se
  registra con confirmación mutua, íntegro para el profesional. El modelo ya
  contempla `method`, `provider` y `external_reference` para incorporar una pasarela
  sin cambio de esquema (FA-02).

- **La plataforma se financia con la suscripción del profesional.** Desde el
  2026-11-22, en lugar de la comisión por atención. Se cobra solo con la facturación
  de Google Play; el motor decide la vigencia, con un interruptor en
  `platform_settings` y 30 días de prueba al aprobarse la verificación.

- **La validación se distribuye por App Distribution; la tienda llega al final.** El
  Sprint 10 corre por App Distribution con el cobro apagado. La publicación en Google
  Play, condicionada a la cuenta de organización con D-U-N-S, es el Sprint 11 (FA-03,
  incorporado el 2026-11-22).

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

## Sprints cerrados

Cada sprint cerrado vive completo en `docs/sprints/`, con sus criterios, experimentos
y retrospectiva. Aquí queda lo necesario para ubicarlo: fechas, incremento e
historias. Una cita como «plan.md, HU-24» se resuelve buscando la historia en esta
lista y abriendo su archivo.

### Sprint 0 — Fundación técnica

**Incremento.** Una prueba de humo con un botón que autentica con Google, crea el perfil en la base de datos y muestra el identificador obtenido. Es código desechable: se elimina en HU-01.

- HT-01 · Repositorio, proyecto y estructura `[x]`
- HT-02 · Servicios externos configurados `[x]`
- HT-03 · Catálogo de versiones `[x]`
- HT-04 · Esquema de base de datos `[x]` — último criterio cerrado el 2026-10-09
- HT-05 · Cliente de Supabase, secretos e inyección de dependencias `[x]`
- HT-06 · Sistema de diseño `[x]`
- HT-07 · Estructura de internacionalización `[x]`
- HT-08 · Integración continua, análisis estático y monitoreo `[x]`
- HT-09 · Documentación de arquitectura `[x]`
- HT-19 · Identidad de marca: ícono y arranque `[x]` — 2026-10-13
- HT-20 · La bienvenida con la marca `[x]` — 2026-10-14
- HT-21 · La búsqueda y la ficha del profesional `[x]` — 2026-10-15

Detalle completo: [`docs/sprints/sprint-00.md`](docs/sprints/sprint-00.md).

### Sprint 1 — Ingreso e identidad

El Sprint 1 se ejecutó del **11/09/2026** al **14/09/2026**.

**Incremento.** Un usuario nuevo ingresa con Google, elige ser paciente, completa su perfil y lo ve persistido al reabrir la aplicación. **Primera demostración a la contraparte.**

- HU-01 · Ingresar con cuenta de Google `[x]` — 8 puntos
- HU-02 · Elegir mi rol `[x]` — 5 puntos
- HU-03 · Completar mis datos básicos `[x]` — 8 puntos

Detalle completo: [`docs/sprints/sprint-01.md`](docs/sprints/sprint-01.md).

### Sprint 2 — Perfiles y ubicación

El Sprint 2 se ejecutó del **14/09/2026** al **15/09/2026**.

**Incremento.** Un profesional publica su perfil con tarifa y radio, y registra su domicilio sobre el mapa. La consulta de cercanía en la base de datos ya lo encuentra.

- HU-04 · Publicar mi perfil profesional `[x]` — 8 puntos
- HU-05 · Registrar mi dirección en el mapa `[x]` — 13 puntos

Detalle completo: [`docs/sprints/sprint-02.md`](docs/sprints/sprint-02.md).

### Sprint 2.5 — Rol múltiple con rol activo

El Sprint 2.5 se ejecutó del **01/10/2026** al **04/10/2026**.

**Incremento.** Una persona con una sola cuenta de Google pide atención como paciente, cambia de rol con un control en «Mi cuenta», y queda registrada como profesional con su propia base de cobertura. **Enunciado con cuidado para no repetir el error del Sprint 2:** no promete que las búsquedas la encuentren, porque eso sigue dependiendo de la aprobación del administrador, que llega con HU-09.

- HU-34 · Tener más de un rol `[x]` — 8 puntos
- HT-16 · Cerrar los huecos que el rol múltiple destapa `[x]` — 5 puntos
- HU-35 · Cambiar de rol con un switch `[x]` — 5 puntos
- HU-36 · Separar mi domicilio de mi base profesional `[x]` — 5 puntos

Detalle completo: [`docs/sprints/sprint-02-5.md`](docs/sprints/sprint-02-5.md).

### Sprint 3 — Verificación de usuarios

El Sprint 3 se ejecutó del **01/10/2026** al **06/10/2026**.

**Incremento.** Un profesional carga su título, el administrador lo aprueba, y el profesional pasa a ser visible en las búsquedas. **El circuito de confianza queda cerrado.**

- HU-06 · Administrar mis direcciones `[x]` — 3 puntos
- HU-07 · Cargar mis documentos de verificación `[x]` — 8 puntos
- HU-08 · Conocer el estado de mi verificación `[x]` — 3 puntos
- HU-09 · Revisar documentos pendientes `[x]` — 10 puntos
- HT-18 · Primera prueba de interfaz y entorno local reproducible `[x]` — 2026-10-08

Detalle completo: [`docs/sprints/sprint-03.md`](docs/sprints/sprint-03.md).

### Sprint 4 — Catálogo y búsqueda por cercanía

El Sprint 4 se ejecutó del **09/10/2026** al **11/10/2026**.

**Incremento.** Un paciente abre la aplicación, ve en el mapa a los profesionales verificados que están a menos de cinco kilómetros de su casa, ordenados por distancia y con su tarifa, filtra por el tipo de atención que necesita, y abre la ficha de uno para ver su biografía, sus servicios con sus precios y su distintivo de verificado. **El diferenciador central del producto queda demostrado.**

- HU-10 · Declarar los servicios que presto `[x]` — 5 puntos
- HU-11 · Buscar profesionales cerca de mi domicilio `[x]` — 13 puntos
- HU-12 · Consultar la ficha de un profesional `[x]` — 5 puntos

Detalle completo: [`docs/sprints/sprint-04.md`](docs/sprints/sprint-04.md).

### Sprint 5 — Solicitudes de atención

El Sprint 5 se ejecutó del **12/10/2026** al **15/10/2026**.

**Incremento.** Un paciente publica una solicitud inmediata y el profesional la ve aparecer en su bandeja sin recargar. Otro paciente agenda una cita para el jueves.

- HU-13 · Solicitar atención inmediata `[x]` — 8 puntos
- HU-14 · Ver las solicitudes de mi zona `[x]` — 8 puntos
- HU-15 · Declarar mi disponibilidad horaria `[x]` — 3 puntos
- HU-16 · Agendar una cita `[x]` — 5 puntos

Detalle completo: [`docs/sprints/sprint-05.md`](docs/sprints/sprint-05.md).

### Sprint 6 — Avisos y negociación de tarifas

El Sprint 6 se ejecutó del **16/10/2026** al **30/10/2026**.

**Incremento.** Una solicitud publicada dispara una notificación a los profesionales cercanos, y una cita agendada, a su destinatario; uno oferta y al paciente le llega el aviso, el paciente contraoferta y al profesional le llega el suyo, el profesional acepta y el servicio queda creado con su pago pendiente. Nadie tuvo que estar mirando la aplicación. **El mecanismo distintivo del producto está completo.**

- HT-23 · Pedir atención y agendar en menos pasos `[x]` — 8 puntos
- HT-22 · Servicios de Google en un proyecto con dos dueños `[x]` — 5 puntos
- HU-20 · Recibir avisos de solicitudes `[x]` — 8 puntos
- HU-17 · Ofertar por una solicitud `[x]` — 5 puntos
- HU-18 · Contraofertar `[x]` — 8 puntos
- HT-24 · Negociar en menos pasos `[x]` — 8 puntos
- HU-19 · Aceptar una oferta `[x]` — 8 puntos

Detalle completo: [`docs/sprints/sprint-06.md`](docs/sprints/sprint-06.md).

### Sprint 7 — Comunicación y ciclo del servicio

El Sprint 7 se ejecutó del **30/10/2026** al **05/11/2026**.

**Incremento.** Las partes conversan, el profesional registra lo realizado, marca llegada y finalización, y la atención queda lista para el pago. Cada paso le llega a la contraparte como notificación, y el veredicto de cada documento le llega a quien lo subió.

- HT-25 · La pantalla de inicio y la barra de navegación `[x]` — 8 puntos
- HU-21 · Conversar sobre la atención `[x]` — 10 puntos
- HT-26 · Mis atenciones: el profesional vuelve a lo que acordó `[x]` — 3 puntos
- HU-22 · Registrar lo realizado en la atención `[x]` — 5 puntos
- HU-23 · Marcar el avance de la atención `[x]` — 8 puntos
- HT-27 · Las listas se actualizan y las tarjetas dicen lo mismo a los dos lados `[x]` — 8 puntos
- HT-28 · Aceptar una oferta desde donde se comparan `[x]` — 3 puntos
- HU-24 · Cancelar una atención `[x]` — 5 puntos
- HT-29 · Contraofertar el mismo monto acepta la oferta `[x]` — 3 puntos
- HU-37 · Enterarme del veredicto de mi verificación `[x]` — 3 puntos

Detalle completo: [`docs/sprints/sprint-07.md`](docs/sprints/sprint-07.md).

---

## Pendiente sin sprint

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

---

# Sprint 8 — Pago de la atención y vigencia del profesional

**Objetivo del sprint.** El pago de cada atención se registra con confirmación de
ambas partes, íntegro para el profesional, y la visibilidad del profesional pasa a
depender de una suscripción vigente.

**Objetivo específico.** 4 · Controles de confirmación y validación mutua de pagos.

**Puntos:** 29. Eran 24 hasta el 2026-11-24, cuando HU-28 sumó la confirmación en cualquier orden (de 3 a 8 puntos).

**Replanificado el 2026-11-22, antes de empezar.** La plataforma dejó la comisión por
atención y se financia con una suscripción que paga solo el profesional, cobrada por
Google Play (`docs/decisions.md`, esa fecha). El sprint tenía 21 puntos: HU-25, HU-26
con el desglose de la comisión, HU-27 con el saldo de comisiones y HU-28 con su
conciliación. HT-30 retira la comisión del motor, HU-26 y HU-28 se reducen, HU-27 se
retira y entra HU-38. La compra de la suscripción no entra aquí: exige la cuenta de
Play Console, y va en el Sprint 11.

**Pendiente de cierre del sprint.** `db reset` y `db diff --linked` de HT-30, que exigen
Docker y quedaron para cuando el autor lo decida. Deben correrse antes de marcar el Sprint 8.

**Orden.** HT-30 primero, porque HU-25 y HU-26 leen el pago que deja. HU-38 al final:
no depende del pago de la atención, y así el ciclo de la atención queda cerrado antes
de tocar la visibilidad del profesional.

### HT-30 · Retirar la comisión del modelo `[x]` — 2026-11-23, con un criterio pendiente — 3 puntos

> Como **autor del sistema**, quiero **que el motor deje de calcular una comisión que
> la plataforma ya no cobra**, para **que el pago de la atención diga lo que de verdad
> ocurre**.

**Criterios de aceptación**

- [x] Dado que se acepta una oferta, cuando `accept_offer` crea el pago, entonces su
      total es el monto acordado entero y no hay comisión que calcular.
- [x] Dado que consulto el esquema, cuando reviso `payments` y `platform_settings`,
      entonces ya no existen `platform_fee_bob`, `professional_amount_bob`,
      `settled_at`, `platform_fee_percent` ni el estado `SETTLED`.
- [x] Dado que un participante intenta cambiar el total de un pago, cuando escribe,
      entonces se rechaza (INV-05).
- [ ] `supabase db reset` reconstruye la base y `db diff --linked` no muestra más
      diferencia que esta migración. **Pendiente por decisión del autor, 2026-11-23:**
      exige Docker y lo enciende cuando él decida. Vence al cerrar el Sprint 8, como
      pide `.claude/rules/supabase.md` para todo sprint que toca el esquema.

**Requisitos:** RF-11.1, RN-08, INV-05, INV-10.

**Tareas técnicas.** Migración nueva, porque una aplicada no se edita: `accept_offer`
reescrita sin desglose; `guard_payment_is_frozen`, `guard_payment_confirmation` y
`confirmations_match_status` sin `settled_at` ni `SETTLED`; `payment_status` renombrado
y recreado sin `SETTLED`, como se hizo con `document_type`; las columnas, retiradas.
En el mismo cambio, `docs/architecture/data-model.md` (diagrama y tabla de invariantes) y
el glosario (`platform_fee_percent`, «Comisión», `SETTLED`). Experimento SQL
`paymentTotalEqualsTheFrozenAgreedAmount`: aceptar crea un pago por el monto entero, y
escribir su total se rechaza. Las filas de prueba del remoto pierden su desglose; sus
experimentos ya están anotados en HU-19 y HT-29.

**Cierre con un criterio abierto.** HT-30 cierra el 2026-11-23 con el cuarto criterio sin
cumplir. No se retocó el criterio para que lo hecho encaje: sigue diciendo lo que dice, y lo
que falta es correr `supabase start -x studio`, `supabase db reset` y
`supabase db diff --linked --schema public`, y anotar aquí el resultado con su fecha. Mientras
tanto, lo único que se probó sobre la cadena completa de migraciones es que esta aplica sobre
los datos reales del remoto, no que reconstruya la base desde cero. Es el mismo trato que
HU-06 recibió con su recorrido en emulador.

**Decisiones del autor, 2026-11-22** (`docs/decisions.md`, «HT-30: el pago es el monto
acordado»): INV-10 pasa a decir que el total iguala el monto congelado del servicio y lo
hace cumplir un disparador al insertar; `payment_status` se recrea sin `SETTLED`; el
`db push` lo ejecuta el autor tras ver los experimentos.

**Experimentos SQL, 2026-11-22.** Contra el remoto, antes del `db push`: cada uno aplica
la migración `20261123035149_ht30_retire_platform_commission.sql` completa dentro de
`begin … rollback`, con la identidad indicada, y se confirmó después que el remoto
quedó intacto (3 columnas de comisión presentes, el disparador nuevo ausente, sin filas
de prueba, 7 pagos). Cada uno trae el caso que pasa y el que se rechaza.

| # | Experimento | Caso que pasa | Caso que se rechaza |
|---|---|---|---|
| 0 | Estructura resultante, sobre los 7 pagos reales | La migración aplica sin errores. `payments` queda sin `platform_fee_bob`, `professional_amount_bob` ni `settled_at`; `platform_settings` sin `platform_fee_percent`; `payment_status` con `PENDING, PATIENT_CONFIRMED, BOTH_CONFIRMED, DISPUTED, CANCELLED`; el disparador `payments_guard_total_matches_service` existe | — |
| 1 | Aceptar crea el pago entero | El paciente acepta una oferta de 120,00: pago `PENDING` con total 120,00 igual al monto del servicio, oferta y solicitud `ACCEPTED`, en una transacción | — |
| 2 | Total congelado (INV-05) | El profesional de la atención actualiza `method` a `CASH` y se acepta | El mismo profesional cambia `total_amount_bob` y se rechaza: `payment_breakdown_is_frozen` |
| 3 | `paymentTotalEqualsTheFrozenAgreedAmount` (INV-10) | Insertar un pago con el monto del servicio (100,00) | Insertar uno con 101,00: `payment_total_must_equal_the_agreed_amount` |
| 4 | `SETTLED` retirado | `'DISPUTED'` y `'CANCELLED'` siguen siendo valores válidos | `'SETTLED'::payment_status` falla: `invalid input value for enum` |
| 5 | La cancelación sigue anulando el pago | Una parte cancela una atención asignada y su pago pasa a `CANCELLED` con el total intacto (98,00) | — |
| 6 | Confirmación por separado (INV-09) | El paciente confirma su parte sobre una atención completada: `PATIENT_CONFIRMED`, sin la del profesional | El profesional escribe la confirmación del paciente: `each_party_confirms_separately` |

Identidades: paciente `299073bc…`, profesional `fcab94c0…`. El 1 creó su solicitud y su
oferta dentro de la misma transacción.

**Aplicada en el remoto, 2026-11-23.** El autor ejecutó `db push`. Consulta de solo lectura
después: `db push --dry-run` dice «Remote database is up to date»; `payments` tiene 11
columnas, ninguna de comisión; `platform_settings` solo `id` e
`immediate_request_ttl_minutes`; `payment_status` es `PENDING, PATIENT_CONFIRMED,
BOTH_CONFIRMED, DISPUTED, CANCELLED`; `payments` lleva los disparadores
`payments_guard_confirmation`, `payments_guard_frozen`,
`payments_guard_total_matches_service` y `payments_set_updated_at`; los 7 pagos siguen
(5 `PENDING`, 2 `CANCELLED`) y ninguno difiere del monto de su servicio.

**Verificación de código.** `./gradlew build` en verde: 1132 pruebas, 0 fallas, 0
omitidas. La primera pasada dejó `testDebugUnitTest` en `UP-TO-DATE`, de modo que no
había leído la migración; se forzó con `--rerun` y `SchemaRulesTest` (2), `ArchitectureRulesTest`
(3) y `NotificationEventParityTest` (1) pasan con ella. Ningún archivo Kotlin cambió: la
aplicación no lee el desglose del pago.

**Recorrido en emuladores, 2026-11-23.** Lo pidió el autor al arrancar la historia. Con el
paciente `ledezma.aramayo.73` y el profesional `chris.ledezma.s`, ya con la migración en el
remoto, y verificado después contra la base:

| Atención | Qué se hizo | Oferta | Pago | Servicio | Solicitud |
|---|---|---|---|---|---|
| Curación de heridas, Bs 110,00 | El paciente aceptó la oferta y luego canceló con motivo «No hoy» | `ACCEPTED`, 110,00 | `CANCELLED`, total 110,00 | `CANCELLED` | `CANCELLED` |
| Aplicación de inyectables, Bs 35,00 | El paciente aceptó, el profesional marcó la llegada y la finalización | `ACCEPTED`, 35,00 | `PENDING`, total 35,00 | `COMPLETED`, con llegada y fin sellados a 1 minuto | `COMPLETED` |

En las dos, el total del pago es el monto de la oferta aceptada, sin desglose, y ninguna
confirmación del pago quedó escrita. En todo el remoto hay 9 pagos y ninguno difiere del
monto congelado de su servicio. Lo que la base prueba es que los dos caminos llegaron a su
estado final; lo que se vio en pantalla lo reportó el autor. La confirmación del pago por las dos partes no tiene pantalla hasta HU-25, de modo que esa parte
queda cubierta por el experimento 6 y no por el recorrido.

### HU-25 · Confirmar que pagué `[x]` — 2026-11-24 — 5 puntos

> Como **paciente**, quiero **declarar que pagué la atención**, para **cerrar mi
> parte del compromiso**.

**Criterios de aceptación**

- [x] Dado que la atención se completó, cuando abro el pago, entonces veo el monto
      acordado y el medio de pago a elegir.
- [x] Dado que confirmo el pago, cuando el profesional consulta, entonces ve mi confirmación.
- [x] Dado que confirmé, cuando el profesional aún no confirma, entonces el pago
      figura pendiente de la otra parte.
- [x] Dado que la atención no se completó, cuando intento confirmar, entonces se rechaza.
- [x] Dado que confirmo el pago, cuando se registra, entonces al profesional le
      llega una notificación que lo lleva a confirmar el cobro.

**Requisitos:** RF-11.2, RF-11.4, RF-09.7, RF-09.10, INV-09.

**A tener en cuenta (2026-11-04).** El aviso `SERVICE_COMPLETED` de HU-23 ya abre la pantalla de la atención. Esta historia
pone ahí la acción de pagar, con lo que se termina de cumplir su último criterio, y puede sumar el pago a la línea de tiempo.

**Decisiones del autor, 2026-11-23** (`docs/decisions.md`, «HU-25: el paciente confirma que pagó»): en la
pantalla de la atención, con «Confirmar que pagué» al pie y un diálogo; efectivo y QR o transferencia, sin uno
preseleccionado; definitiva; el aviso lleva nombre, monto y medio; el profesional ve la confirmación sin botón
hasta HU-26; el pago no entra en la línea de tiempo. Prototipo aprobado:
[Confirmar pago HU-25](https://claude.ai/artifact/TA8muKTUVSFfQqMHti8r84).

**Experimentos SQL, 2026-11-23.** Contra el remoto, antes del `db push`: cada uno aplica la migración
`20261123140557_hu25_confirm_payment.sql` completa dentro de `begin … rollback`, con la identidad indicada
(paciente `299073bc…`, profesional `fcab94c0…`) sobre la atención completada de Aplicación de inyectables,
Bs 35,00. Después se confirmó que el remoto quedó intacto: ninguno de los tres disparadores nuevos existe y
ningún pago tiene confirmación.

| # | Experimento | Resultado |
|---|---|---|
| 1 | `paymentReachesConfirmedOnlyWhenBothPartiesConfirm`: el paciente confirma | `PATIENT_CONFIRMED`, medio `CASH`, `patient_confirmed_at` igual a la hora de la transacción, `professional_confirmed_at` nulo; 1 aviso encolado y 1 señal en `attention:<solicitud>` |
| 1b | El paciente intenta saltar a `BOTH_CONFIRMED` | Rechazado: `patient_confirms_a_pending_payment_once` |
| 2 | Confirmar sobre una atención en curso | Rechazado: `payment_confirmation_requires_a_completed_service` |
| 3a | El profesional pasa el pago a `PATIENT_CONFIRMED` | Rechazado: `each_party_confirms_separately` |
| 3b | El profesional escribe el medio | Rechazado: `payment_method_is_declared_by_the_patient` |
| 4a | El paciente deshace su confirmación | Rechazado: `patient_confirms_a_pending_payment_once` |
| 4b | El paciente cambia el medio después | Rechazado: `patient_confirms_a_pending_payment_once` |
| 4c | Confirmar dos veces lo mismo | Pasa sin cambiar nada: sigue `PATIENT_CONFIRMED`, `CASH` |
| 5a | Confirmar sin medio | Rechazado: `payment_method_not_allowed` |
| 5b | Confirmar con `GATEWAY` | Rechazado: `payment_method_not_allowed` |
| 5c | Confirmar con `DIRECT_QR` | Pasa: `PATIENT_CONFIRMED`, `DIRECT_QR` |
| 6 | El paciente envía su propia hora | Rechazado: `confirmation_time_is_sealed_by_the_engine` |
| 7 | `notificationPayloadCarriesNoTextWrittenByAUser` para el evento nuevo | Destinatarios: solo el profesional. Cuerpo: evento, solicitud, rol `PROFESSIONAL`, tipo de servicio, `amountBob` 35.00, `paymentMethod` `CASH`, `senderName` del paciente y `ttlSeconds`; ni la descripción ni ningún texto escrito |
| 8 | El paciente cancela una atención asignada | El pago pasa a `CANCELLED`: la regla nueva no bloquea la cancelación |
| 9 | `my_attention` para cada parte | Las dos leen `PATIENT_CONFIRMED`, `CASH` y la confirmación del paciente |

**Verificación de código, 2026-11-23.** `./gradlew build` en verde: 1159 pruebas, 0 fallas, 0 omitidas (27
nuevas). La primera corrida falló en `NotificationContentTest.theLockScreenVersionNeverShowsTheServiceType`: el
aviso nuevo no nombraba el tipo de servicio, que esa regla previa exige a todo aviso de una solicitud. Se corrigió
el texto, no la prueba (`docs/decisions.md`, HU-25).

**Aplicada y desplegada, 2026-11-23.** El autor ejecutó `db push` y `functions deploy send-push`. Consulta de
solo lectura después: `db push --dry-run` dice «Remote database is up to date» y existen
`payments_stamp_confirmation`, `payments_queue_push` y `payments_notify_change`.

**Recorrido en emuladores, 2026-11-24.** Historia crítica («Cuándo hace falta un dispositivo»). Paciente
`ledezma.aramayo.73` y profesional `chris.ledezma.s`, sobre la atención completada de Aplicación de inyectables,
Bs 35,00: el profesional la tenía abierta sin botón de pago; el paciente vio «Confirmar que pagué», el diálogo con
«Confirmar pago» deshabilitado hasta elegir el medio, eligió efectivo y confirmó; la atención del profesional se
actualizó sola y le llegó el aviso «Pago confirmado», que abre esa atención; una atención en curso no ofreció
pagar. El autor lo recorrió sin observaciones. Después, en la base: ese pago en `PATIENT_CONFIRMED`, `CASH`,
`patient_confirmed_at` 24/11 00:54 sellado por el motor, `professional_confirmed_at` nulo; es el único pago
confirmado del remoto, y las atenciones en curso siguen `PENDING`. La entrega del aviso la confirma el recorrido:
`net._http_response` ya no conserva esa respuesta.

**Previsualizaciones.** Siete nuevas en `AttentionScreen.kt`, en claro, oscuro y 200 % a 320 dp: el diálogo, el
paciente que pagó y el profesional que lo lee. Mirarlas es del autor (punto 9 de la Definición de Terminado);
las pruebas de interfaz al 200 % no ven el texto recortado dentro de su contenedor.

### HU-26 · Confirmar que cobré `[x]` — 2026-11-24 — 5 puntos

> Como **profesional**, quiero **declarar si recibí el pago**, para **que la atención
> quede cerrada o, si no me pagaron, que la plataforma lo revise**.

**Criterios de aceptación**

- [x] Dado que el paciente confirmó, cuando confirmo la recepción, entonces el pago
      pasa a confirmado por ambas partes.
- [x] Dado que el pago se confirma, cuando lo consulto, entonces veo el monto
      acordado, íntegro para mí, y el medio que declaró el paciente.
- [x] Dado que el paciente declara haber pagado y yo declaro no haber recibido,
      cuando ambos registramos, entonces el pago queda en disputa.
- [x] Dado que el pago queda en disputa, cuando ocurre, entonces al paciente le
      llega una notificación de que el administrador lo revisará.

**Requisitos:** RF-11.3, RF-11.4, RF-11.5, RF-09.7, RF-09.10, RN-09, INV-09, INV-10.

**Antes 8 puntos.** Llevaba el desglose de la comisión, su cálculo como caso de uso con
pruebas de valores límite y la restricción de cuadre en el motor; HT-30 retira las tres.
Queda el estado de disputa.

**Decisiones del autor, 2026-11-24** (`docs/decisions.md`, «HU-26: el profesional responde una vez»): dos botones
al pie, «Confirmar que cobré» y «No recibí el pago», cada uno con su diálogo y definitivos; la disputa sin motivo;
solo después de que el paciente confirmó; aviso solo al paciente al quedar en disputa, y RF-09.7 corregido;
`payments.disputed_at` sellada por el motor; bajo el chip del cobro, que el monto es todo del profesional. Prototipo
aprobado: [Confirmar cobro HU-26](https://claude.ai/artifact/LNr2uo5Lf3M2vhMJqRNp88).

**Experimentos SQL, 2026-11-24.** Contra el remoto, antes del `db push`: cada uno aplica la migración
`20261124054159_hu26_confirm_receipt.sql` completa dentro de `begin … rollback`, con la identidad indicada (paciente
`299073bc…`, profesional `fcab94c0…`). Sobre la atención de Aplicación de inyectables, Bs 35,00, ya confirmada por el
paciente en efectivo, salvo donde se dice otra cosa. Después se confirmó que el remoto quedó intacto: sin
`disputed_at` y ese pago todavía en `PATIENT_CONFIRMED`.

| # | Experimento | Resultado |
|---|---|---|
| 0 | Estructura, sobre los 9 pagos reales | La migración aplica; `disputed_at` existe; ningún pago en disputa; los siete disparadores de `payments` siguen |
| 1 | `paymentReachesConfirmedOnlyWhenBothPartiesConfirm`: el profesional confirma el cobro | `BOTH_CONFIRMED`, `professional_confirmed_at` igual a la hora de la transacción, sin `disputed_at`; 0 avisos encolados y 1 señal en `attention:<solicitud>` |
| 2 | `mismatchedConfirmationsLeavePaymentInDispute`: el profesional declara que no lo recibió | `DISPUTED`, `disputed_at` igual a la hora de la transacción, `professional_confirmed_at` nulo; 1 aviso encolado y 1 señal |
| 3a | Confirmar el cobro con el pago pendiente (Bs 45,00, completada) | Rechazado: `professional_answers_a_patient_confirmed_payment_once` |
| 3b | Disputar con el pago pendiente | Rechazado: `professional_answers_a_patient_confirmed_payment_once` |
| 3c | El profesional pasa el pago a `PATIENT_CONFIRMED` | Rechazado: `each_party_confirms_separately` |
| 3d | Disputar una atención en curso | Rechazado: `professional_answers_a_patient_confirmed_payment_once` |
| 4a | Confirmado, después disputar | Rechazado: `professional_answers_a_patient_confirmed_payment_once` |
| 4b | En disputa, después confirmar | Rechazado: `professional_answers_a_patient_confirmed_payment_once` |
| 4c | Confirmar el cobro dos veces | Pasa sin cambiar nada: sigue `BOTH_CONFIRMED` |
| 4d | Confirmado, volver a `PATIENT_CONFIRMED` | Rechazado: `each_party_confirms_separately` |
| 5a | El profesional envía su propia hora | Rechazado: `confirmation_time_is_sealed_by_the_engine` |
| 5b | El profesional envía la hora de la disputa | Rechazado: `confirmation_time_is_sealed_by_the_engine` |
| 6a | El paciente pasa el pago a `DISPUTED` | Rechazado: `patient_confirms_a_pending_payment_once` |
| 6b | El paciente pasa el pago a `BOTH_CONFIRMED` | Rechazado: `patient_confirms_a_pending_payment_once` |
| 7 | `notificationPayloadCarriesNoTextWrittenByAUser` para `PAYMENT_DISPUTED` | Destinatarios: solo el paciente, en sus dos dispositivos. Cuerpo: evento, solicitud, rol `PATIENT`, tipo de servicio, `amountBob` 35.00, `senderName` del profesional y `ttlSeconds`; ni medio, ni descripción, ni texto escrito |
| 8a | `my_attention` del paciente, en disputa | `DISPUTED`, `CASH`, con `disputed_at`, sin hora del profesional |
| 8b | `my_attention` del profesional, cobrado | `BOTH_CONFIRMED`, `CASH`, con la hora del profesional, sin `disputed_at` |
| 9 | El ciclo entero sobre Bs 45,00: el paciente confirma por QR y el profesional disputa | `DISPUTED`, `DIRECT_QR`, horas selladas; 1 aviso y 1 señal |
| 10 | Una cuenta ajena a la atención intenta disputar | 0 filas actualizadas: la política no la deja ver el pago |

**Verificación de código, 2026-11-24.** `./gradlew build --rerun` en verde: 1184 pruebas, 0 fallas, 0 omitidas (25
nuevas). La primera corrida de `theNotReceivedDialogKeepsItsWarningTheErrorAndItsAnswersOnScreen` falló por la
prueba, no por la pantalla: comprobaba primero el cuerpo largo, y desplazarse hasta él dejaba fuera de la vista el
error, que el diálogo ya había mostrado solo. Se reordenó para comprobar primero lo que la persona ve sin gesto.

**Aplicada, 2026-11-24.** El autor ejecutó `db push`; `send-push` no se volvió a desplegar porque el aviso nuevo no
agrega claves. Consulta de solo lectura después: `db push --dry-run` dice «Remote database is up to date», existe
`payments.disputed_at`, y `confirmations_match_status`, `guard_payment_confirmation` y `request_push_body` son los
de esta migración.

**Recorrido en emuladores, 2026-11-24.** Historia crítica («Cuándo hace falta un dispositivo»). Paciente
`ledezma.aramayo.73` y profesional `chris.ledezma.s`. Sobre la atención de Aplicación de inyectables, Bs 35,00, ya
pagada en efectivo: el profesional vio los dos botones al pie, volvió atrás desde el diálogo sin cambiar nada,
confirmó el cobro y leyó que el monto es todo suyo; la pantalla del paciente se actualizó sola, sin aviso. Sobre la
otra, Bs 45,00, con el pago pendiente: el paciente confirmó por QR o transferencia, al profesional le llegó «Pago
confirmado», que abre la atención, y declaró que no lo recibió; los dos vieron «Pago en revisión» y al paciente le
llegó el aviso. La atención de Curación de heridas, Bs 95,00, no ofreció responder mientras estaba en curso; el autor
además la completó y las dos partes confirmaron su pago. El autor lo recorrió sin observaciones.

Después, en la base, a la hora de La Paz:

| Atención | Estado | Medio | Paciente | Profesional | Disputa |
|---|---|---|---|---|---|
| Aplicación de inyectables, Bs 35,00 | `BOTH_CONFIRMED` | `CASH` | 24/11 00:54:47 | 24/11 10:06:44 | — |
| Aplicación de inyectables, Bs 45,00 | `DISPUTED` | `DIRECT_QR` | 24/11 10:09:47 | — | 24/11 10:11:58 |
| Curación de heridas, Bs 95,00 | `BOTH_CONFIRMED` | `CASH` | 24/11 10:13:15 | 24/11 10:13:25 | — |

`net._http_response` registra las cuatro llamadas a `send-push` del recorrido, todas 200 con un envío: la confirmación
del paciente a las 10:09:47, la disputa a las 10:11:58 (que además retiró un token caducado del paciente), la
atención completada a las 10:12:57 y la segunda confirmación del paciente a las 10:13:15. A las 10:06:44 y 10:13:25,
cuando el profesional confirmó el cobro, no hay ninguna: confirmar no avisa a nadie. Las tres quedan como evidencia;
la de Bs 45,00 es la disputa real con la que se prueba HU-28.

**Previsualizaciones.** Quince nuevas en `AttentionScreen.kt`, en claro, oscuro y 200 % a 320 dp: la barra con los
dos botones, los dos diálogos, el cobro confirmado visto por cada parte y el pago en revisión. El autor las miró el
2026-11-24, sin observaciones (punto 9 de la Definición de Terminado).

### ~~HU-27 · Consultar mi saldo de comisiones~~ — retirada el 2026-11-22

La plataforma ya no cobra comisión, y RF-11.6 se retiró con ella. El número se
conserva para que las referencias anteriores sigan siendo legibles. Su lugar en el
sprint lo ocupa HU-38.

### HU-28 · Resolver las disputas de pago y confirmar en cualquier orden `[x]` — 2026-11-24 — 8 puntos

> Como **administrador**, quiero **revisar y resolver los pagos en disputa**, para
> **que un desacuerdo entre paciente y profesional no quede abierto**.

**Criterios de aceptación**

- [x] Dado que hay pagos en disputa, cuando abro la revisión, entonces los veo con
      monto, medio declarado, participantes y fechas, del más antiguo al más
      reciente y paginados.
- [x] Dado que reviso una disputa, cuando la resuelvo en cualquiera de los dos
      sentidos, entonces queda registrado quién la resolvió, cuándo y en qué sentido.
- [x] Dado que soy administrador, cuando abro una disputa, entonces veo montos,
      estados y participantes, pero nunca el contenido de la conversación.
- [x] Dado que resuelvo una disputa, cuando registro la resolución, entonces al
      paciente y al profesional les llega una notificación que los lleva al pago.
- [x] Dado que la atención se completó y el pago está pendiente, cuando el profesional
      confirma que cobró eligiendo el medio, entonces su parte queda cerrada y al paciente
      le llega un aviso para aceptarlo.
- [x] Dado que el profesional confirmó el cobro, cuando el paciente lo acepta, entonces el
      pago pasa a confirmado por ambas partes; el paciente no puede negarlo.
- [x] Dado que una parte ya declaró, cuando la otra intenta declarar primero, entonces se
      rechaza y ve lo que declaró la otra.

**Requisitos:** RF-11.2, RF-11.3, RF-11.4, RF-11.5, RF-11.7, RF-09.7, RF-09.10, RN-09, RN-11, INV-05, INV-09,
INV-12.

**Antes 5 puntos, con la conciliación,** que se retiró con la comisión, y después 3. **Pasó a 8 el 2026-11-24:** el
autor vio que el orden de HU-25 y HU-26 hacía esperar al profesional que ya había cobrado, y pidió que declare primero
quien sea. Los tres criterios nuevos son ese cambio.

**Decisiones del autor, 2026-11-24** (`docs/decisions.md`, «HU-28: el pago se declara en cualquier orden»):
- **Orden.** Declara primero quien sea. Si el profesional es el primero, elige el medio, al paciente le llega un aviso
  y el paciente solo acepta. «No recibí el pago» sigue siendo solo una respuesta, y el paciente que nunca confirma queda
  como FA-21.
- **Resolución.** Dos estados finales, resuelto como pagado y resuelto como no pagado; el motor sella quién y cuándo.
  Es definitiva, con un diálogo antes y sin nota.
- **Pantallas del administrador.** Inicio, después la lista y después el detalle; el detalle muestra el correo de las
  dos partes, nunca la conversación.
- **Propuestas menores aprobadas con el plan.** Aceptar el cobro sin diálogo; «Se pagó» y «No se pagó» con el mismo
  peso visual; ningún aviso al administrador.

Prototipo aprobado: [Pago en cualquier orden y disputas HU-28](https://claude.ai/artifact/M4fXW5Wjk9EvHUmDbKXehf).

**Hallazgo de la planificación.** Desde el primer esquema, la guarda de pagos devolvía la fila tal como llegara si
quien escribía era administrador: podía reescribir cualquier pago. Ahora solo pasa una disputa a una de sus dos
resoluciones.

**Experimentos SQL, 2026-11-24.** Contra el remoto, antes del `db push`: cada uno aplica la migración
`20261124150053_hu28_payment_in_any_order_and_disputes.sql` completa dentro de `begin … rollback`, con la identidad
indicada (paciente `299073bc…`, profesional `fcab94c0…`, administrador `08ddb28f…`). Las atenciones son de la misma
pareja: Aplicación de inyectables, Bs 45,00, en curso y completada dentro del experimento; la otra de Bs 45,00, en
disputa desde HU-26; la de Bs 35,00, confirmada por ambos; y Curación de heridas, Bs 100,00, asignada. Después se
confirmó que el remoto quedó intacto: el tipo con sus cinco estados, sin `resolved_at` ni la vista, y la disputa de
Bs 45,00 todavía en `DISPUTED`.

| # | Experimento | Resultado |
|---|---|---|
| 0 | Estructura, sobre los 9 pagos reales | La migración aplica; ocho estados; los pagos conservan el suyo (3 pendientes, 2 confirmados por ambos, 1 en disputa, 3 cancelados); los siete disparadores siguen |
| 1 | El profesional declara primero, en efectivo | `PROFESSIONAL_CONFIRMED`, su hora igual a la de la transacción, sin hora del paciente; 1 aviso y 1 señal |
| 1b | `notificationPayloadCarriesNoTextWrittenByAUser` para `PROFESSIONAL_CONFIRMED_PAYMENT` | Destinatario: solo el paciente. Cuerpo: evento, solicitud, rol `PATIENT`, tipo de servicio, monto, medio y nombre del profesional |
| 2 | `paymentReachesConfirmedOnlyWhenBothPartiesConfirm`: el paciente acepta el cobro por QR | `BOTH_CONFIRMED`, medio intacto, hora del paciente sellada ahora; 0 avisos y 1 señal |
| 3a | El paciente niega el cobro del profesional | Rechazado: `patient_only_accepts_the_professionals_receipt` |
| 3b | El paciente acepta cambiando el medio | Rechazado: `patient_only_accepts_the_professionals_receipt` |
| 3c | El paciente declara después de que declaró el profesional | Rechazado: `patient_only_accepts_the_professionals_receipt` |
| 4a | El profesional declara sin medio | Rechazado: `payment_method_not_allowed` |
| 4b | El profesional declara con `GATEWAY` | Rechazado: `payment_method_not_allowed` |
| 4c | El profesional declara con la atención en curso | Rechazado: `payment_confirmation_requires_a_completed_service` |
| 4d | El profesional declara después de que declaró el paciente | Rechazado: `professional_declares_a_pending_payment_once` |
| 4e | El profesional cierra el pago con su propio cobro | Rechazado: `professional_answers_a_patient_confirmed_payment_once` (INV-09) |
| 4f | El profesional cambia después el medio que declaró | Rechazado: `professional_declares_a_pending_payment_once` |
| 5 | El camino de HU-26 sigue: el paciente primero, el profesional confirma | `BOTH_CONFIRMED`; 0 avisos y 1 señal |
| 5b | El paciente primero sigue avisando al profesional | `PATIENT_CONFIRMED`; 1 aviso y 1 señal |
| 6 | `onlyAnAdministratorResolvesADispute`: se pagó | `RESOLVED_PAID`, `resolved_at` igual a la hora de la transacción, `resolved_by` el administrador, horas anteriores intactas; 2 avisos y 1 señal |
| 6b | No se pagó | `RESOLVED_NOT_PAID`, lo mismo; 2 avisos y 1 señal |
| 6c | Cuerpo de «Pago revisado» para el paciente | Solo el paciente, rol `PATIENT`, `status` `RESOLVED_NOT_PAID`, nombre del profesional; ni el del administrador ni texto escrito |
| 6d | Cuerpo para el profesional | Solo el profesional, rol `PROFESSIONAL`, nombre del paciente |
| 6e | Cuerpo pedido para alguien que no es parte | Vacío: no se envía |
| 7a | Resolver dos veces, en sentidos distintos | Rechazado: `an_administrator_only_resolves_a_dispute` |
| 7b | El administrador resuelve un pago confirmado por ambos | Rechazado: `an_administrator_only_resolves_a_dispute` |
| 7c | El administrador pasa una disputa a confirmada por ambos | Rechazado: `an_administrator_only_resolves_a_dispute` |
| 7d | El administrador cambia el medio al resolver | Rechazado: `an_administrator_only_resolves_a_dispute` |
| 7e | El administrador escribe la hora de la resolución | Rechazado: `confirmation_time_is_sealed_by_the_engine` |
| 7f | El administrador nombra a otro como quien resolvió | Rechazado: `confirmation_time_is_sealed_by_the_engine` |
| 8a | El paciente resuelve | Rechazado: `patient_confirms_a_pending_payment_once` |
| 8b | El profesional resuelve | Rechazado: `each_party_confirms_separately` |
| 8c | Una cuenta ajena resuelve | 0 filas: la política no la deja ver el pago |
| 9a | El administrador lee la lista | 1 disputa con tipo, monto, medio, horas y nombre y correo de cada parte |
| 9b | El paciente y el profesional leen la lista | 0 filas cada uno |
| 9c | Escribir por la vista | Rechazado: la vista no se puede actualizar |
| 9d | Una disputa resuelta sale de la lista | 0 filas |
| 9e | `adminCannotReadMessages` sobre la atención en disputa | La conversación tiene 3 mensajes; el administrador lee 0 |
| 10a | `my_attention` del paciente tras la resolución | `RESOLVED_PAID`, con hora de disputa y de resolución, sin hora del profesional |
| 10b | `my_attention` del profesional que declaró primero | `PROFESSIONAL_CONFIRMED`, `CASH`, sin hora del paciente |
| 11 | El paciente cancela una atención asignada | El pago pasa a `CANCELLED`: la guarda nueva no bloquea la cancelación |

**Verificación de código, 2026-11-24.** `./gradlew build --rerun` en verde: 1233 pruebas, 0 fallas, 0 omitidas (49
nuevas). Una prueba previa cambió porque cambió la regla, no porque fallara:
`thePayDialogOpensOnlyForThePatientOfACompletedAttention` pasó a
`theDeclarationOpensOnlyOnACompletedAttentionNobodyDeclaredYet`, ya que ahora el profesional también declara. ktlint
dejó tres líneas largas sin corregir solo, y se partieron a mano.

**Aplicada, 2026-11-24.** El autor ejecutó `db push`; `send-push` no se volvió a desplegar. Consulta de solo lectura
después: `db push --dry-run` dice «Remote database is up to date», `payment_status` tiene sus ocho estados y existe
`payment_dispute_queue`.

**Recorrido en emuladores, 2026-11-24.** Historia crítica («Cuándo hace falta un dispositivo»). Paciente
`ledezma.aramayo.73`, profesional `chris.ledezma.s` y administrador `christian.ledezma@ucb.edu.bo`. Sobre la
atención de Aplicación de inyectables, Bs 45,00, que estaba en curso: el profesional la completó, vio «Confirmar que
cobré», eligió efectivo en el diálogo y leyó que su parte estaba cerrada; al paciente le llegó «Cobro confirmado»,
que abre la atención con solo «Aceptar el cobro», y al aceptarlo el pago quedó confirmado por ambos sin aviso. Sobre la
disputa de Bs 45,00 de HU-26: el administrador entró desde «Revisar pagos en disputa», vio la disputa en la lista y su
detalle con lo que declaró cada uno y el correo de las dos partes, volvió una vez desde el diálogo y la dio por no
pagada; la lista quedó vacía y a las dos partes les llegó «Pago revisado», que abre la atención con «Resuelto: no
pagado». El autor lo recorrió sin observaciones.

Después, en la base, a la hora de La Paz:

| Atención | Estado | Medio | Paciente | Profesional | Disputa | Resolución |
|---|---|---|---|---|---|---|
| Aplicación de inyectables, Bs 45,00, la que estaba en curso | `BOTH_CONFIRMED` | `CASH` | 24/11 14:16:18 | 24/11 14:11:16 | — | — |
| Aplicación de inyectables, Bs 45,00, la disputa de HU-26 | `RESOLVED_NOT_PAID` | `DIRECT_QR` | 24/11 10:09:47 | — | 24/11 10:11:58 | 24/11 14:21:25, por el administrador |

`net._http_response` registra, todas 200 con un envío: la atención completada a las 14:09:39, «Cobro confirmado» a las
14:11:16 y una respuesta a las 14:21:25. A esa hora debían salir dos, una por parte, y el autor vio llegar las dos; la
tabla conserva solo una, y después el paciente quedó sin teléfono registrado, porque el administrador cerró su sesión
en el emulador del paciente. Que la resolución encola un aviso por parte lo prueban los experimentos 6 y 6b. Al aceptar
el cobro no hubo envío. Las dos atenciones quedan como evidencia.

**Previsualizaciones.** Veintiséis nuevas: 13 en `AttentionScreen.kt`, 6 en `DisputeQueueScreen.kt` y 7 en
`DisputeReviewScreen.kt`, en claro, oscuro y 200 % a 320 dp. El autor las miró el 2026-11-24, sin observaciones (punto
9 de la Definición de Terminado).

### HU-38 · Saber hasta cuándo aparezco ante los pacientes `[ ]` — 8 puntos

> Como **profesional**, quiero **saber hasta cuándo está vigente mi suscripción y qué
> pierdo sin ella**, para **no dejar de recibir solicitudes sin enterarme**.

**Criterios de aceptación**

- [ ] Dado que se aprueba mi verificación, cuando consulto mi suscripción, entonces
      tengo 30 días de prueba con su fecha de fin.
- [ ] Dado que mi suscripción está vigente, cuando abro mi Inicio, entonces veo hasta
      cuándo.
- [ ] Dado que el cobro está activado y mi suscripción venció, cuando un paciente
      busca o publica una solicitud en mi radio, entonces no aparezco, no la recibo en
      mi bandeja y no me llega su aviso.
- [ ] Dado que mi suscripción venció, cuando intento una primera oferta, entonces se
      rechaza y la aplicación me dice por qué.
- [ ] Dado que mi suscripción venció con una negociación abierta o una atención
      acordada, cuando sigo, entonces puedo contraofertar en ese hilo y avanzar la
      atención.
- [ ] Dado que el cobro está desactivado, cuando un profesional aprobado y activo
      opera, entonces todo funciona como antes de esta historia.
- [ ] Dado que soy paciente, también con los dos roles, cuando pido atención,
      entonces nada me pide una suscripción.

**Requisitos:** RF-14.1, RF-14.2, RF-14.3, RF-14.5, RF-14.6, RN-01, INV-07, INV-18.

**Tareas técnicas.** Propuesta, a confirmar al planificar la historia:

- `platform_settings.subscription_required`, booleano apagado por omisión, y
  `trial_days`, 30. Se cambian por migración, como el resto de `platform_settings`.
- Tabla `subscription_periods`, de solo agregar y con su política en la misma
  migración: profesional, origen (`TRIAL` ahora, `PLAY_BILLING` en el Sprint 11),
  inicio y fin. Solo el motor escribe; el profesional lee los suyos.
- Un definidor, `professional_has_valid_subscription`, que leen
  `search_nearby_professionals`, `request_inbox`, `immediate_request_recipients`,
  `professional_may_offer_on` (solo para la primera oferta), `create_scheduled_request`
  y la acción «Agendar cita» de la ficha.
- Un disparador que crea la prueba en la transición a `APPROVED`, y la prueba de los
  profesionales ya aprobados, contada desde la fecha de la migración.
- El estado de la suscripción en Ajustes y su fila en el Inicio del profesional, con
  prototipo aprobado antes de programar.
- El interruptor se enciende solo para el experimento y la demostración. El Sprint 10
  corre con él apagado.

## Incremento del sprint

Una atención se completa, ambas partes confirman su pago, íntegro para el
profesional, y el administrador resuelve las disputas. **El ciclo de negocio de la
atención está completo de punta a punta.** La visibilidad del profesional depende de
una suscripción vigente, demostrable con la prueba de 30 días y el interruptor
encendido. La compra de la suscripción llega en el Sprint 11.

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

> **El tercer criterio cierra una verificación que HU-12 dejó abierta.** La ficha
> muestra los comentarios desde el 2026-10-11, pero el remoto no tenía ninguno y no
> podía tenerlo: una calificación exige una atención completada. El camino de datos
> se verificó con experimentos SQL; lo que falta es recorrerlo en el emulador, y
> esta historia es la primera vez que eso será posible.

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
      atención con su monto y el estado de su pago.
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
- [ ] La pantalla de consentimiento de OAuth está **en producción**. Google lo exige
      con una página principal y una política de privacidad publicadas, y la
      política es también el destino del botón «Política de privacidad» de la
      bienvenida, que hoy no lleva a ningún lado. El texto lo redacta y revisa el
      autor. Viene de HT-22: hasta el 2026-10-23 el ingreso funcionaba en «Prueba»
      con la lista de usuarios de prueba vacía, pero nada garantiza que Google lo
      siga permitiendo. Debe quedar hecho antes de HT-13.
- [ ] `platform_settings.subscription_required` está apagado en el remoto durante el
      piloto. Por App Distribution nadie puede comprar la suscripción, y con el cobro
      encendido los profesionales desaparecerían al vencer su prueba (HU-38).

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

---

# Sprint 11 — Publicación y suscripción en Google Play

**Objetivo del sprint.** La aplicación se publica en Google Play y el profesional
compra y renueva su suscripción desde ella.

**Objetivo específico.** 4 · Controles de confirmación y validación mutua de pagos.

**Puntos:** por estimar en su Sprint Planning. Las historias de abajo son la propuesta
del 2026-11-22, al cambiar el modelo de negocio (`docs/decisions.md`, esa fecha).

**Condición de entrada.** La cuenta de organización de Google Play, con su número
D-U-N-S, y el perfil de pagos de comerciante en Bolivia. Sin ellas el sprint no puede
empezar: Play Billing solo se prueba con una versión subida a un canal de prueba de
Play Console. **Si no llegan antes de la defensa, la compra no se demuestra**, y la
regla de vigencia se muestra con la prueba de 30 días (HU-38).

### HT-31 · Publicar en el canal de prueba interna `[ ]`

- [ ] La aplicación está en el canal de prueba interna de Play Console, con su ficha,
      su clasificación de contenido, la declaración de seguridad de los datos y la
      política de privacidad publicada (la de HT-10).
- [ ] **La huella SHA-1 de la clave de firma de Play App Signing está registrada** en
      el identificador OAuth de Android y en Firebase. Google firma la versión de la
      tienda con su propia clave, y sin esa huella el ingreso con Google falla solo en
      esa versión. El `applicationId` no cambia.
- [ ] El perfil de pagos de comerciante está creado. El país del perfil no se cambia
      después, y el pago llega en dólares por transferencia desde Estados Unidos.

### HU-39 · Suscribirme desde la aplicación `[ ]`

> Como **profesional**, quiero **suscribirme y renovar desde la aplicación**, para
> **seguir apareciendo ante los pacientes sin hacer trámites aparte**.

- [ ] Dado que soy profesional, cuando abro mi suscripción, entonces veo el plan con
      el precio que dice Google Play y me suscribo con la hoja de compra de la tienda.
- [ ] Dado que compro, cuando la compra se completa, entonces mi período queda vigente
      solo después de que el servidor la verificó con Google Play.
- [ ] Dado que un cliente envía un token de compra que Google no reconoce, cuando el
      servidor lo verifica, entonces no se crea ningún período.
- [ ] Dado que soy paciente, cuando uso la aplicación, entonces nunca veo una compra.

**Requisitos:** RF-14.1, RF-14.4, RF-14.7, INV-18.

**Tareas técnicas.** La biblioteca de facturación de Google Play, en versión estable y
en `gradle/libs.versions.toml`, exigida por RF-14.4. Una función de servidor que
verifica el token con la API de Google Play, reconoce la compra y crea el período con
origen `PLAY_BILLING`; la credencial de la cuenta de servicio vive solo en los secretos
de Supabase (INV-14). Probada con cuentas de prueba de licencia, que no pagan.

### HU-40 · Renovaciones y cancelaciones `[ ]`

> Como **profesional**, quiero **que mi suscripción refleje lo que hago en Google
> Play**, para **no perder visibilidad por una renovación ni conservarla tras
> cancelar**.

- [ ] Dado que mi suscripción se renueva, cuando Google Play lo informa, entonces mi
      vigencia se extiende sin abrir la aplicación.
- [ ] Dado que cancelo o Google reembolsa la compra, cuando lo informa, entonces mi
      vigencia termina cuando corresponde y no antes.
- [ ] Dado que el aviso de Google llega dos veces, cuando se procesa, entonces no
      duplica el período.

**Requisitos:** RF-14.4, RF-14.5, RF-14.7, INV-18.

**Tareas técnicas.** Las notificaciones en tiempo real de Google Play, por Cloud
Pub/Sub, hacia una función de servidor: el caso 3 que `supabase.md` reserva para la
recepción de notificaciones externas. Verificar que el costo siga dentro de RNF-10.

## Incremento del sprint

La aplicación está en Google Play y la plataforma cobra la suscripción del
profesional. **El modelo de negocio funciona de punta a punta.**

## Retrospectiva

_Completar al cerrar el sprint._

---

## Retrospectiva final

_Completar al cerrar el proyecto. Incluir la evolución de la velocidad por sprint,
que es evidencia directa del carácter incremental e iterativo del desarrollo._

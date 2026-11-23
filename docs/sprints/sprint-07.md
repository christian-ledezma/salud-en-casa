# Sprint 7 — Comunicación y ciclo del servicio

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-22 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.


**Objetivo del sprint.** Las partes se comunican, el profesional registra lo
realizado y la atención se completa.

**Objetivo específico.** 3 · Comunicación, coordinación y agenda.

**Puntos:** 56. Eran 23; el 2026-10-15 se sumó HU-37, que estaba fuera de alcance
como FA-11. Va al final del sprint: es la de menor prioridad y la primera que se
traslada si el corte muerde. El 2026-10-30 se sumó HT-25, con 8 puntos, que va
primero porque cambia la pantalla desde la que se llega a todo lo demás. El 2026-11-03
se sumó HT-26, con 3 puntos, que el recorrido de HU-21 reveló y que HU-22 y HU-23 necesitan, y HU-22
pasó de 3 a 5 al planificarse: la decisión del autor le agregó una migración, un modo de escritura y la
entrada del paciente. El 2026-11-04 se sumó HT-27, con 8 puntos, que el recorrido de HU-22 reveló. Con 47
puntos, HU-37 sigue siendo la primera que se traslada; HU-31 baja cuando se la replanifique. Ese mismo día HU-23
pasó de 5 a 8, al sumarle el rediseño de la atención, sus datos nuevos, el cierre de permisos y la lectura en vivo.
También ese día se sumó HT-28, con 3 puntos, a pedido del autor al revisar HT-27, y HT-29, con 3 puntos, a pedido del autor
al recorrer la negociación durante HU-24.

### HT-25 · La pantalla de inicio y la barra de navegación `[x]` — 8 puntos

> Como **paciente y como profesional**, quiero **una pantalla de inicio con la barra
> de navegación de una aplicación móvil**, para **llegar a lo que uso sin recorrer una
> lista de botones iguales**.

**Origen.** `AccountScreen` apilaba ocho botones del mismo peso y el rol activo decidía
cuáles. El autor pidió el 2026-10-29 una barra inferior y una pantalla de inicio con la
marca, y la diseñó con el agente en cinco vueltas de prototipo, la última aprobada el
2026-10-30. `FloatingNavigationBar`, del Sprint 0, existía sin que ninguna pantalla la
usara. El razonamiento y las decisiones están en `docs/decisions.md`, 2026-10-30, HT-25.

**Dónde queda cada cosa.** Ninguna pantalla que `AccountScreen` alcanzaba se queda sin
entrada, y dos ganan una.

| Hoy en `AccountScreen` | Dónde queda | Ruta |
|---|---|---|
| Atención ahora | Inicio del paciente, primera acción | `ImmediateCareRoute` |
| Agendar una cita | Inicio del paciente, segunda acción | `ProfessionalSearchRoute` |
| Mis solicitudes | Destino «Solicitudes» del paciente | `MyRequestsRoute`, como pestaña |
| Solicitudes de mi zona | Destino «Solicitudes» del profesional, y las tres primeras en su Inicio | `InboxRoute`, como pestaña |
| Mi disponibilidad | Ajustes del profesional, y el interruptor «Disponible ahora» en su Inicio | `AvailabilityRoute` |
| Mi verificación | Ajustes, con su estado para el profesional, y una fila en su Inicio mientras falte | `VerificationRoute` |
| Mis direcciones | Ajustes | `AddressListRoute` |
| Mi perfil | Cuenta | `ProfileRoute` |
| Revisar documentos | Inicio del administrador, sin barra | `DocumentReviewQueueRoute` |
| Cambiar de rol | Menú de las iniciales, desde los cuatro destinos | — |
| Activar el otro rol | Cuenta | — |
| Cerrar sesión | Menú de las iniciales y pie de Cuenta | — |
| Mis servicios, **gana entrada** | Ajustes del profesional; antes solo desde «Mi perfil» | `MyServicesRoute` |

**Criterios de aceptación**

- [x] Dado que ingreso con cualquier rol que no sea administrador, cuando se abre la
      aplicación, entonces veo Inicio con la marca arriba y una barra con Inicio,
      Ajustes, Solicitudes y Cuenta.
- [x] Dado que estoy en cualquier destino, cuando toco mis iniciales, entonces se abre un
      menú con mi nombre, los roles a los que puedo cambiar y «Cerrar sesión».
- [x] Dado que tengo un solo rol, cuando abro el menú, entonces no se me ofrece cambiar de
      rol, y Cuenta me ofrece activar el otro.
- [x] Dado que soy paciente, cuando abro Inicio, entonces encuentro primero «Atención
      ahora» y «Agendar cita», y debajo mi solicitud inmediata en curso si la tengo.
- [x] Dado que soy profesional, cuando abro Inicio, entonces puedo cambiar «Disponible
      ahora», veo las primeras solicitudes de mi zona y, si no estoy verificado, por qué
      no aparezco en las búsquedas.
- [x] Dado que vuelvo a Inicio o a Solicitudes desde otra pantalla, cuando la pantalla se
      reanuda, entonces muestra lo que cambió mientras no estaba.
- [x] Dado que soy administrador, cuando abro la aplicación, entonces llego a la revisión
      de documentos sin barra de navegación.
- [x] Al 200 % de fuente y a 320 dp, cada control se alcanza, se muestra y no se sale de
      los bordes, y el rótulo «Solicitudes» de la barra no termina en puntos suspensivos.

**Requisitos:** RF-01.8, RF-02.5, RF-07.4, RNF-07, RNF-08.

**Tareas**

- [x] `HomeShell`, con la cabecera, el menú de las iniciales, el cuerpo por destino y la
      barra; `HomeRoute` reemplaza a `AccountRoute`.
- [x] `BottomNavigationBar` en `ui/components`, y el retiro de `FloatingNavigationBar`.
- [x] Inicio, Ajustes y Cuenta en `features/home/presentation`, con
      `PatientHomeViewModel` y `ProfessionalHomeViewModel`; el retiro de `AccountScreen`.
- [x] `FloatingCircles` y `StaggeredEntrance` en `ui/animations`.
- [x] «Mis solicitudes» y la bandeja como pestañas: cabecera opcional y lectura al
      reanudarse. `AccountViewModel.reloadRoles`.
- [x] Cadenas en `values/` y `values-es/`; las trece que quedaron sin uso, retiradas.
- [x] Pruebas: `HomeShellLargeFontTest` (doce), `ProfessionalHomeViewModelTest`,
      `PatientHomeViewModelTest`, y las nuevas de `MyRequestsViewModelTest` y
      `AccountViewModelTest`. `AccountScreenLargeFontTest` se retira con su pantalla.
- [x] `./gradlew build` en verde.
- [x] Mirar las previsualizaciones de `HomeShell.kt` en claro, oscuro y al 200 %.
- [x] Recorrido del autor en emulador con los dos roles y el de administrador.

**Pruebas obligatorias**

- Prueba de interfaz al 200 % y 320 dp para la pantalla nueva:
  `HomeShellLargeFontTest`. El rótulo largo de la barra se prueba leyendo su
  `TextLayoutResult`, que es la única forma de ver un recorte dentro de su propio
  contenedor sin mirar.
- `switchFailureKeepsThePreviousActiveRole` y
  `theSwitchIsHiddenForSomeoneWithASingleRole` siguen valiendo: el modelo de vista del
  cambio de rol es el mismo; la segunda tiene ahora su par de interfaz,
  `someoneWithOneRoleIsNotOfferedASwitch`.

**Deuda que salda.** La de HU-01: el botón «Reintentar» del cierre de sesión fallido no
tenía prueba de interfaz. La tiene, `retryingAFailedSignOutAsksForItAgain`.

### HU-21 · Conversar sobre la atención `[x]` — 10 puntos

> Como **cualquiera de las partes**, quiero **intercambiar mensajes dentro de la
> solicitud**, para **coordinar los detalles sin salir de la aplicación**.

**Criterios de aceptación**

- [x] Dado que la oferta fue aceptada, cuando abro la atención, entonces puedo
      enviar y recibir mensajes.
- [x] Dado que la contraparte envía un mensaje, cuando estoy en la conversación,
      entonces lo veo aparecer sin recargar.
- [x] Dado que leo un mensaje, cuando la contraparte consulta, entonces ve que fue leído.
- [x] Dado que no estoy en la aplicación, cuando recibo un mensaje, entonces me
      llega una notificación con el nombre de quien escribe y **sin el contenido
      del mensaje**, que me lleva a la conversación.
- [x] Dado que tengo la conversación abierta, cuando llega un mensaje, entonces no
      me llega además la notificación.
- [x] Dado que soy administrador, cuando intento leer una conversación, entonces
      el acceso se deniega.
- [x] La conversación se carga paginada, del mensaje más reciente hacia atrás.

**Requisitos:** RF-09.1, RF-09.2, RF-09.6, RF-09.10, RN-11, INV-12, RNF-08.

> **Por qué la notificación no lleva el mensaje.** La conversación guarda el
> detalle de la atención (RF-09.3), que es información de salud. Un aviso pasa por
> los servidores de Google y se lee en la pantalla bloqueada de un teléfono que
> puede ser compartido. Ver `docs/decisions.md`, 2026-10-15, «Notificaciones push».

**Planificación (2026-11-03).** Decidida con el autor y registrada en `docs/decisions.md`,
2026-11-03, HU-21. Prototipo aprobado el mismo día, en el lienzo «Conversación HU-21»:
claro, oscuro, 200 % a 320 dp, la entrada desde la atención con los dos avisos, y los
estados cargando, vacío, error, cancelada y envío fallido.

- La conversación es una **pantalla propia**, que se abre con «Abrir conversación» desde
  la atención. El aviso abre la conversación con la atención debajo.
- **Se escribe mientras la atención no esté cancelada.** Cancelada, queda solo para leer;
  completada, sigue abierta para coordinar el pago. Lo exige la política del motor.
- **Con el teléfono bloqueado el aviso no muestra el nombre**; desbloqueado, «<nombre> te
  escribió · <tipo de servicio>». Nunca el contenido.
- **Paginación por cursor `(created_at, id)`**, no por desplazamiento; el motor sella
  `created_at` y `read_at`.
- **El aviso no se muestra con la conversación abierta**: lo descarta el teléfono.

**Tareas**

- [x] Migración `20261103183624_conversation`: índice por cursor, `conversation_page`, sellos
      de `created_at` y `read_at`, inserción cerrada en atención cancelada,
      `mark_conversation_read`, señales `notify_conversation_message` y
      `notify_conversation_read`, política `conversation_listen`, `request_push_body` reescrita
      con `MESSAGE_RECEIVED` y `queue_message_push`.
- [x] Experimentos SQL contra el remoto, antes del `db push` (abajo).
- [x] `features/conversation`: dominio, datos, modelo de vista y pantalla.
- [x] Ayudante de canal privado en `core/network`, usado también por las ofertas y la bandeja.
- [x] «Abrir conversación» en la atención y `ConversationRoute`.
- [x] Notificaciones: `MESSAGE_RECEIVED`, canal «Mensajes», destino `Conversation`,
      descarte con la conversación abierta, espacio de aviso propio; `senderName` en
      `send-push`.
- [x] Cadenas en `values/` y `values-es/`, glosario.
- [x] Pruebas: `MessageContentTest`, `MessageTest`, `CatchUpMessagesUseCaseTest`,
      `SendMessageUseCaseTest`, `GetConversationHeaderUseCaseTest`, `ConversationRowsTest`,
      `ConversationViewModelTest`, `ConversationScreenLargeFontTest`, y las nuevas de
      `ResolveIncomingNotificationUseCaseTest`, `PushPayloadTest`, `NotificationPublisherTest`,
      `OpenNotificationUseCaseTest`, `NotificationTapEffectTest` y `AttentionScreenLargeFontTest`.
      Las dos más delicadas se comprobaron por mutación. Previsualizaciones en claro, oscuro, 200 %,
      vacía, cerrada, no enviada y con error; `ScreenCatalog` gana la conversación y la atención.
- [x] Mirar las previsualizaciones de `ConversationScreen.kt` y `AttentionScreen.kt`: generadas con
      `ScreenCatalog` y revisadas el 2026-11-03 (claro, oscuro, 200 % a 320 dp, vacía, cancelada,
      no enviada, con error, y la atención al 200 % con «Abrir conversación»). Sin recortes.
- [x] `./gradlew build` en verde el 2026-11-03: 913 pruebas, ninguna omitida, análisis estático y
      lint incluidos.
- [x] Hecho por el autor el 2026-11-03: `supabase db push` y el redespliegue de `send-push` (22:48).

**Confirmado por el autor (2026-11-03).** Recorrió el ejercicio con los dos lados, en el Nubia y en
el emulador, sin observaciones sobre la conversación. El remoto lo respalda sin leer ningún contenido:
una conversación con 3 mensajes de 2 personas, los 3 leídos, y 6 envíos de `send-push` respondidos con
200, cada uno con 1 enviado y 0 fallidos. La paginación y el descarte del aviso con la conversación
abierta se verificaron además con pruebas, porque 3 mensajes no alcanzan para llenar una página.
**Cerrada el 2026-11-03.** No es una historia crítica, así que no exigía recorrido.

### HT-26 · Mis atenciones: el profesional vuelve a lo que acordó `[x]` — 3 puntos

> Como **profesional**, quiero **encontrar las atenciones que acordé**, para **volver a
> ellas y a su conversación cuando lo necesite, y no solo en el momento de aceptar**.

**Origen.** El autor lo encontró el 2026-11-03 al recorrer HU-21. Una solicitud aceptada sale
de la bandeja, y el profesional solo llegaba a la atención al aceptar desde el hilo o tocando el
aviso: si volvía atrás o descartaba el aviso, no tenía cómo regresar. Su lista estaba prevista
recién en HU-32, en el Sprint 9, y sin ella tampoco funcionarían HU-22 ni HU-23. El paciente no
tiene el problema: su tarjeta de «Mis solicitudes» abre la atención. Es la misma clase de hueco que
«El hueco del 09/10»: un camino que nadie había caminado. Registrado en `docs/decisions.md`,
2026-11-03, HT-26.

**Criterios de aceptación**

- [x] Dado que soy profesional, cuando abro «Solicitudes», entonces elijo entre «Disponibles», que
      es la bandeja, y «Mis atenciones».
- [x] Dado que acordé una atención, cuando abro «Mis atenciones», entonces la veo con el servicio,
      su estado, el paciente, el monto y, si es agendada, la hora de la cita.
- [x] Dado que toco una atención, cuando se abre, entonces llego a su pantalla y desde ahí a la
      conversación, también si está completada o cancelada.
- [x] Dado que tengo los dos roles, cuando abro «Mis atenciones», entonces solo veo las que atiendo
      como profesional.
- [x] La lista está paginada, de la más reciente a la más antigua.
- [x] Al 200 % de fuente y a 320 dp, los rótulos del control caben en una línea y cada control se
      alcanza.

**Requisitos:** RF-13.4, RF-09.1, RNF-08, INV-13, INV-17.

**Tareas**

- [x] Migración `20261104022210_professional_attentions`: `my_attentions_as_professional`.
- [x] `AttentionSummary`, `GetMyAttentionsUseCase`, `MyAttentionsViewModel` y `MyAttentionsScreen` en
      `features/services`; `AttentionMapper` lee la fecha con `OffsetDateTime`.
- [x] `ProfessionalRequestsTab` en el shell, con el control de dos vistas.
- [x] `SegmentedControl`: el rótulo se achica hasta caber, con el piso de HT-25.
- [x] Pruebas: `GetMyAttentionsUseCaseTest`, `MyAttentionsViewModelTest`,
      `MyAttentionsScreenLargeFontTest`, `ProfessionalRequestsContentLargeFontTest` y las nuevas de
      `AttentionMapperTest`. La del rótulo se comprobó por mutación: sin el achique falla con
      «Disponibles ends in an ellipsis». Previsualizaciones generadas y mirando las imágenes.
- [x] `./gradlew build` en verde el 2026-11-03: 935 pruebas, ninguna omitida.
- [x] Hecho por el autor el 2026-11-03: `supabase db push` y el recorrido en los dos lados.

**Experimentos SQL (2026-11-03, remoto, `begin … rollback`, sobre la migración sin aplicar).** El
profesional `chris.ledezma.s` lee sus 4 atenciones, de la más reciente a la más antigua, con el nombre
de la paciente, y las páginas no se repiten. Una atención cancelada sigue en la lista con su estado. La
paciente de esos mismos servicios, una persona ajena y el administrador leen 0: la función filtra por
`professional_id`, no por ser parte. `anon` no puede ejecutarla.

**Confirmado por el autor (2026-11-03).** Recorrió el camino en los dos lados. **Cerrada el
2026-11-03.**

**El cierre de la aplicación en el Nubia, 22:50.** El autor observó que la aplicación se cerró al entrar
por primera vez a la atención. **No fue un fallo de la aplicación**, según lo que el sistema registró.
De 16 salidas guardadas en `exit-info`, ninguna es `CRASH`, `CRASH_NATIVE` ni `ANR`, y el búfer de fallos
está vacío. El proceso abierto a las 22:49:53 terminó a las 22:50:09 con motivo `PACKAGE UPDATED` y en
primer plano, sin pasar por `PAUSED`; la misma versión llegó al emulador a las 22:50:14, y la aplicación
se volvió a abrir sola a las 22:50:15 y siguió sin cierres. Es lo que deja una instalación lanzada con la
aplicación en uso. La causa se dedujo de esos registros y no se reprodujo: el log general del teléfono
tiene un búfer de 256 KiB y terminaba a las 22:20. Si vuelve a ocurrir fuera de una instalación, el
historial de salidas lo distinguirá.

**Relación con HU-32.** HU-32 agrega el ingreso, la comisión, el resumen y el filtro de fechas sobre
esta misma lista; ya no tiene que construirla.

### HU-22 · Registrar lo realizado en la atención `[x]` — 5 puntos

> Como **profesional**, quiero **dejar constancia de lo que hice durante la
> atención**, para **que el paciente y yo tengamos el registro**.

**Criterios de aceptación**

- [x] Dado que la atención está en curso, cuando registro lo realizado, entonces
      queda en la conversación de la solicitud.
- [x] Dado que la atención finalizó, cuando consulto el historial, entonces accedo
      a esa conversación.
- [x] Dado que soy estudiante, cuando registro, entonces puedo describir lo
      realizado pero la aplicación no ofrece emitir diagnóstico.

**Requisitos:** RF-09.3, RN-03, FA-01.

> **Nota de alcance.** El registro clínico estructurado está fuera de alcance
> (FA-01). Esta historia deja el detalle en la conversación, de modo deliberado.
> No crear campos improvisados en otras tablas para este propósito: eso
> comprometería la incorporación aditiva del registro clínico en una fase posterior.

**Planificación (2026-11-03).** Decidida con el autor y registrada en `docs/decisions.md`, 2026-11-03,
HU-22. Prototipo aprobado el mismo día, en el lienzo «Registro HU-22»: claro, oscuro, 200 % a 320 dp, el
estudiante, el paciente, la atención asignada y el registro no enviado. Subió de 3 a 5 puntos.

- **Un mensaje marcado**, `messages.is_work_record`, que se dibuja como tarjeta. Solo lo pone el
  profesional de esa atención; lo exige el motor. El texto sigue en `content`, con su autor y su fecha.
- **Con la atención en curso o completada.** Antes de llegar se rechaza; un mensaje común conserva la regla
  de HU-21.
- **El estudiante ve un aviso**, que informa y no impide. Nadie tiene campo de diagnóstico (FA-01).
- **Corregir es agregar otro registro.** Lo avisa la barra.
- **La tarjeta del paciente abre la atención también en curso y completada.** Cancelada queda para HU-24.
- **Va antes que HU-23.** Se recorre poniendo a mano una atención de prueba en curso.

**Tareas**

- [x] Migración `20261104034405_work_record_messages`: la columna y las dos políticas de inserción.
- [x] Experimentos SQL contra el remoto, antes del `db push` (abajo).
- [x] Conversación: la marca en dominio, datos y modelo de vista; `canRecord` y `showsStudentNotice` en la
      cabecera; `MessageError.WorkRecordNotAllowed`.
- [x] Pantalla: la tarjeta, el control, los avisos y el campo en modo registro. La barra se desplaza dentro de
      su alto, para que guardar se alcance con fuente grande y el teclado abierto.
- [x] `negotiationEntry`: la atención también en `IN_PROGRESS` y `COMPLETED`.
- [x] Cadenas en `values/` y `values-es/`, `ic_work_record`, glosario y sistema de diseño.
- [x] Pruebas: `MessageMapperTest`, y las nuevas de `GetConversationHeaderUseCaseTest`,
      `SendMessageUseCaseTest`, `ConversationViewModelTest`, `ConversationScreenLargeFontTest` y
      `NegotiationEntryTest`. Tres se comprobaron por mutación: `aRetriedRecordIsStillARecord`,
      `aRefusedRecordAsksAgainWhatTheAttentionAllows` y `savingARecordStaysReachableWithTheKeyboardUp`.
- [x] Mirar las previsualizaciones: generadas con `ScreenCatalog` y revisadas el 2026-11-03 (claro, oscuro,
      estudiante, paciente, no permitido y 200 %, también a 320 dp). Sin recortes.
- [x] `./gradlew build` en verde el 2026-11-03: 963 pruebas, ninguna omitida, análisis estático y lint incluidos.
- [x] Hecho por el autor el 2026-11-04: `supabase db push` y el recorrido en los dos lados.

**Experimentos SQL (2026-11-03, remoto, dentro de una transacción revertida, sobre la migración sin aplicar).**
Cada caso se ejecutó con la identidad indicada, y los estados se pusieron como dueño dentro de la misma
transacción. Después se confirmó que no quedó residuo: la columna no existe y los 4 servicios siguen `ASSIGNED`.

| Caso | Resultado |
|---|---|
| El profesional registra en curso y en completada | Pasa |
| El profesional registra en asignada y en cancelada | Rechazado por la política |
| El profesional escribe un mensaje común en asignada | Pasa, de modo que la regla discrimina |
| El profesional escribe un mensaje común en cancelada | Rechazado, como en HU-21 |
| El paciente registra, también dándole el rol profesional, y haciéndose pasar por el profesional | Rechazado |
| El paciente escribe un mensaje común en curso | Pasa |
| Un profesional ajeno, y uno ajeno con los dos roles, registran | Rechazado |
| El paciente marca un mensaje común, desmarca un registro o edita su contenido; el profesional marca el suyo | `permission denied` |
| El profesional borra su registro | 0 filas |
| La marca explícita en nulo | Rechazada por `not null` |
| `conversation_page` devuelve la marca a las dos partes, sin reescribirse | Pasa |
| `mark_conversation_read` marca leído un registro | Pasa |
| El administrador y un ajeno leen la conversación | 0 filas |
| El cuerpo del aviso de un registro | `MESSAGE_RECEIVED` con el nombre, sin el contenido |
| Los mensajes existentes | Ninguno queda marcado |

**Confirmado por el autor (2026-11-04).** Recorrió el ejercicio en los dos lados. El remoto lo respalda sin
leer ningún contenido: la migración está aplicada, y la atención de prueba tiene un registro escrito por su
profesional y leído por el paciente. **Cerrada el 2026-11-04.** No es una historia crítica.

Dos cosas no salieron del recorrido. Registrar con la atención completada y el caso del estudiante se apoyan en
los experimentos, las pruebas y las previsualizaciones: en el remoto no hay ninguna cuenta de estudiante, y esa
atención quedó con la solicitud en `COMPLETED` y el servicio en `IN_PROGRESS`, porque ambos estados se movieron
a mano. HU-23 es la que los mueve juntos.

### HU-23 · Marcar el avance de la atención `[x]` — 8 puntos

> Como **profesional**, quiero **indicar que llegué y que terminé**, para **que el
> paciente conozca el estado y se habilite el pago**.

**Criterios de aceptación**

- [x] Dado que la atención está asignada, cuando marco mi llegada, entonces pasa a
      en curso y el paciente lo ve.
- [x] Dado que la atención está en curso, cuando la marco completada, entonces se
      habilita la confirmación de pago.
- [x] Dado que intento marcar llegada en una atención cancelada, cuando lo intento,
      entonces se rechaza.
- [x] Dado que la atención se completó, cuando consulto sus tiempos, entonces
      figuran el inicio y el fin.
- [x] Dado que el profesional marca su llegada, cuando ocurre, entonces al
      paciente le llega una notificación.
- [x] Dado que el profesional marca la atención completada, cuando ocurre,
      entonces al paciente le llega una notificación que lo lleva a confirmar el
      pago. El aviso abre la atención; la acción de pagar la pone ahí HU-25.

**Requisitos:** RF-10.1, RF-10.2, RF-10.4, RF-09.6, RF-09.10.

**Tareas técnicas.** Máquina de estados modelada como jerarquía sellada · prueba
de las transiciones válidas e inválidas.

**Planificación (2026-11-04).** Decidida con el autor y registrada en `docs/decisions.md`, 2026-11-04, HU-23.
Prototipo aprobado en su segunda vuelta, en el lienzo «Avance HU-23», sin el degradado de la cabecera. Subió de 5 a 8.

- **Se marca en la atención**, con confirmación en las dos marcas y sin regla de horario para una cita.
- **El motor sella las fechas y mueve la solicitud** dentro de la misma transacción. Los clientes solo escriben
  `status` y las columnas de cancelación.
- **El paciente lo ve al instante**, con una señal sin datos en un tema privado propio, y le llega un aviso en el canal
  «Atenciones».
- **La atención dice a dónde ir:** dirección, referencia, «Cómo llegar», hora de la cita y lo pedido. El profesional
  ve la dirección exacta solo con la atención asignada o en curso.
- **El aviso de atención completada abre la atención.** Lo que falta del último criterio es la acción de pagar, que
  HU-25 pone en esa misma pantalla.

**Tareas**

- [x] Migración `20261104053310_service_progress`: permisos de columna, `stamp_service_progress`,
      `guard_request_lifecycle` reescrita, `move_request_with_service`, `notify_attention_changed`, `attention_listen`, `request_push_body`
      reescrita con `PROFESSIONAL_ARRIVED` y `SERVICE_COMPLETED`, `queue_service_progress_push` y `my_attention` con
      los datos nuevos.
- [x] Experimentos SQL contra el remoto, antes del `db push` (abajo).
- [x] Dominio: `AttentionStep`, `nextStep`, `AttentionPlace`, `AdvanceAttentionUseCase` y `ObserveAttentionUseCase`.
- [x] Pantalla de la atención rediseñada, con la línea de tiempo, el botón y los diálogos; lectura en vivo y al volver.
- [x] Notificaciones: los dos eventos, su destino y el canal «Atenciones».
- [x] Cadenas en `values/` y `values-es/`, `ic_payments`, `ic_near_me`, glosario y sistema de diseño.
- [x] Pruebas: `AttentionTest` (`serviceStateTransitionsRejectInvalidJumps` y `cannotMarkArrivalOnCancelledService`, del
      lado del teléfono), `AttentionTimelineTest`, las nuevas de `AttentionViewModelTest`, `AttentionMapperTest`,
      `PushPayloadTest`, `OpenNotificationUseCaseTest` y `NotificationContentTest`, y
      `AttentionScreenLargeFontTest` reescrita. Se comprobaron por mutación: que el paciente no tenga paso, el orden de
      latitud y longitud, la segunda confirmación y la señal.
- [x] Mirar las previsualizaciones: generadas con `ScreenCatalog` y revisadas el 2026-11-04 (claro, oscuro, paciente y
      200 % a 320 dp). El diálogo no se puede capturar, porque abre otra ventana: lo cubre `theDialogKeepsBothAnswersOnScreen`.
- [x] `./gradlew build` en verde el 2026-11-04: 1000 pruebas, ninguna omitida, análisis estático y lint incluidos.
- [x] Hecho por el autor el 2026-11-04: `supabase db push` y el recorrido en los dos lados, en los emuladores.

**Experimentos SQL (2026-11-04, remoto, dentro de una transacción revertida, sobre la migración sin aplicar).**

| Caso | Resultado |
|---|---|
| El paciente marca llegada o fin | `arrival_and_completion_are_recorded_by_the_professional` |
| Un profesional ajeno marca llegada | 0 filas |
| Saltar de asignada a completada, y volver a asignada | `invalid_service_transition` |
| Marcar llegada en una atención completada o cancelada (`cannotMarkArrivalOnCancelledService`) | `service_status_is_terminal` |
| Cualquiera de las partes escribe una fecha o el monto | `permission denied` |
| El paciente mueve su solicitud a en curso | `request_status_change_requires_the_atomic_operation` |
| El profesional marca llegada | Sella el inicio, la solicitud pasa a `IN_PROGRESS`, 1 aviso y 1 señal |
| Marca llegada otra vez | Pasa, sin aviso ni señal nuevos |
| El profesional marca el fin | Sella el fin, la solicitud pasa a `COMPLETED`, 1 aviso, las atenciones realizadas suben 1 |
| Marca el fin otra vez | Pasa, sin contar otra atención |
| Cancelar, el camino de HU-24 | Sigue pasando con los permisos nuevos |
| `my_attention` del profesional, en curso | Con dirección y punto |
| `my_attention` del profesional, completada o cancelada | Sin dirección, referencia ni punto; con la ciudad |
| `my_attention` del paciente, completada | Con su dirección, inicio y fin |
| `my_attention` de un ajeno y del administrador | 0 filas |
| Cuerpo de los dos avisos | Solo para el paciente, con el nombre del profesional y el tipo de servicio |
| `anon` | No puede actualizar `services` |
| Escuchar el tema de la atención | Las dos partes reciben la señal; un ajeno y el administrador, no |

**Confirmado por el autor (2026-11-04).** Recorrió el ejercicio en los emuladores. El remoto lo respalda sin leer ningún
contenido: la migración está aplicada, y una atención quedó completada con su inicio y su fin en orden y la solicitud en
`COMPLETED`. `send-push` respondió 2 veces con 200, cada una con 1 enviado y 0 fallidos. Marcar en una atención cancelada
y la señal entre dos partes se apoyan en los experimentos. **Cerrada el 2026-11-04.** No es una historia crítica.

**A tener en cuenta en HU-24.** Cancelar todavía no mueve la solicitud, y las columnas de cancelación se pueden escribir
sin cambiar el estado. Le corresponde cerrarlo a esa historia.

### HT-27 · Las listas se actualizan y las tarjetas dicen lo mismo a los dos lados `[x]` — 8 puntos

> Como **paciente y como profesional**, quiero **actualizar mis listas deslizando y ver en cada tarjeta la
> fecha y el precio acordado**, para **encontrar mis atenciones y confiar en lo que dicen**.

**Origen.** Al recorrer HU-22 el 2026-11-04, el autor no encontró en «Mis solicitudes» la atención que el
aviso sí le abría: la tarjeta decía «Aceptada», tenía otras dos iguales, no mostraba fecha y quedaba debajo de
solicitudes canceladas. Pidió actualizar deslizando, más información en las tarjetas y coherencia entre los dos
lados, y aprobó el alcance y el momento el mismo día.

**Por qué después de HU-23 y antes de HU-24.** HU-23 crea las fechas reales de inicio y fin, y mueve juntos los
dos estados, de modo que la tarjeta tiene algo cierto que decir. HU-24 tiene que abrir atenciones canceladas
desde la lista del paciente, y en «Mis atenciones» todas tienen servicio: una cita cancelada antes de aceptarse
no llega ahí.

**Alcance acordado, a precisar al planificarla, con prototipo antes de programar**

- Deslizar hacia abajo actualiza, con su animación de carga, «Mis solicitudes», la bandeja y «Mis atenciones».
  **Se vuelven a leer todos los datos de cada tarjeta**, no solo el estado. La recarga al volver a la pantalla
  se conserva; una lectura fallida deja la lista como estaba.
- El paciente gana las mismas dos vistas que el profesional: «Solicitudes» y «Mis atenciones», con
  `my_attentions_as_patient`. Así una atención activa ya no queda entre solicitudes canceladas, sin agregar un
  orden: el historial conserva el de RF-13.4.
- Un mismo diseño de tarjeta a los dos lados: servicio, estado, contraparte, fecha y monto. La bandeja sigue sin
  mostrar la identidad del paciente, porque todavía no hay acuerdo.
- La fecha es la hora de la cita si es agendada y, si no, la de inicio; antes de iniciar, la de creación.
- El precio acordado reemplaza al sugerido una vez aceptada. Antes de aceptarse sigue el presupuesto, rotulado.
- Dos detalles de HU-19: el monto que se lee «Bs150» y las iniciales que tocan el borde del círculo al 200 %.

**Fuera de esta historia.** Buscar y elegir el orden: FA-16.

**Relación con HU-31.** HU-31 agrega la calificación sobre esta lista; ya no tiene que construirla.

**Planificación (2026-11-04).** Decidida con el autor y registrada en `docs/decisions.md`, 2026-11-04, HT-27. Prototipo
aprobado el mismo día, en el lienzo «Listas HT-27»: las listas de los dos roles, Inicio, deslizar, el fallo, el vacío, oscuro
y 200 % a 320 dp. Se mantuvo en 8 puntos.

- **Una tarjeta aceptada pasa a «Mis atenciones» y sale de «Mis solicitudes».**
- **Inicio gana «Tu atención» en los dos roles**: la que está en curso o la próxima asignada. No estaba en el alcance.
- **«Mis solicitudes» muestra primero lo abierto.** Corrige la línea de arriba, que decía no agregar un orden.
- **Un fallo al deslizar se dice**, con un aviso arriba de la lista; al volver a la pantalla sigue siendo silencioso.
- **`my_attentions`, con un parámetro de rol, reemplaza a `my_attentions_as_patient`** de la línea de arriba.
- **La sección «En curso» de Inicio se llama «Tu solicitud»**, porque esa palabra ya nombra el estado de una atención.

**Tareas**

- [x] Migración `20261104135622_ht27_lists`: `has_attention` e `is_open` en `my_service_requests`, `my_attentions` y
      `my_active_attention`.
- [x] Experimentos SQL contra el remoto, antes del `db push` (abajo).
- [x] `ServiceCard`, `StatusBadge` y `RefreshableContent`; `ListRefresher` en los tres modelos de vista.
- [x] Las dos vistas del paciente, `RequestsTabContent`, y la fila «Tu atención» en los dos Inicios.
- [x] «Bs 150» con espacio que no se parte, e iniciales que no crecen con la fuente.
- [x] Cadenas en `values/` y `values-es/`, glosario y sistema de diseño. Se retiraron `ModalityBadge` y
      `NegotiationEntry.Attention`, que quedaron sin uso.
- [x] Pruebas: `ListRefresherTest`, `AttentionSummaryTest`, `InitialsTextTest`, `RequestsTabContentLargeFontTest`, y las
      nuevas de los modelos de vista, las pantallas, `AttentionMapperTest` y `MoneyFormatTest`.
      `anEmptyListCanBePulled` se comprobó por mutación: sin el desplazamiento de la columna, falla.
- [x] Revisión de código: encontró que una página en camino caía sobre la lista recién leída de la bandeja. Corregido, con
      `aPageStillOnItsWayDoesNotLandOnTheListAPullReplaced`.
- [x] Mirar las previsualizaciones: generadas con `ScreenCatalog` y revisadas el 2026-11-04 (claro, oscuro, 200 % y vacío).
      Sin recortes.
- [x] Hecho por el autor el 2026-11-04: `supabase db push` y el recorrido en los dos lados.

**Experimentos SQL (2026-11-04, remoto, dentro de una transacción revertida, sobre la migración sin aplicar).** Cada caso
se ejecutó con la identidad indicada. Después se confirmó que no quedó residuo: las funciones nuevas no existen, la anterior
sigue y los 2 servicios siguen `ASSIGNED`.

| Caso | Resultado |
|---|---|
| El paciente y el profesional leen sus 4 atenciones, cada uno con el nombre del otro | Pasa |
| Cada uno pide el lado que no tiene | 0 filas |
| Un ajeno en los dos lados, el administrador con `ADMIN` y un rol nulo | 0 filas |
| `anon` ejecuta `my_attentions` o `my_active_attention` | Sin permiso |
| Activa: sin fecha, la acordada primero; con citas, la más próxima | Pasa |
| Activa: la que está en curso gana a una cita más próxima | Pasa |
| Activa: completada y cancelada no cuentan; un ajeno y el administrador | Ninguna |
| La vista marca las 4 filas con servicio de las 27 del paciente | Coincide con la tabla |
| Una fila antigua reabierta va primero | Pasa |
| Una atención cancelada, con su solicitud cancelada, no está en «Mis solicitudes» y sí en «Mis atenciones» | Pasa |
| Escribir por la vista, también la columna nueva | Rechazado |
| Un ajeno lee las filas del paciente en la vista | 0 filas |

**Confirmado por el autor (2026-11-04).** Aplicó la migración e hizo las pruebas. El remoto lo respalda sin leer ningún
contenido: la migración está registrada, las dos funciones y las dos columnas existen, y `my_attentions_as_professional` ya
no está. Hay una atención nueva en curso, como pedía el recorrido, y `61eb5448` y `54b5c052` siguen `ASSIGNED` para HU-24.
**Cerrada el 2026-11-04.** No es una historia crítica.

**A tener en cuenta.** `05adae36` sigue con la solicitud en `COMPLETED` y el servicio en `IN_PROGRESS`, de modo que ocupa
«Tu atención» de esa pareja. Son datos de prueba; desde HU-23 los dos estados se mueven juntos.

### HT-28 · Aceptar una oferta desde donde se comparan `[x]` — 3 puntos

> Como **paciente**, quiero **aceptar una oferta inmediata desde la pantalla donde veo todas**, para **no abrir otra
> pantalla solo para decir que sí**.

**Origen.** Al revisar HT-27 el 2026-11-04, el autor sintió que aceptar una oferta seguía costando demasiados pasos:
tarjeta, oferta, «Aceptar» y confirmar. Pidió evaluar «Aceptar» en la lista y fusionar la pantalla intermedia. Aprobó esta
recomendación el mismo día.

**Por qué no en la lista ni sin pantalla intermedia.** Una inmediata puede tener varias ofertas, y la tarjeta de la lista
representa la solicitud, de modo que «Aceptar» no diría a quién ni por cuánto. Aceptar crea la atención y el pago pendiente
con el monto congelado, así que se hace viendo al profesional. La pantalla intermedia sigue siendo donde las ofertas llegan
en vivo y se comparan.

**Alcance acordado, a precisar al planificarla, con prototipo antes de programar**

- «Aceptar» con su monto en cada oferta, cuando le toca responder al paciente, con la confirmación actual. Tocar el resto de
  la tarjeta abre la negociación, que queda para contraofertar, rechazar o leer los mensajes.
- Se quita «Ver mis solicitudes»: la flecha de volver ya cumple esa función.
- El título dice «Buscando profesional» solo sin ofertas, y «Ofertas recibidas» después.
- Las ofertas van arriba, con el resumen de la solicitud más abajo y más corto.
- La tarjeta de «Mis solicitudes» muestra cuántas ofertas tiene.

**Resultado esperado.** De 4 a 3 toques para aceptar una inmediata, sin cambiar de pantalla antes de «Aceptar». La
confirmación se conserva. Las citas agendadas no cambian: ya abren directo su negociación.

**Requisitos:** RF-08.5.

**Antes:** HT-27 cerrada, porque esta historia parte de su tarjeta.

**Planificación (2026-11-04).** Registrada en `docs/decisions.md`, 2026-11-04, HT-28. Prototipo aprobado el mismo día, en el
lienzo «Ofertas HT-28»: la lista, las ofertas, la confirmación, sin ofertas, aceptar fallido, oscuro y 200 % a 320 dp. Se
mantuvo en 3 puntos. Respecto del alcance de arriba, el prototipo sumó «Negociar» junto a «Aceptar», para que contraofertar
se vea sin adivinar, y «Volver» en el diálogo, para no confundirlo con cancelar la solicitud.

**Criterios de aceptación**

- [x] Dado que me toca responder una oferta, cuando abro «Ofertas recibidas», entonces la tarjeta ofrece «Aceptar» con su
      monto y «Negociar».
- [x] Dado que acepto, cuando se abre la confirmación, entonces nombra el monto y al profesional; al confirmar voy a la
      atención, y volver no regresa a las ofertas.
- [x] Dado que el diálogo está abierto y llega otra oferta, cuando confirmo, entonces se acepta la oferta que el diálogo
      nombra.
- [x] Dado que la oferta cambió antes de confirmar, cuando el motor rechaza, entonces la pantalla lo dice y muestra la oferta
      vigente.
- [x] Dado que espero la respuesta del profesional, cuando veo su tarjeta, entonces solo puedo abrir la negociación.
- [x] Dado que no hay ofertas, cuando abro la pantalla, entonces dice «Buscando profesional», y no lo dice mientras la primera
      lectura no responde.
- [x] Dado que una inmediata abierta tiene ofertas, cuando veo «Mis solicitudes», entonces la tarjeta dice cuántas.

**Experimento contra el remoto (2026-11-04), con la migración aplicada dentro de una transacción con `rollback`**

| Caso | Resultado |
|---|---|
| Sin ofertas | 0 |
| Una oferta sin responder | 1 |
| Dos profesionales | 2 |
| Una contraoferta sobre una de ellas | 2: es la misma negociación |
| Un tercer profesional | 3 |
| Se rechaza su oferta | 2: sale de la cuenta, como de la pantalla |
| Un ajeno y el profesional leen la fila | 0 filas |
| Escribir por la vista, también el contador | Rechazado, `55000` |
| `anon` lee, `authenticated` actualiza | No y no |
| El paciente acepta una oferta | Pasa, y la solicitud queda `ACCEPTED`, con `has_attention` |
| Otro profesional acepta la que le quedaba abierta | Rechazado, `request_not_open` |
| La oferta de ese otro profesional | Sigue `PROPOSED`, sin poder aceptarse |

La última fila es la razón de `showsOfferCount`: el motor seguiría contando esa oferta, y es el modelo el que limita la cuenta a
las inmediatas abiertas.

**Mutaciones comprobadas (2026-11-04).** Las cinco hacen fallar exactamente una prueba de `ImmediateCareViewModelTest`: aceptar
la oferta de la lista en vez de la del diálogo, no tratar `Accepted` como ocupado, no cancelar la lectura en curso al aceptar,
no comprobar el turno, y no marcar la primera lectura como pendiente.

**Previsualizaciones (2026-11-04).** Abiertas y miradas: ofertas en claro, oscuro y 200 %, sin ofertas, aceptar fallido y la
lista con «2 ofertas». **El diálogo no se ve en ellas:** es una ventana aparte, que `ScreenCatalog` no captura. Lo cubre la
prueba a 320 dp y 200 %, que no ve el texto recortado dentro de su contenedor.

**Cerrada el 2026-11-04, con recorrido del autor en emuladores.** El autor aplicó la migración y luego hizo el ejercicio contra el
remoto, sin informar defectos. No figura en la tabla de historias críticas, de modo que el recorrido no era obligatorio, aunque
usa la misma aceptación que HU-19, que sí lo es.

**Lo que respalda el remoto, sin leer ningún contenido.** La migración está registrada y es la última, la columna existe, `anon`
no lee la vista y `authenticated` no la actualiza. La solicitud inmediata `b5a319f2` nació a las 18:15 UTC y llegó al acuerdo a las
18:21, con un profesional y tres ofertas: la suya, la contraoferta del paciente y otra suya, que el paciente aceptó. Quedó con un
servicio `ASSIGNED`, un pago y el monto final igual al de la oferta aceptada.

**Lo que solo respalda el recorrido.** Que el texto del diálogo no quede recortado, que tras aceptar se llegue a la atención y que
volver no regrese a las ofertas: la navegación no tiene prueba automática. El remoto tampoco distingue si se aceptó desde la
tarjeta o desde el hilo. Estos puntos no se anotaron uno por uno: quedan cubiertos por el ejercicio del autor en conjunto.

### HU-24 · Cancelar una atención `[x]` — 5 puntos

> Como **cualquiera de las partes**, quiero **cancelar una atención acordada
> indicando el motivo**, para **resolver un imprevisto sin dejar el registro abierto**.

**Criterios de aceptación**

- [x] Dado que la atención no se completó, cuando la cancelo, entonces indico un
      motivo obligatorio.
- [x] Dado que cancelo, cuando la contraparte consulta, entonces ve el estado y el motivo.
- [x] Dado que cancelo, cuando consulto el pago asociado, entonces no queda pendiente.
- [x] Dado que la atención ya se completó, cuando intento cancelar, entonces se rechaza.
- [x] Dado que la contraparte cancela, cuando ocurre, entonces me llega una
      notificación **sin el motivo**, que me lleva a la atención, donde sí lo veo.
- [x] Dado que la atención se completó, cuando abro la conversación, entonces veo en esa misma
      pantalla hasta qué fecha puedo escribir.
- [x] Dado que pasaron 7 días desde que se completó, cuando intento escribir o registrar, entonces
      se rechaza, y la conversación se sigue pudiendo leer.

**Requisitos:** RF-10.3, RF-09.3, RF-09.6, RF-09.10, INV-05.

**Agregado el 2026-11-04.** Los dos últimos criterios. El autor decidió que la conversación de una atención completada
admita mensajes durante 7 días y después quede solo para leer, y que la misma pantalla lo diga. Desde HU-21 quedaba abierta
sin fecha de cierre, para coordinar el pago. Lo exige la política de inserción, contra `finished_at`; la lectura no cambia,
porque HU-22 y HU-31 acceden a la conversación desde el historial. El texto y la disposición los propone el agente en el
prototipo de esta historia; la propuesta de partida es la fecha exacta, «Puedes escribir hasta el lun 16 nov, 15:40», porque
una cuenta de días envejece con la pantalla abierta. Sin puntos adicionales.

**Planificación (2026-11-04).** Registrada en `docs/decisions.md`, 2026-11-04, HU-24. Prototipo aprobado el mismo día, en el lienzo
«Cancelar atención HU-24», con un cambio pedido por el autor: el botón lleva recuadro. Se mantuvo en 5 puntos.

**Experimento contra el remoto (2026-11-04), con la segunda migración aplicada dentro de una transacción con `rollback`**

| Caso | Resultado |
|---|---|
| Cancelar sin motivo, con espacios, con saltos de línea o con 301 caracteres | Rechazado, `23514` |
| Cancelar con 300 caracteres, en curso, siendo el profesional | Pasa, y la solicitud queda `CANCELLED` |
| Un ajeno o el administrador cancela | 0 filas |
| Escribir el motivo sin cancelar | Rechazado |
| Escribir `cancelled_by` o `cancelled_at` | Rechazado, `42501` |
| El paciente mueve a mano su solicitud aceptada a `CANCELLED` | Rechazado |
| El paciente cancela una asignada | Pasa: el autor y la fecha sellados, una señal y un aviso |
| El aviso | Al profesional, con el nombre y el servicio, sin el motivo |
| Repetir con el mismo motivo | Pasa, sin otro aviso y con la misma fecha |
| Reescribir el motivo, el autor o la contraparte | Rechazado |
| Marcar llegada en una cancelada | Rechazado, `service_status_is_terminal` |
| Leer la atención cancelada | Las dos partes ven el motivo y quién fue; el profesional, sin la dirección; un ajeno y el administrador, 0 filas |
| Escribir en la conversación de una cancelada | Rechazado |
| «Tu atención», «Mis atenciones», «Mis solicitudes» | Sale de la primera, sigue en la segunda, y no vuelve a la tercera |
| Cancelar una completada | Rechazado, `service_status_is_terminal` |
| Un segundo antes de los 7 días | Las dos partes escriben, el profesional registra, el paciente no registra |
| A los 7 días exactos | Rechazado a los tres; las dos partes siguen leyendo y marcando leído |
| Una atención asignada | Sigue admitiendo mensajes |
| Permisos | `authenticated` actualiza `status` y `cancellation_reason`; `anon`, nada |
| Datos de prueba `05adae36` | Cancelar se rechaza y nada cambia: quedó `COMPLETED` con el servicio `IN_PROGRESS` antes de HU-23 |
| Tras cada rechazo de cancelar | El servicio, la solicitud y el pago siguen como estaban |
| El pago de las dos canceladas | `CANCELLED`, con el desglose intacto; el de otra atención sigue `PENDING` |
| Una parte o el administrador anula un pago vivo | Rechazado, `payment_cancellation_follows_the_service` |
| Una parte o el administrador saca un pago de `CANCELLED` | Rechazado |
| Confirmar un pago cancelado | Rechazado |
| Un ajeno toca el pago | 0 filas |
| Confirmar el pago de una completada | Sigue pasando |

No se pudo probar que cancelar una solicitud abierta siga pasando: el remoto no tenía ninguna. Esa rama del guardia no cambió.

**Mutaciones comprobadas (2026-11-04).** Las 15 hacen fallar al menos una prueba: que una completada se pueda cancelar, que releer
no cierre el diálogo, un segundo toque que envía de nuevo, cerrar mientras se envía, un fallo que deja el diálogo bloqueado, el
encabezado que ignora al motor, registrar pasada la ventana, la portada que promete registrar pasada la ventana, el aviso cerrado
sin fecha, el campo sin fecha, el motivo ya escrito leído como error inesperado, contar en UTF-16, el caso de uso sin validar, la
línea de tiempo sin hora y quitar «Sin pago pendiente».

**Previsualizaciones (2026-11-04).** Abiertas y miradas: atención cancelada en claro, oscuro y 200 %, y la conversación completada en claro, oscuro y 200 %, y cerrada.
El botón y el diálogo no entran en `ScreenCatalog`: el botón queda bajo el pliegue, y el diálogo es otra ventana. Se capturaron una
vez con una prueba temporal, ya retirada: el botón en claro y al 200 %, y el diálogo al 200 % a 320 dp. **Bajar hasta el error no
tiene prueba automática:** a la altura de la prueba de interfaz todo el diálogo cabe, y mutarlo no la hace fallar. Se comprobó solo
en esa captura.

**Confirmado por el autor (2026-11-04).** Aplicó las dos migraciones y recorrió el ejercicio en los dos lados. El remoto lo respalda sin
leer ningún motivo: `54b5c052` la canceló el paciente y `61eb5448` el profesional, a las 22:20 y 22:21 en La Paz. Las dos quedaron con
el servicio, la solicitud y el pago en `CANCELLED`, el autor y la fecha sellados y ningún mensaje posterior. `send-push` respondió 2 veces
con 200, cada una con 1 enviado y 0 fallidos. **Cerrada el 2026-11-04.** No es una historia crítica.

**El error del primer intento.** Antes de esas dos, el autor vio «error inesperado» en un emulador. La causa más probable es `05adae36`,
que es lo que abre «Tu atención» en esas dos cuentas: no dejó ninguna fila a medias, y las dos cancelaciones por «Mis atenciones»
pasaron. No se confirmó, porque la aplicación no registra ese error y la pantalla del emulador no se pudo leer.

**A tener en cuenta.**
- `05adae36` seguirá ocupando «Tu atención» de esa pareja hasta que alguien decida corregirla. Son datos de prueba y no se tocaron.
- Que la conversación se cierre a los 7 días se comprobó en el experimento, no en pantalla: ninguna atención lleva ese tiempo completada.

### HT-29 · Contraofertar el mismo monto acepta la oferta `[x]` — 3 puntos

> Como **paciente o profesional que negocia**, quiero **que contraofertar el monto de la oferta vigente la acepte**, para **no
> seguir negociando cuando ya estamos de acuerdo**.

**Origen.** El 2026-11-04, al probar la negociación durante HU-24, el autor vio que una contraoferta con el mismo monto de la última
oferta deja la negociación abierta. El remoto tenía 2 contraofertas así, de ese mismo día. Pidió que la pantalla avise que ese monto
acepta la oferta, y que la negociación termine como si se hubiera tocado «Aceptar». Eligió una historia aparte, después de HU-24.

**Planificación (2026-11-04).** Registrada en `docs/decisions.md`, 2026-11-04, HT-29. Prototipo aprobado el mismo día, en el lienzo
«Contraoferta igual HT-29». Se mantuvo en 3 puntos.

**Alcance acordado**

- Si el monto escrito es igual al de la oferta vigente, «Enviar contraoferta» abre la confirmación de «Aceptar», con un aviso de que
  ese monto acepta la oferta. Vale para los dos lados.
- Al confirmar se ejecuta la aceptación que ya existe, `accept_offer`: no se crea una oferta nueva.
- El motor rechaza una contraoferta igual a la oferta que responde. Las que ya existen no se tocan.

**Decidido al planificarla**

- **El mínimo del paciente va primero.** Se proponía comparar primero con la oferta vigente; el autor eligió lo contrario. Si la oferta
  vigente está por debajo del mínimo, escribir ese monto sigue mostrando el error, y aceptarla sigue siendo con el botón.
- **El mensaje escrito no se envía.** Si hay uno, la confirmación lo avisa y sugiere la conversación de la atención.

**Verificación (2026-11-04).**

- **Experimento SQL** contra el remoto, con la migración dentro de la transacción y `rollback`. Rechazan con
  `counter_offer_repeats_the_offer_it_answers`: el paciente que repite 150 y 150.00, el profesional que repite 130 y el paciente que
  repite 120 bajo su mínimo. Pasan: 150.01 y 149.99, el profesional con 140, el paciente con exactamente su mínimo y una primera oferta
  igual al presupuesto. Un tercero, otro profesional o quien se hace pasar por paciente reciben la misma denegación de la política con el
  monto correcto y con otro. `accept_offer` da servicio `ASSIGNED` con el monto, pago `PENDING`, oferta y solicitud `ACCEPTED`, ninguna
  oferta nueva y 1 aviso encolado; también acepta 120 bajo el mínimo con el botón. Las 2 contraofertas viejas siguen ahí.
- **Mutaciones de la migración:** sin el filtro por parte, los tres terceros reciben la etiqueta con el monto correcto y la denegación
  con otro; sin el disparador, las contraofertas iguales pasan.
- **Mutaciones de la aplicación:** 12 sobre el caso de uso, el modelo de vista y la pantalla, 3 sobre el desplazamiento de los diálogos del
  precio y de cancelar, y 1 sobre el ayudante de pruebas. Cada una hizo fallar al menos una prueba.
- **`./gradlew build`** en verde: 1118 pruebas, 0 fallas, 0 omitidas. Las dos primeras corridas se cayeron sin relación con el código: en una
  se detuvo el daemon de Gradle, y en la otra la JVM falló al compilar internamente un método de lint.
- **Previsualizaciones:** se abrió el formulario detrás del diálogo en oscuro y al 200 %. El diálogo es otra ventana y el catálogo no lo
  dibuja, así que se capturó aparte: claro y oscuro con letra normal, y claro al 200 % real, donde el aviso del mensaje requiere desplazarse.
  El diálogo oscuro al 200 % no se capturó.
- **Fuera de lo previsto:** el ayudante de pruebas no medía el 200 % en ningún diálogo, y al corregirlo apareció el campo inalcanzable del
  diálogo del precio. Ver `docs/decisions.md`, 2026-11-04, HT-29.

**Recorrido y remoto (2026-11-05).**

- **El disparador desplegado.** El autor aplicó la migración. La definición del remoto coincide con el archivo, ningún cliente puede
  ejecutar la función, y el mismo experimento, sin crearla otra vez, dio los mismos resultados.
- **Recorrido del autor, por los dos lados.** Lo que quedó en el remoto: «Curación de heridas» con presupuesto 90, y las ofertas 100 del
  profesional, 95 del paciente, 98, 95 y 98. La última quedó `ACCEPTED`, sin ninguna oferta después. Servicio `ASSIGNED` por Bs 98, pago
  `PENDING` de 98 con 9,80 de comisión y 88,20 para el profesional, solicitud `ACCEPTED`. Las contraofertas que repiten su oferta siguen
  siendo las 2 viejas.
- **Lo que el remoto no puede mostrar.** La confirmación y «Volver» no dejan filas, y la aceptación deja lo mismo con el botón que con la
  contraoferta, así que el primer y el tercer criterio descansan en lo que el autor vio. Ninguna oferta enviada lleva mensaje. El aviso
  del push se comprobó en el experimento, con 1 encolado, y no en la entrega.

**Criterios de aceptación**

- [x] Dado que me toca responder, cuando escribo el monto de la oferta vigente y envío, entonces veo que ese monto acepta la oferta,
      y puedo confirmar o volver.
- [x] Dado que confirmo, cuando se registra, entonces la negociación termina como al tocar «Aceptar»: el servicio, el pago pendiente,
      el aviso a la contraparte y la atención.
- [x] Dado que vuelvo, cuando reviso la negociación, entonces sigue abierta y conservo lo que escribí.
- [x] Dado que escribo otro monto, cuando envío, entonces la contraoferta sigue siendo una contraoferta.
- [x] Dado que una contraoferta igual a la oferta que responde llega al motor, cuando se inserta, entonces se rechaza.

**Requisitos:** RF-08.2, RF-08.5, INV-06.

**Antes:** HU-24 cerrada y versionada.

**A tener en cuenta.** No figura en la tabla de historias críticas, pero usa la aceptación de HU-19, que sí está: lleva recorrido en
emulador.

### HU-37 · Enterarme del veredicto de mi verificación `[x]` — 3 puntos

> Como **paciente o profesional que subió sus documentos**, quiero **saber cuándo
> el administrador los revisó**, para **corregir un rechazo a tiempo o empezar a
> recibir solicitudes en cuanto me verifiquen**.

**Planificación (2026-11-05).** Registrada en `docs/decisions.md`, 2026-11-05, HU-37. A diferencia de toda historia anterior, ninguno
de los dos eventos pertenece a una solicitud, y `request_push_body`/`PushSubject` estaban armados enteros alrededor de una. Se
presentaron dos decisiones con recomendación:

- **Generalizar el único canal en vez de abrir uno aparte** (elegido): `requestId`, `recipientRole` y `serviceTypeName` pasan a ser
  opcionales en `PushSubject`; `request_push_body` gana `p_profile_id`, `p_document_count` y `p_status`. Sigue siendo una sola función
  vigilada por `NotificationEventParityTest`, como las demás. La alternativa —una función y un test de paridad aparte para
  verificación— quedó descartada por duplicar el mecanismo.
- **El aviso dice solo la cantidad, nunca cuáles documentos** (elegido): «Tus 3 documentos fueron aprobados», con un recurso de
  plurales. Nombrarlos habría exigido una lista que se lea bien combinada en los dos idiomas, y la pantalla ya dice el detalle al
  abrirse.

Sin pantalla nueva que previsualizar: la verificación reutiliza `VerificationScreen` sin tocarla, así que no hubo prototipo que
aprobar. Esta historia figura en «Cuándo hace falta un dispositivo» entre las que no exigen recorrido manual: HU-20 ya probó la
entrega efectiva del canal, y esta solo le agrega dos eventos.

**Alcance acordado**

- Un veredicto, propio o conjunto, avisa una sola vez a quien subió el documento, sin el motivo del rechazo. La pantalla sigue siendo
  donde se lee.
- Un profesional que llega a `APPROVED` recibe un aviso de que ya aparece en las búsquedas.
- Ninguno de los dos cambia el rol activo al abrirse: la verificación responde a todos los roles que la persona tenga.

**Verificación (2026-11-05).**

- **Experimento SQL** contra el remoto, con la migración dentro de la transacción y `rollback`. Un veredicto conjunto sobre 2
  documentos en una sola sentencia encola 1 aviso, no 2; repetir el mismo veredicto sobre filas ya resueltas no encola nada; un
  veredicto de un solo documento encola 1; el cuerpo de un rechazo lleva el estado y la cantidad y nunca el motivo. Un profesional
  que llega a `APPROVED` encola 1 aviso; reafirmar el mismo estado no encola nada; volver a `PENDING` y aprobar otra vez encola otro.
  Un evento desconocido sigue rechazándose. Ninguna de las dos funciones nuevas es ejecutable por `authenticated`.
- **`NotificationEventParityTest`** sigue pasando: los nombres de los dos eventos están escritos como `p_event = '…'` en la migración,
  no con `in (...)`, para que el escaneo los encuentre.
- **Mutaciones del motor:** cambiar el disparador del veredicto de por sentencia a por fila hizo que la aprobación conjunta de 2
  documentos encolara 2 avisos en vez de 1; quitar la condición de la transición en el disparador de `professionals` hizo que
  reafirmar `APPROVED` encolara un aviso de más. Las dos cayeron en el experimento.
- **`./gradlew build`** en verde: 1132 pruebas, 0 fallas, 0 omitidas (1118 antes de esta historia, 14 nuevas).

**Criterios de aceptación**

- [x] Dado que el administrador aprueba o rechaza uno de mis documentos, cuando
      registra el veredicto, entonces me llega una notificación que me lleva a mi
      pantalla de verificación.
- [x] Dado que el documento fue rechazado, cuando llega la notificación, entonces
      **no muestra el motivo**: lo veo al abrir la pantalla, como hasta ahora.
- [x] Dado que el administrador da un veredicto conjunto sobre varios documentos,
      cuando lo registra, entonces me llega una sola notificación, no una por
      documento.
- [x] Dado que soy profesional, cuando el administrador me verifica, entonces me
      llega una notificación de que ya aparezco en las búsquedas.

**Requisitos:** RF-09.9, RF-09.10, RF-04.5.

**Antes:** FA-11, fuera de alcance desde el Sprint 3. Se incorporó el 2026-10-15 al
replanificar las notificaciones: el veredicto es la espera más larga de toda la
aplicación, la decide un tercero y hoy solo se descubre volviendo a abrir la
pantalla. Reutiliza el canal de HU-20 y no exige dispositivo.

## Incremento del sprint

Las partes conversan, el profesional registra lo realizado, marca llegada y
finalización, y la atención queda lista para el pago. Cada paso le llega a la
contraparte como notificación, y el veredicto de cada documento le llega a quien
lo subió.

## Retrospectiva

> El Sprint 7 se ejecutó del **30/10/2026** al **05/11/2026**.

Entregó el resto del ciclo de una atención: la conversación, lo que el profesional registra
mientras trabaja, la llegada y el fin marcados y arrastrados por el motor, el pago pendiente
que queda listo, cancelar con motivo y cerrar la conversación a los 7 días, y el aviso de cada
veredicto de verificación. De paso rehízo la pantalla de inicio (HT-25) y acortó la negociación
a una sola pantalla con la contraoferta que acepta (HT-24 venía de antes; HT-29 la cerró).

**Qué se aprendió.**

1. **Recorrer una historia sigue revelando el alcance de la siguiente, no solo de la primera.**
   HT-26 salió de recorrer HU-21, HT-27 de recorrer HU-22, y HT-29 de recorrer la negociación
   durante HU-24: 14 de los 30 puntos que el sprint ganó después de empezarlo vinieron
   directamente de esa vía, sin contar HT-28, que salió de que el autor revisara HT-27 por su
   cuenta. La graduación del 2026-10-04 —previsualizar en vez de recorrer toda pantalla— deja el
   recorrido disponible como herramienta de descubrimiento, y ese fue su uso más frecuente este
   sprint.
2. **Un ayudante de pruebas puede dar un falso verde durante semanas.** `setContentAtDoubleFontScale`
   nunca escaló un diálogo —toma su densidad de la configuración, no de `LocalDensity`— y ninguna
   prueba de diálogo lo notó hasta HT-29, cuando corregirlo hizo aparecer un defecto real en «Mis
   servicios» que llevaba ahí desde antes. El ayudante compartido es tan parte del contrato de
   prueba como la prueba misma, y hay que mirar lo que mide, no solo si pasa.
3. **El primer evento que no es de una solicitud obliga a declarar explícito lo que antes era
   implícito.** `PushSubject` y `request_push_body` se diseñaron alrededor de una solicitud
   porque los primeros nueve eventos lo eran; HU-37 no lo es, y generalizar en vez de abrir un
   segundo mecanismo mantuvo una sola función vigilada por un solo test de paridad, al costo de
   volver opcionales tres campos que antes parecían siempre presentes.
4. **Reutilizar la pantalla que ya existe sigue siendo más barato que construir otra.** HU-37 no
   tocó `VerificationScreen`; el veredicto y la aprobación abren la misma pantalla de siempre, sin
   previsualización nueva que verificar.

**Verificación de cierre.** `./gradlew build` en verde: 1132 pruebas, 0 fallas, 0 omitidas.
Experimentos SQL de política y disparadores anotados por historia, incluidos los de HU-37.
`supabase db diff --linked --schema public` el 2026-11-05, tras aplicar las 54 migraciones sobre
la base de sombra: la única diferencia contra el remoto era la migración de HU-37, sin deriva
alguna por fuera de ella. El autor aplicó ese `db push` y confirmó el control de acceso de cierre
con las dos cuentas propietarias de Google Cloud y los dos miembros de Supabase, el 2026-11-05;
el remoto tiene ahora los dos disparadores de HU-37 y una sola versión de `request_push_body`,
la de siete parámetros.

**Velocidad.** Empezó en 26 puntos y cerró en 56, en seis días: creció más de lo que duró,
porque la mitad de sus historias no estaban planificadas al empezar y llegaron por recorrido,
por revisión del autor o por decisiones tomadas al planificar cada una, sin un bloqueo externo
como el de HU-20 en el Sprint 6. El Sprint 8 tiene 21 puntos, de confirmación de pagos: HU-25
depende de que exista una atención completada, que este sprint ya deja.

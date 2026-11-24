---
paths:
  - "**/*.kt"
  - "**/*.sql"
---

# Glosario de dominio — español a inglés

Se carga al trabajar con archivos Kotlin o SQL.

**Regla.** Todo identificador de código y de esquema se escribe en inglés. Nunca
transliterar. No usar `Paciente`, `Solicitud`, `Calificacion`, `Monto`. Si un
término del dominio no figura en esta tabla, agregarlo aquí en el mismo commit
que lo introduce.

**Cómo se escribe una fila.** Qué es y la regla que no se puede romper, en una línea.
El porqué y la historia viven en la historia o la decisión citada entre paréntesis,
no aquí: este archivo se carga con cada `.kt` y cada `.sql`. Un nombre que se retira
pasa a «Nombres retirados», al final.

## Actores y roles

| Español | Inglés | Uso |
|---|---|---|
| Perfil | `Profile` | Identidad común a toda persona |
| Paciente | `Patient` | Rol que solicita atención |
| Profesional | `Professional` | Rol que presta atención |
| Administrador | `Admin` | Rol de verificación de documentos y resolución de disputas de pago |
| Roles de una persona | `ProfileRoles` | Los roles que tiene y cuál está activo. Una persona puede tener paciente y profesional a la vez |
| Médico | `DOCTOR` | Valor de `professional_type` |
| Enfermera | `NURSE` | Valor de `professional_type` |
| Fisioterapeuta | `PHYSIOTHERAPIST` | Valor de `professional_type` |
| Estudiante | `STUDENT` | Valor de `professional_type` |

## Direcciones, catálogo y búsqueda

| Español | Inglés | Uso |
|---|---|---|
| Dirección | `Address` | Domicilio georreferenciado |
| Tipo de servicio | `ServiceType` | Catálogo de tipos de atención |
| Servicio ofrecido | `ProfessionalService` | Unión profesional–tipo de servicio, con el precio que el profesional declaró |
| Servicios declarados | `DeclaredServices` | Lo que el profesional presta hoy junto con el catálogo que le queda por declarar |
| Tipos por declarar | `undeclaredTypes` | Los tipos del catálogo que el profesional todavía no ofrece. No se le ofrecen dos veces |
| Servicios de otro profesional | `ProfessionalServicesResult` | Lo que un visitante lee del catálogo ajeno: lo declarado, sin los tipos por declarar |
| Franja de disponibilidad | `AvailabilitySlot` | Horario declarado por el profesional |
| Disponibilidad semanal | `WeeklyAvailability` | La vista agrupada por día: las siete claves siempre pobladas, aun sin franjas |
| Borrador de franja | `SlotDraft` / `SlotEditor` | La franja que se declara y su editor, dentro de su día. Sin diálogo ni selector de reloj (`docs/decisions.md`, 2026-10-15) |
| Rejilla de medias horas | `SlotTimes` | Las horas del editor: cada 30 minutos, y las de «Hasta» siempre posteriores al inicio elegido |
| Error de disponibilidad | `AvailabilityError` | `EndNotAfterStart`, `OverlapsExistingSlot`, `NetworkUnavailable`, `Unexpected`. Solapamiento y duplicado llegan por el mismo errcode 23505 y presentan igual |
| Guarda contra solapamientos | `reject_overlapping_slot` | `BEFORE INSERT/UPDATE` que usa `OVERLAPS` sobre `time` contra las franjas del mismo profesional y día |
| Búsqueda por cercanía | `ProfessionalSearch` | La característica entera: `features/search/` y `search_nearby_professionals` |
| Profesional cercano | `NearbyProfessional` | Una fila del resultado: lo público del profesional, su distancia y el punto aproximado de su base |
| Criterios de búsqueda | `SearchCriteria` | Radio, tipo de servicio y disponibilidad con que el paciente recorta la búsqueda |
| Origen de la búsqueda | `SearchOrigin` | La dirección **principal** del paciente. Nunca su base profesional |
| Radio de búsqueda | `SearchRadius` | 2, 5 y 10 km; 5 por omisión |
| Punto aproximado de la base | `basePoint` / `base_latitude`, `base_longitude` | Dónde se dibuja el marcador: la base redondeada a la manzana. Nunca la coordenada exacta |
| Distancia en metros | `distanceM` | Redondeada a la centena de metros antes de salir del motor |
| Cobertura de un profesional dado | `professional_covers_for` | La regla de cobertura escrita una vez. Ningún cliente la ejecuta: con profesional y punto libres sería un oráculo sobre su base |

## Solicitudes y agenda

| Español | Inglés | Uso |
|---|---|---|
| Solicitud de atención | `ServiceRequest` | Petición publicada por el paciente |
| Atención ahora | `ImmediateCare` / `ImmediateCareRoute` | La pantalla de la inmediata: mapa de disponibles, formulario y espera de ofertas en una sola (HT-23) |
| Disponibles cerca | `NearbyState` / `FindProfessionalsAvailableNowUseCase` | Lo que muestra ese mapa: disponibles ahora, con el servicio elegido, en el radio mayor, alrededor de la dirección elegida. Indicativo, no la lista de destinatarios |
| Solicitud inmediata activa | `ActiveRequestResult` / `GetActiveImmediateRequestUseCase` | La inmediata más reciente del paciente que sigue publicada o en negociación: `Found`, `None` o `Failure` |
| Dirección elegida al volver | `selectionAfterRefresh` | Al volver de «Agregar otra dirección»: la nueva; si no hay, la que estaba; si se borró, la principal |
| Cita agendada | `RequestModality.SCHEDULED` / `create_scheduled_request` | Solicitud dirigida a un profesional para una fecha y hora. Solo nace por esa función, que valida la hora contra `bookable_start_times` |
| Horarios reservables | `bookable_start_times` / `BookableTimes` / `BookableStart` | Inicios en los próximos 14 días: cada 30 minutos desde el inicio de la franja, con 2 horas de anticipación y la atención completa dentro de una franja. Cada uno trae el instante y su fecha y hora en La Paz |
| Zona de la cita | `AppointmentZone` | `America/La_Paz`. Solo para mostrar una `scheduled_at` guardada; el cálculo vive en el motor |
| Parte del día | `PartOfDay` | `MORNING` antes de las 12:00, `AFTERNOON` hasta las 18:00, `EVENING` desde las 18:00. En `core/vo/`: lo usan agendar y disponibilidad |
| Agendar una cita | `ScheduleAppointment` | La pantalla y su modelo de vista; se abre desde la ficha del profesional |
| Puede agendar | `CanScheduleAppointmentUseCase` / `showsScheduleAction` | La ficha ofrece «Agendar cita» con rol activo paciente, ficha ajena y profesional con servicios declarados |
| Bandeja de solicitudes | `request_inbox` / `InboxRequest` | Las inmediatas publicadas en el radio del profesional y las agendadas dirigidas a él, por RPC de definidor. Tipo, ciudad, presupuesto, modalidad, hora de la cita y distancia redondeada; nunca la ubicación exacta ni la identidad del paciente (HU-16) |
| Aviso de la bandeja | `inboxSignals` / `notify_immediate_inbox` | Broadcast sin datos al tema `immediate_inbox`; el cliente re-consulta. No transporta la fila |
| Solicitud para ofertar | `OfferableRequest` / `inbox_request` | Lo que el profesional abre para ofertar: la fila de la bandeja más si ya ofertó. Nunca la ubicación exacta ni la identidad del paciente |
| Solicitud abierta | `is_open` | Columna de `my_service_requests`: publicada o en negociación. Va primero en «Mis solicitudes» |
| Ya tiene atención | `has_attention` | Columna de `my_service_requests`: llegó a un servicio, así que su tarjeta está en «Mis atenciones», en cualquier estado |
| Ofertas abiertas | `open_offer_count` / `openOfferCount` / `showsOfferCount` | Columna de `my_service_requests`: negociaciones cuya última oferta sigue propuesta y sin respuesta. Solo se muestra en una inmediata abierta |
| Dónde se negocia | `NegotiationEntry` | A dónde lleva una tarjeta de «Mis solicitudes»: la espera, si es inmediata abierta; el hilo, si es agendada en negociación; nada, si no queda qué negociar |

## Negociación

| Español | Inglés | Uso |
|---|---|---|
| Oferta | `RequestOffer` | Propuesta de monto sobre una solicitud. El paciente la lee con el nombre y la calificación públicos del profesional |
| Contraoferta | Oferta con `parentOfferId` | No es un tipo aparte |
| Estado de la oferta | `OfferStatus` | `PROPOSED`, `ACCEPTED`, `REJECTED`, `EXPIRED`. Lo único que el cliente escribe de una oferta emitida, y solo de `PROPOSED` a `REJECTED` |
| Hilo de la negociación | `OfferThread` / `OfferThreadRoute` | La cadena de ofertas de un profesional sobre una solicitud, en orden, leída y contestada por los dos lados en una pantalla, donde el profesional hace también la primera oferta. Qué lado lee lo decide su identificador contra `professionalId` (INV-16; HU-18, HT-24) |
| Primera oferta | `OfferThreadUiState.Opening` | Esa pantalla antes de ofertar: detalle de la solicitud y formulario. Al enviar pasa a ser la negociación (HT-24) |
| Turno del hilo | `isMyTurn` / `offer_continues_thread` | Se contesta la oferta sobre la mesa: la que sigue `PROPOSED` y sin respuesta. El definidor recibe el emisor que el padre debe tener, y cada rama de la política valida el suyo |
| Hilo cerrado | `isClosed` / `guard_offer_rejection` | Un rechazo cierra la cadena: nadie contesta una oferta rechazada ni rechaza una ya contestada, y el rechazado no abre otra, porque tiene una sola primera oferta por solicitud |
| Rechazar una oferta | `RejectOfferUseCase` / `request_offers_reject_counterparty` | La rechaza la contraparte del emisor, y solo marca esa oferta `REJECTED`: la solicitud sigue en `NEGOTIATING` y visible para los demás. La política que no admite la fila actualiza cero filas sin lanzar: cero es denegación |
| Ya ofertó | `professional_already_offered_on` | Definidor: si el llamante ya tiene una fila en la solicitud. Una primera oferta por profesional y solicitud; con dos escritores la sostienen dos índices únicos parciales |
| Puede ofertar | `professional_may_offer_on` | Definidor: si el llamante puede ofertar, sin devolver ubicación ni identidad. La rama agendada decide por `professional_id`, no por cobertura (HU-17) |
| Ofertas de mi solicitud | `my_request_offers` | Definidor: las ofertas de la propia solicitud del paciente, con el directorio público del profesional |
| Reloj de la oferta | `stamp_offer_created_at` | El motor sella `created_at`; el cliente nunca pone la fecha |
| Pasar a negociación | `move_request_to_negotiating` | Disparador: `PUBLISHED` → `NEGOTIATING` con la primera oferta |
| Mínimo del paciente | `counterFloorBob` / `guard_patient_counter_floor` | El paciente nunca contraoferta por debajo del mayor entre su presupuesto sugerido y su propuesta más alta en ese hilo. Lo dice la pantalla y lo exige el motor, solo para el dueño de la solicitud |
| Contraoferta que acepta | `CounterOfferResult.AcceptsTheOffer` / `guard_counter_differs_from_the_offer` | Repetir el monto vigente abre la confirmación de «Aceptar» en vez de crear otra oferta; el mínimo del paciente se evalúa antes. El motor rechaza una contraoferta igual a la que responde (HT-29) |
| Tarjeta de oferta | `OfferCard` | Un profesional y su oferta como los ve el paciente mientras espera: nombre, calificación, distancia, monto sobre la mesa y turno. En su turno, «Aceptar» con el monto y «Negociar»; tocar el resto abre el hilo (HT-24) |
| Aceptar desde las ofertas | `ImmediateCareUiState.Waiting.confirmingAcceptance` / `AcceptOfferDialog` | La oferta queda fijada al abrir la confirmación: una señal no cambia lo que se confirma. El diálogo es el mismo del hilo (HT-28) |
| Primera lectura de ofertas | `offersRead` | Falso hasta la primera respuesta: mientras tanto la pantalla no dice que sigue buscando |
| Aceptar una oferta | `AcceptOfferUseCase` / `accept_offer` | Operación atómica (RF-08.5) que ejecuta la contraparte del emisor: marca la oferta `ACCEPTED`, crea el servicio y el pago pendiente, y lleva la solicitud a `ACCEPTED` con su `professional_id`, en una transacción (HU-19) |
| Reconocer la operación atómica | `guard_request_lifecycle` | Admite `→ ACCEPTED` y el cambio de `professional_id` solo si ya existe el `services` de esa solicitud, que solo `accept_offer` inserta. La fila es la prueba, no una bandera (`docs/decisions.md`, 2026-10-29) |
| Aviso de oferta al paciente | `patient_offers` / `notify_patient_offers` | Broadcast sin datos a `patient_offers:<id del paciente>` al insertarse una oferta. `patient_offers_listen` deja escuchar solo el propio tema |
| Tema del profesional | `professional_offers` | Broadcast sin datos a `professional_offers:<id del profesional>`, con `professional_offers_listen`, también al rechazar. En el cliente, un flujo compartido por tema (HT-24) |
| Encolar el aviso de oferta | `queue_offer_push` / `OFFER_RECEIVED` | Encola `OFFER_RECEIVED` si el emisor es el profesional, con tipo de servicio y monto, nunca el mensaje |
| Aviso de contraoferta | `COUNTER_OFFER_RECEIVED` / `notify_offer_parties` | Llega siempre a la contraparte del emisor, en cualquiera de los dos roles; sin padre es `OFFER_RECEIVED`. Lleva monto y `professionalId`, nunca el mensaje (RF-09.10) |
| Aviso de oferta aceptada | `OFFER_ACCEPTED` / `queue_acceptance_push` | Al emisor de la oferta aceptada; lleva a la atención. Lo encola un disparador `after insert on services` dentro de la transacción: un `rollback` no envía aviso. Lleva el monto, nunca un mensaje |

## Atención

| Español | Inglés | Uso |
|---|---|---|
| Atención / Servicio | `Service` | Atención acordada y prestada |
| La atención | `Attention` / `AttentionScreen` / `my_attention` | Pantalla del acuerdo cerrado, en `features/services`: monto congelado, estado y contraparte, sin teléfono ni llamada; se coordina por la conversación. `my_attention` es un definidor solo para las partes, con la contraparte relativa a `auth.uid()` (`docs/decisions.md`, 2026-10-30) |
| Lado de la atención | `AttentionSide` | `PATIENT` o `PROFESSIONAL`: cuál de sus dos lados lee una persona. Solo elige entre filas propias y no concede nada (INV-17) |
| Mis atenciones | `MyAttentionsScreen` / `AttentionSummary` / `my_attentions` | Lo acordado con su estado, segunda vista de «Solicitudes» en los dos roles. Cada fila lleva solo lo que su tarjeta muestra, también completadas y canceladas (HT-26, HT-27) |
| Fecha de la tarjeta | `cardDate` / `AttentionCardDate` / `AttentionDateKind` | La cita si la hay; si no, el inicio; antes de iniciar, el acuerdo |
| Atención activa | `my_active_attention` / `GetActiveAttentionUseCase` / `ActiveAttentionResult` | La que está en curso y, si no hay, la próxima asignada. La abre la fila «Tu atención» de Inicio, en los dos roles |
| Avance de la atención | `AttentionStep` / `nextStep` / `AdvanceAttentionUseCase` | `MarkArrival` y `MarkCompleted`, lo que el profesional marca a continuación. El paciente nunca tiene un paso, y marcar dos veces no hace nada (HU-23) |
| Sellar el avance | `stamp_service_progress` / `started_at`, `finished_at` | El motor sella inicio y fin al cambiar el estado; ningún cliente los escribe |
| La solicitud sigue a la atención | `move_request_with_service` | Disparador que lleva la solicitud a `IN_PROGRESS` y `COMPLETED` en la misma transacción. El guardia lo admite si el servicio ya está en ese estado |
| Señal de la atención | `attention:<request_id>` / `notify_attention_changed` / `attention_listen` / `ObserveAttentionUseCase` | Broadcast sin datos para releerla en vivo; solo las dos partes. Tema propio, no el de la conversación |
| Dónde es la atención | `AttentionPlace` | La ciudad siempre; dirección, referencia y punto, para el paciente siempre y para el profesional solo con la atención asignada o en curso |
| Línea de tiempo | `TimelineStep` / `timeline` | El acuerdo, la llegada y el fin con sus horas |
| Cancelar la atención | `CancelAttentionUseCase` / `canCancel` / `CancelDraft` | Cualquiera de las partes, asignada o en curso, con motivo. El cliente envía solo estado y motivo; el motor sella quién y cuándo. `CancelDraft` guarda lo escrito mientras el diálogo está abierto (HU-24) |
| Motivo de cancelación | `CancellationReason` / `AttentionCancellation` | Objeto de valor: recortado, no vacío, hasta 300 puntos de código, como `char_length`. Se escribe una vez; lo leen las partes y el administrador; nunca viaja en un aviso (RF-09.10) |

## Conversación

| Español | Inglés | Uso |
|---|---|---|
| Mensaje | `Message` | Comunicación dentro de la solicitud |
| La conversación | `Conversation` / `ConversationScreen` / `ConversationRoute` | Mensajes de una atención, en `features/conversation`. Se abre desde la atención, y el aviso la abre con la atención debajo. Se escribe salvo con la atención cancelada o 7 días después de completada; después solo se lee (HU-21, HU-24) |
| Conversación abierta al escribir | `conversation_is_open` / `conversation_writable_until` / `writableUntil` | Si admite mensajes y hasta cuándo. Los 7 días viven solo en el motor; las políticas y `my_attention` usan estas funciones |
| Contenido del mensaje | `MessageContent` | Objeto de valor: recortado, no vacío, hasta 4000 puntos de código, como `char_length` |
| Cursor del mensaje | `MessageCursor` / `conversation_page` | `(created_at, id)`: se pagina por cursor, no por desplazamiento, comparando por fila sobre el índice. `startOf` es el cursor anterior a todo mensaje de un instante |
| Ponerse al día | `CatchUpMessagesUseCase` | Lo que hace una señal: todo lo posterior a lo que hay en pantalla, página tras página hasta una corta. Mientras mi último mensaje siga sin leer, empieza en él, para traer su «Leído» |
| Reloj y lectura del mensaje | `stamp_message_created_at` / `stamp_message_read_at` | El motor sella `created_at` al insertar y `read_at` la primera vez; nadie los cambia ni los borra |
| Marcar leído | `mark_conversation_read` / `MarkConversationReadUseCase` | Función de invocador que marca leídos los mensajes de la contraparte. Solo con la pantalla en primer plano |
| Señal de la conversación | `conversation:<request_id>` / `notify_conversation_message`, `notify_conversation_read` / `conversation_listen` | Broadcast sin datos al insertar un mensaje y una vez por sentencia al leer. Solo las dos partes |
| Señales de Broadcast | `broadcastSignals` | El ciclo de un canal privado sin datos, en `core/network`. Lo usan la bandeja, las ofertas y la conversación |
| Registro de la atención | `isWorkRecord` / `is_work_record` / `WorkRecordRow` | Un mensaje marcado con lo realizado. Solo lo escribe el profesional de esa atención, en curso o completada, y lo exige el motor. No se edita: corregir es agregar otro (HU-22) |
| Puede registrar | `canRecord` / `recordRequested` / `recording` | Lo que la cabecera permite, lo que la persona pidió y lo que vale: lo pedido deja de contar cuando la cabecera ya no lo permite |
| Aviso al estudiante | `showsStudentNotice` | Que describa lo que hizo sin un diagnóstico (RN-03). Informa, no impide. Con el tipo de profesional desconocido, se muestra |
| Por enviarse | `ConversationItem.Sending` / `NotSent` | Mensaje que no llegó al motor, con el identificador que generó el teléfono; el reintento lo reutiliza |
| Conversación abierta | `enterConversation` / `leaveConversation` / `DiscardReason.CONVERSATION_OPEN` | La que está en primer plano. Su aviso de mensaje se descarta en el teléfono |

## Pagos

| Español | Inglés | Uso |
|---|---|---|
| Pago | `Payment` | El pago de una atención por `totalAmountBob`, igual al monto congelado del servicio e íntegro del profesional (INV-10). Se confirma por las dos partes (INV-09) |
| Pago de la atención | `AttentionPayment` / `payment_status`, `payment_method` en `my_attention` | El estado, el medio y las dos horas de confirmación, como los lee cualquiera de las dos partes (HU-25) |
| Estado del pago | `PaymentStatus` | `PENDING`, `PATIENT_CONFIRMED`, `PROFESSIONAL_CONFIRMED`, `BOTH_CONFIRMED`, `DISPUTED`, `RESOLVED_PAID`, `RESOLVED_NOT_PAID`, `CANCELLED` |
| Medio de pago | `PaymentMethod` / `declarable` | `CASH` y `DIRECT_QR` los elige quien declara primero, sin uno preseleccionado (HU-28). `GATEWAY` nunca lo escribe un cliente (FA-02): lo rechazan el caso de uso y el motor |
| Confirmar que pagué | `canConfirmPayment` / `ConfirmPaymentUseCase` / `PaymentDraft` | Solo el paciente, con la atención completada y el pago pendiente, una sola vez: no se deshace ni cambia el medio (HU-25). El cliente envía estado y medio; el borrador guarda el medio mientras el diálogo está abierto |
| Confirmar que cobré | `canAnswerPayment` / `PaymentAnswer` / `AnswerPaymentUseCase` / `AnswerDraft` | El profesional responde una sola vez, después de que el paciente confirmó: `RECEIVED` lleva a `BOTH_CONFIRMED`, `NOT_RECEIVED` a `DISPUTED`. Sin motivo, y no se deshace (HU-26). El botón elige la respuesta; el diálogo solo la confirma |
| Confirmar el cobro primero | `canConfirmReceipt` / `ConfirmReceiptUseCase` / `PROFESSIONAL_CONFIRMED` | El profesional declara que cobró antes que el paciente, con el medio; su parte queda cerrada (HU-28). Comparte el diálogo y el borrador de «Confirmar que pagué» |
| Aceptar el cobro | `canAcceptReceipt` / `AcceptReceiptUseCase` / `onAcceptReceipt` | Lo único que el paciente hace ante el cobro que el profesional declaró primero: lleva a `BOTH_CONFIRMED` y conserva el medio. Sin diálogo, porque solo da la razón a lo registrado |
| Declarar el pago | `canDeclarePayment` | Confirmar que pagué o confirmar el cobro primero: lo que cada parte puede hacer mientras nadie habló |
| Pago en revisión | `DISPUTED` / `disputed_at` / `disputedAt` | La disputa que abrió el profesional, a la espera del administrador (HU-28). La hora la sella el motor |
| Resolver la disputa | `DisputeResolution` / `ResolveDisputeUseCase` / `RESOLVED_PAID`, `RESOLVED_NOT_PAID` | El administrador da el pago por pagado o por no pagado, una sola vez. Los dos estados son finales; el motor sella `resolved_at` y `resolved_by` (HU-28) |
| Disputa de pago | `PaymentDispute` / `payment_dispute_queue` | Lo que el administrador revisa: monto, medio declarado, las dos partes con su correo y cada hora, nunca la conversación. La vista filtra por `is_admin()` |
| Sellar la confirmación | `stamp_payment_confirmation` | El motor pone `patient_confirmed_at`, `professional_confirmed_at`, `disputed_at`, `resolved_at` y `resolved_by` en sus transiciones. Su disparador se ordena después de la guarda, que ve la fila como la mandó el cliente |
| Quién escribe el pago | `guard_payment_confirmation` | El paciente, `PENDING` → `PATIENT_CONFIRMED` con su medio, o `PROFESSIONAL_CONFIRMED` → `BOTH_CONFIRMED`; el profesional, `PENDING` → `PROFESSIONAL_CONFIRMED` con su medio, o `PATIENT_CONFIRMED` → `BOTH_CONFIRMED` o `DISPUTED`; el administrador, solo `DISPUTED` → una resolución; nadie las horas ni los campos de pasarela. La cancelación que sigue al servicio pasa antes de esas reglas |
| Aviso de pago confirmado | `PATIENT_CONFIRMED_PAYMENT` / `queue_payment_push` / `paymentMethod` | Al profesional, con el nombre del paciente, el monto y el medio, y abre la atención. Canal `ATTENTIONS`. `paymentMethod` es una clave más de la lista de `send-push` |
| Aviso de cobro confirmado | `PROFESSIONAL_CONFIRMED_PAYMENT` / `ProfessionalConfirmedPayment` | Al paciente, cuando el profesional declaró primero, con su nombre, el monto, el medio y el tipo de servicio; abre la atención para aceptarlo. Canal `ATTENTIONS` |
| Aviso de pago en revisión | `PAYMENT_DISPUTED` / `PaymentDisputed` | Al paciente, con el nombre del profesional, el monto y el tipo de servicio, y abre la atención. Canal `ATTENTIONS`. Nadie recibe aviso cuando confirman ambos |
| Aviso de pago revisado | `PAYMENT_RESOLVED` / `PaymentResolved` | A cada parte, en un envío propio, con el nombre de la otra, el monto, el tipo de servicio y el sentido en `status`; abre la atención. Nunca nombra al administrador |
| Señal del pago | `notify_attention_payment_changed` | Un cambio de estado del pago avisa al tema `attention:<request_id>`, así la atención abierta se relee |
| Anular el pago | `cancel_payment_with_service` | Disparador que lleva el pago a `CANCELLED` al cancelarse la atención. Ningún cliente lo hace: `guard_payment_confirmation` exige el servicio cancelado, y de ese estado no se sale |

## Calificaciones

| Español | Inglés | Uso |
|---|---|---|
| Calificación | `Review` | Puntaje y comentario cruzados |
| Comentario de la ficha | `ProfessionalReview` | Una calificación como la muestra la ficha: puntaje, comentario y fecha. **Sin su autor**, que no se publica |
| Proyección pública de calificaciones | `professional_reviews` | La vista que decide cuáles son públicas. El cliente nunca consulta `reviews` |
| Reputación | `averageRating` | Promedio de calificaciones recibidas |
| Total de calificaciones | `totalReviews` | Cuántas calificaciones sostienen el promedio |

## Verificación y revisión

| Español | Inglés | Uso |
|---|---|---|
| Documento de verificación | `VerificationDocument` | Respaldo de identidad o título |
| Documento adicional | `DocumentType.OTHER` | Adjunto opcional por persona, con leyenda obligatoria |
| Leyenda del documento | `caption` / `DocumentCaption` | Línea de texto que acompaña al `OTHER` |
| Checklist de verificación | `VerificationChecklist` | Documentos requeridos según rol más el adjunto opcional |
| Documentos requeridos | `required_document_types` | Función del motor que dice qué debe tener aprobado una persona |
| Cola de revisión | `PendingReviewSubject` | Persona con trabajo pendiente del administrador: documentos por revisar, la verificación por conceder, o las dos |
| Listo para verificar | `awaitingVerification` / `awaiting_verification` | Profesional con todos sus requeridos aprobados que todavía no es `APPROVED` |
| Quién espera la verificación | `professionals_awaiting_verification` | Función del motor, solo para el administrador, que dice a quién le falta el acto de verificar |
| Esperando desde | `waitingSince` / `waiting_since` | Cuándo pasó el turno al administrador: la subida del documento o la aprobación del último requerido |
| Criterios de la cola | `PendingReviewQuery` | Búsqueda, filtro por rol y orden |
| Filtro por rol | `ReviewRoleFilter` | `ALL`, `PATIENT` o `PROFESSIONAL` |
| Orden de la cola | `ReviewQueueOrder` | `OLDEST_FIRST` o `NEWEST_FIRST`, por la fecha del documento más antiguo |
| Revisado | `DocumentReviewSubject` | La persona cuyos documentos revisa el administrador |
| Expediente de revisión | `DocumentReviewDossier` | La persona revisada, sus documentos y su lista de requeridos |
| Documentos marcados | `checkedTypes` | Los seleccionados para un veredicto conjunto |
| Veredicto | `verdict` | Aprobación o rechazo de un documento por el administrador |
| Motivo de rechazo | `RejectionReason` | Objeto de valor del texto que el usuario lee al ser rechazado un documento |
| Verificar al profesional | `approveProfessional` | Acto del administrador que lleva al profesional a `APPROVED` |
| Visor a pantalla completa | `FullScreenDocumentImage` | El documento sobre fondo oscuro, con zoom y arrastre |
| Transformación de la imagen | `ImageTransform` | La escala y el desplazamiento del visor, con su recorte |

## Notificaciones

| Español | Inglés | Uso |
|---|---|---|
| Notificación push | `PushPayload` / `features/notifications/` | El evento, a quién y lo necesario para abrirlo. Nunca texto escrito por un usuario (RF-09.10): el texto lo compone el teléfono |
| Evento notificado | `NotificationEvent` | `REQUEST_NEARBY`, `APPOINTMENT_SCHEDULED`, `APPOINTMENT_CANCELLED`, `REQUEST_EXPIRED`, `OFFER_RECEIVED`, `COUNTER_OFFER_RECEIVED`, `OFFER_ACCEPTED`, `MESSAGE_RECEIVED`, `PROFESSIONAL_ARRIVED`, `SERVICE_COMPLETED`, `ATTENTION_CANCELLED`, `DOCUMENT_VERDICT`, `VERIFICATION_APPROVED`, `PATIENT_CONFIRMED_PAYMENT`, `PROFESSIONAL_CONFIRMED_PAYMENT`, `PAYMENT_DISPUTED`, `PAYMENT_RESOLVED`. Mismos nombres en SQL y Kotlin: `NotificationEventParityTest` lee solo la última migración que define `request_push_body`, así que esa función se reescribe entera cada vez |
| Aviso de mensaje | `MESSAGE_RECEIVED` / `senderName` | Lleva el nombre del remitente, nunca el contenido; con el teléfono bloqueado no muestra el nombre |
| Veredicto de un documento | `DOCUMENT_VERDICT` / `queue_verification_document_push` | Disparador por sentencia, agrupado por `profile_id`: un veredicto conjunto avisa una vez. Lleva el estado y la cantidad, nunca el motivo del rechazo (HU-37) |
| Verificación aprobada | `VERIFICATION_APPROVED` / `queue_verification_approved_push` | Al profesional, cuando `verification_status` llega a `APPROVED`. Disparador por fila, solo en esa transición (HU-37) |
| Sujeto del aviso | `PushSubject` | Solicitud, destinatario, rol con que recibe y tipo de servicio. Sin solicitud (los dos de verificación), `requestId`, `recipientRole` y `serviceTypeName` son nulos y abrirlos nunca cambia el rol activo (HU-37) |
| Destino al tocar | `NotificationDestination` | `Inbox`, `MyRequests`, `OfferThread(requestId, professionalId)`, `Attention(requestId)`, `Conversation(requestId)` sobre la atención, y `Verification`. El rol con que se recibe se activa antes de abrir (HT-24) |
| Espacio del aviso | `NotificationSlot` | `EVENT` o `MESSAGE`: un aviso se identifica por su solicitud y su espacio, y un mensaje no reemplaza otro evento de la misma solicitud. Sin solicitud, por el destinatario |
| Canal de notificación | `NotificationChannelSpec` | Un canal de Android por categoría: `REQUESTS`, `MESSAGES`, `ATTENTIONS` y `VERIFICATION`. Cada historia agrega el de su evento |
| Avisos mostrados | `ShownNotificationsDataSource` | Los que siguen en la bandeja del sistema. Se retiran al cerrar sesión |
| Explicación del permiso | `NotificationPermissionPrompt` / `NotificationRationale` | La tarjeta que explica los avisos antes de pedir el permiso. Se muestra una sola vez |
| Identificador de dispositivo | `DeviceToken` | Destino de notificaciones |
| Identificador de instalación | FID / `installationId` | Lo que guarda `device_tokens.token`: el ID de instalación de Firebase (HU-20) |
| Registrar el dispositivo | `register_my_device` / `unregister_my_device` | Las dos únicas vías de escritura de `device_tokens`. El registro le da el FID a la última cuenta que ingresó en el teléfono |
| Destinatarios de la inmediata | `immediate_request_recipients` | La regla de `request_inbox` más `available_now`. Solo la llama el motor |
| Cuerpo del envío | `request_push_body` | Arma el evento, los identificadores y los destinatarios con sus FID. Lo encola `queue_request_push` con `pg_net` hacia la función `send-push` |
| Alcance de la aplicación | `ApplicationScope` | El `CoroutineScope` que sobrevive a las pantallas, donde el servicio de mensajería deja su trabajo |

## Inicio, navegación y componentes

| Español | Inglés | Uso |
|---|---|---|
| Pantalla de inicio | `HomeShell` / `HomeRoute` | Después del ingreso: cabecera, cuerpo por destino y barra (HT-25) |
| Destino de la barra | `HomeTab` | `HOME`, `SETTINGS`, `REQUESTS`, `ACCOUNT`: los mismos para los dos roles. El administrador no tiene |
| Barra de navegación | `BottomNavigationBar` | La barra anclada al pie con los destinos y sus rótulos |
| Menú de la cuenta | `AccountMenu` | El menú de las iniciales en la cabecera: la persona, el cambio de rol y «Cerrar sesión» |
| Ajustes | `SettingsContent` | Verificación, servicios, disponibilidad y direcciones. Su pantalla se titula «Configuración» |
| Solicitudes cerca de ti | `NearbyRequests` | Las primeras tres de la bandeja en el Inicio del profesional, con «Ver todas» |
| Tu solicitud | `PatientHomeUiState.Content.activeRequest` | La inmediata activa del paciente en su Inicio. Un atajo: si su lectura falla, no se dibuja |
| Vistas de Solicitudes | `RequestsTabContent` / `ProfessionalRequestsTab` / `PatientRequestsTab` / `RequestsView` | `REQUESTS` («Disponibles» o «Mis solicitudes») y `MY_ATTENTIONS`. Salir del destino vuelve a la primera; volver de una atención conserva la vista |
| Tarjeta común | `ServiceCard` / `ServiceCardAmount` | La tarjeta de las tres listas: los dos lados leen lo mismo en el mismo lugar |
| Píldora de estado | `StatusBadge` / `StatusTone` | El estado de una solicitud o atención, igual dondequiera que se lea |
| Deslizar para actualizar | `RefreshableContent` / `ListRefresher` / `ListRefresh` | Relee la primera página; si falla, deja la lista como estaba y lo dice |
| Selector desplegable | `ExpandableOptionPicker` | Para elegir entre muchas opciones de nombre largo, como los tipos de servicio |
| Carrusel de días | `DayCarousel` | Tarjetas de día que se detienen en cada una al deslizar |
| Fila de horas | `TimeChipRow` | Píldoras de hora por parte del día, que se detienen al deslizar. La comparten agendar y el editor de franjas |
| Barra de acción al pie | `BottomActionBar` | El botón principal fijo al pie, fuera del contenido que se desplaza |
| Círculos del fondo | `FloatingCircles` | Los dos círculos y el anillo que derivan detrás de Inicio |
| Entrada en cascada | `StaggeredEntrance` | Cada bloque de Inicio sube un paso después del de arriba |

## Marca y tema

| Español | Inglés | Uso |
|---|---|---|
| Marca | `brand` | Logo, nombre dibujado y colores: `ic_brand_mark`, `ic_brand_wordmark`, `BrandColors` |
| Botón de marca | `BrandColors.button` | El turquesa del botón de ingreso, igual en claro y oscuro |
| Animación de arranque | `intro` / `playIntro` | Al abrir la aplicación, nunca al volver del ingreso |
| Línea cardio | `HeartbeatTrace` | La línea bajo el nombre en el arranque, con sus ondas P, QRS y T |
| Barrido | `Sweep` | Hasta dónde llegó la cabeza de la línea, y si es una pasada repetida |
| Latido del logo | `beatScale` | El pulso doble del logo cuando la línea cruza el pico |
| Marca asentada del arranque | `SettledStartupBrand` / `settledStartupBrand` | Dónde deja el cuadro final del arranque el logo, el nombre y la línea |
| Entrada de la bienvenida | `entrance` / `entranceFrom` | La marca va desde donde la dejó el arranque hasta su lugar en la bienvenida |
| Puntos de valor | `WelcomeValues` | Lo que la bienvenida promete y la aplicación cumple: verificación, cercanía y rol doble |
| Píldora de disponibilidad | `AvailabilityPill` | El punto y «Disponible ahora» sobre el contenedor positivo, en la tarjeta y la ficha |
| Contenedor positivo | `positiveContainer` / `onPositiveContainer` | El verde suave de lo disponible y lo verificado, y el texto encima |
| Cifras de la ficha | `Stats` / `Stat` | Calificación, años de experiencia y atenciones en la cabecera de la ficha |
| Profesión y especialidad | `occupation` | La línea que une tipo de profesional y especialidad bajo el nombre |
| Grosor del borde | `borderWidth` | Medida de `Spacing`: el trazo de un control que se lee como campo |
| Ancho de la burbuja | `bubbleMaxWidthFraction` | Fracción de `Spacing`: lo que ocupa una burbuja en su fila, para que el lado diga quién escribió |
| Burbuja de mensaje | `myBubble` / `theirBubble` | Formas de `ExtraShapes`: el radio del botón, con la esquina del lado de quien escribió más cerrada |

## Atributos frecuentes

| Español | Inglés |
|---|---|
| Nombre completo | `fullName` / `full_name` |
| Correo electrónico | `email` |
| Teléfono | `phone` |
| Fotografía | `photoUrl` / `photo_url` |
| Rol | `role` |
| Rol activo | `activeRole` / `active_role` |
| Roles que tiene | `heldRoles` |
| Base profesional | `isProfessionalBase` / `is_professional_base` |
| Estado de verificación | `verificationStatus` / `verification_status` |
| Activo | `active` |
| Fecha de nacimiento | `birthDate` / `birth_date` |
| Contacto de emergencia | `emergencyContact` / `emergency_contact` |
| Notas médicas | `medicalNotes` / `medical_notes` |
| Especialidad | `specialty` |
| Biografía | `biography` |
| Tarifa base | `baseRateBob` / `base_rate_bob` |
| Años de experiencia | `yearsOfExperience` / `years_of_experience` |
| Radio de cobertura | `coverageRadiusKm` / `coverage_radius_km` |
| Disponible ahora | `availableNow` / `available_now` |
| Total de servicios | `totalServices` / `total_services` |
| Alias | `alias` |
| Texto de dirección | `addressText` / `address_text` |
| Referencia | `reference` |
| Ubicación | `location` |
| Ciudad | `city` |
| Principal | `isPrimary` / `is_primary` |
| Tipo de documento | `documentType` / `document_type` |
| Ruta de almacenamiento | `storagePath` / `storage_path` |
| Revisado por | `reviewedBy` / `reviewed_by` |
| Revisado en | `reviewedAt` / `reviewed_at` |
| Motivo de rechazo | `rejectionReason` / `rejection_reason` |
| Precio de referencia | `referencePriceBob` / `reference_price_bob` |
| Duración estimada | `estimatedDurationMin` / `estimated_duration_min` |
| Día de la semana | `dayOfWeek` / `day_of_week` |
| Hora de inicio / fin | `startTime` / `endTime` |
| Modalidad | `modality` |
| Fecha programada | `scheduledAt` / `scheduled_at` |
| Descripción | `description` |
| Presupuesto sugerido | `suggestedBudgetBob` / `suggested_budget_bob` |
| Estado | `status` |
| Expira en | `expiresAt` / `expires_at` |
| Oferta padre | `parentOfferId` / `parent_offer_id` |
| Monto | `amountBob` / `amount_bob` |
| Emisor | `issuer` |
| Monto final | `finalAmountBob` / `final_amount_bob` |
| Inicio / fin de la atención | `startedAt` / `finishedAt` |
| Monto total | `totalAmountBob` / `total_amount_bob` |
| Método de pago | `method` |
| Proveedor | `provider` |
| Referencia externa | `externalReference` / `external_reference` |
| Confirmado por el paciente | `patientConfirmedAt` / `patient_confirmed_at` |
| Confirmado por el profesional | `professionalConfirmedAt` / `professional_confirmed_at` |
| En disputa desde | `disputedAt` / `disputed_at` |
| Resuelto en | `resolvedAt` / `resolved_at` |
| Resuelto por | `resolvedBy` / `resolved_by` |
| Motivo de cancelación | `cancellationReason` / `cancellation_reason` |
| Cancelado por | `cancelledBy` / `cancelled_by` |
| Cancelado en | `cancelledAt` / `cancelled_at` |
| Autor | `authorId` / `author_id` |
| Destinatario | `recipientId` / `recipient_id` |
| Puntaje | `rating` |
| Comentario | `comment` |
| Visible | `visible` |
| Contenido | `content` |
| Leído en | `readAt` / `read_at` |
| Remitente | `senderId` / `sender_id` |
| Plataforma | `platform` |
| Último uso | `lastUsedAt` / `last_used_at` |
| Creado en | `createdAt` / `created_at` |
| Notas del profesional | `professionalNotes` — **no existe y no se crea**: ver la nota de abajo |

**`professionalNotes` no es una columna.** Lo realizado en una atención vive en la
conversación (`is_work_record`). Una columna de notas en `services` sería el «campo
suelto para salir del paso» que `docs/decisions.md` prohíbe el 2026-09-08, y
comprometería el registro clínico estructurado (FA-01). El nombre se reserva por si esa
tabla llega a existir.

## Tipos enumerados

`SCREAMING_SNAKE_CASE`, igual en Kotlin y en PostgreSQL.

| Tipo | Valores |
|---|---|
| `user_role` | `PATIENT`, `PROFESSIONAL`, `ADMIN` |
| `review_status` | `PENDING`, `APPROVED`, `REJECTED` |
| `professional_type` | `DOCTOR`, `NURSE`, `PHYSIOTHERAPIST`, `STUDENT` |
| `document_type` | `ID_FRONT`, `ID_BACK`, `SELFIE`, `DEGREE`, `LICENSE`, `STUDENT_CARD`, `OTHER` |
| `request_modality` | `IMMEDIATE`, `SCHEDULED` |
| `request_status` | `PUBLISHED`, `NEGOTIATING`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `EXPIRED` |
| `offer_issuer` | `PROFESSIONAL`, `PATIENT` |
| `offer_status` | `PROPOSED`, `ACCEPTED`, `REJECTED`, `EXPIRED` |
| `service_status` | `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `payment_method` | `CASH`, `DIRECT_QR`, `GATEWAY` |
| `payment_status` | `PENDING`, `PATIENT_CONFIRMED`, `PROFESSIONAL_CONFIRMED`, `BOTH_CONFIRMED`, `DISPUTED`, `RESOLVED_PAID`, `RESOLVED_NOT_PAID`, `CANCELLED` |
| `device_platform` | `ANDROID`. `IOS` se retiró el 2026-09-12 (FA-04) |

## Esquema

**Tablas, diecisiete:** `profiles` · `profile_roles` · `patients` · `professionals` ·
`verification_documents` · `addresses` · `service_types` · `professional_services` ·
`availability_slots` · `service_requests` · `request_offers` · `messages` · `services` ·
`payments` · `reviews` · `device_tokens` · `platform_settings`, la fila única de lo
configurable.

**Vistas:**
- `professional_directory`: la proyección pública del profesional, sin contacto. Es lo
  único que la contraparte lee de un profesional (`docs/decisions.md`, 2026-10-09).
- `professional_reviews`: los comentarios recibidos **como profesional** y nada más.
  `reviews` responde distinto al dueño de la ficha, porque `reviews_select_own` es una
  segunda política permisiva (`docs/decisions.md`, 2026-10-11).
- `my_addresses`: las direcciones propias, con el punto en latitud y longitud.
- `my_roles`: los roles propios y el rol activo, en una fila.
- `my_service_requests`: las solicitudes propias con `is_open`, `has_attention` y
  `open_offer_count`.
- `document_review_subject_queue` y `document_review_profiles`: las dos lecturas del
  administrador en la revisión de documentos.

## Sufijo de moneda

Toda columna y propiedad monetaria lleva el sufijo `Bob` o `_bob`, porque el sistema
opera en bolivianos y una fase posterior podría incorporar otra moneda. Nunca `amount` a
secas para un valor monetario.

## Nombres retirados

No se vuelven a usar. Cada uno dice qué lo reemplazó y cuándo.

| Retirado | Lo reemplaza | Cuándo |
|---|---|---|
| `MakeOfferScreen` | `OfferThreadUiState.Opening`, en la pantalla del hilo | HT-24 |
| `NegotiationScreen` | `OfferCard` y el hilo | HT-24 |
| `CreateRequestScreen` | `ImmediateCare` | HT-23 |
| `immediate_inbox` (RPC) | `request_inbox`. El tema de Broadcast `immediate_inbox` sigue | HU-16 |
| `DatePickerRow` | `DayCarousel` | HU-16 |
| `AccountScreen` / `AccountRoute` | `HomeShell` / `HomeRoute` | HT-25 |
| `FloatingNavigationBar`, que nunca se usó | `BottomNavigationBar` | HT-25 |
| `service_requests_select_addressee` | `professional_may_offer_on` | HU-17 |
| `professionals_select_counterpart` | `professional_directory` | HU-11 |
| Token de registro de FCM | FID (`installationId`) | HU-20 |
| `Spacing.filterChipMaxWidth` | Ya no existe en `Spacing.kt` | — |
| `platformFee`, `platformFeeBob`, `platform_fee_percent` | Nada: la plataforma no retiene comisión; se financia con la suscripción del profesional (INV-10, INV-18) | HT-30 |
| `professionalAmountBob`, `professional_amount_bob` | `totalAmountBob`: el pago es íntegro del profesional | HT-30 |
| `settledAt`, `settled_at`, `PaymentStatus.SETTLED` | Nada: no hay liquidación de comisiones | HT-30 |

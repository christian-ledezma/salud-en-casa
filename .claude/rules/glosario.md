# Glosario de dominio — español a inglés

Se carga al trabajar con archivos Kotlin o SQL.

**Regla.** Todo identificador de código y de esquema se escribe en inglés. Nunca
transliterar. No usar `Paciente`, `Solicitud`, `Calificacion`, `Monto`. Si un
término del dominio no figura en esta tabla, agregarlo aquí en el mismo commit
que lo introduce.

## Actores y roles

| Español | Inglés | Uso |
|---|---|---|
| Perfil | `Profile` | Identidad común a toda persona |
| Paciente | `Patient` | Rol que solicita atención |
| Profesional | `Professional` | Rol que presta atención |
| Administrador | `Admin` | Rol de verificación y conciliación |
| Roles de una persona | `ProfileRoles` | Los roles que tiene y cuál está activo. Una persona puede tener paciente y profesional a la vez |
| Médico | `DOCTOR` | Valor de `professional_type` |
| Enfermera | `NURSE` | Valor de `professional_type` |
| Fisioterapeuta | `PHYSIOTHERAPIST` | Valor de `professional_type` |
| Estudiante | `STUDENT` | Valor de `professional_type` |

## Conceptos del negocio

| Español | Inglés | Uso |
|---|---|---|
| Solicitud de atención | `ServiceRequest` | Petición publicada por el paciente |
| Oferta | `RequestOffer` | Propuesta de monto sobre una solicitud. Desde HU-17 leída por el paciente con el nombre y la calificación públicos del profesional (vía `my_request_offers`) |
| Contraoferta | Oferta con `parentOfferId` | No es un tipo aparte |
| Hilo de la negociación | `OfferThread` / `OfferThreadRoute` | La cadena de ofertas de un profesional sobre una solicitud, en el orden en que se escribió, leída y contestada por los dos lados en la misma pantalla, que desde HT-24 es también donde el profesional hace la primera oferta. Qué lado es el lector lo decide su identificador contra `professionalId`, que INV-16 mantiene inequívoco (HU-18) |
| Turno del hilo | `isMyTurn` / `offer_continues_thread` | Se contesta la oferta que está sobre la mesa: la que sigue en `PROPOSED` y todavía no tiene respuesta. El definidor recibe además el emisor que el padre debe tener, de modo que cada rama de la política valida el suyo |
| Hilo cerrado | `isClosed` / `guard_offer_rejection` | Un rechazo cierra la cadena: nadie contesta una oferta rechazada, y nadie rechaza una ya contestada. El profesional rechazado tampoco abre otra cadena, porque solo tiene una primera oferta por solicitud |
| Rechazar una oferta | `RejectOfferUseCase` / `request_offers_reject_counterparty` | Marca esa oferta `REJECTED` y nada más: la solicitud se queda en `NEGOTIATING` y los demás profesionales la siguen viendo. Rechaza la contraparte del emisor. Una política que no admite la fila actualiza cero filas sin lanzar, así que cero se lee como denegación |
| Ya ofertó | `professional_already_offered_on` | Definidor que responde si el llamante ya tiene una fila en esa solicitud. Una primera oferta por profesional y solicitud; va en un definidor para no releer `request_offers` dentro de su propia política. Dos índices únicos parciales —uno por `parent_offer_id`, otro por `(request_id, professional_id)` sin padre— son los que sostienen la regla cuando hay dos escritores a la vez |
| Reloj de la oferta | `stamp_offer_created_at` | Disparador que sella `created_at` con la hora del motor cuando hay un llamante autenticado. El hilo se lee en el orden que el motor registró, así que la fecha no la pone el cliente |
| Estado de la oferta | `OfferStatus` | `PROPOSED`, `ACCEPTED`, `REJECTED`, `EXPIRED`. Lo único que el cliente escribe de una oferta emitida, y solo de `PROPOSED` a `REJECTED` |
| Aviso de contraoferta | `COUNTER_OFFER_RECEIVED` / `notify_offer_parties` | El único evento que llega a cualquiera de los dos roles: siempre a la contraparte del emisor. `parent_offer_id` nulo es `OFFER_RECEIVED`; con padre, contraoferta. Lleva el monto y el `professionalId` del hilo, nunca el mensaje (RF-09.10) |
| Tema del profesional | `professional_offers` | El Broadcast sin datos al tema privado `professional_offers:<id del profesional>`, espejo del del paciente, con su política `professional_offers_listen`. Un disparador avisa al tema de quien tiene que re-consultar, también al rechazar. En el cliente, cada tema es un solo flujo compartido entre las pantallas que lo escuchan (HT-24) |
| Puede ofertar | `professional_may_offer_on` | Función de definidor que responde si el llamante puede ofertar sobre una solicitud, sin devolver ubicación ni identidad. Reemplazó la lectura que hacía `request_offers_insert_participants`, de modo que `service_requests_select_addressee` se retiró (HU-17). La rama agendada decide por `professional_id`, no por cobertura |
| Ofertas de mi solicitud | `my_request_offers` | Función de definidor que da las ofertas de la propia solicitud del paciente, unidas al directorio público del profesional |
| Solicitud para ofertar | `OfferableRequest` / `inbox_request` | La solicitud que el profesional abre para ofertar: la proyección de la bandeja para una fila, más si ya ofertó. Nunca la ubicación exacta ni la identidad del paciente |
| Primera oferta | `OfferThreadUiState.Opening` | El estado de la pantalla única del profesional antes de ofertar: el detalle de la solicitud y el formulario. Al enviar, la misma pantalla pasa a ser la negociación. Reemplazó a `MakeOfferScreen` en HT-24 |
| Tarjeta de oferta | `OfferCard` | Un profesional con su oferta como lo ve el paciente mientras espera, apiladas como en InDrive: nombre, calificación, distancia, el monto sobre la mesa y de quién es el turno. Tocarla abre el hilo. Reemplazó a `NegotiationScreen` en HT-24 |
| Mínimo del paciente | `counterFloorBob` / `guard_patient_counter_floor` | El paciente nunca contraoferta por debajo del mayor entre su presupuesto sugerido y su propuesta más alta en ese hilo. Por hilo, solo para el paciente; lo dice la pantalla y lo exige el motor, que solo lo evalúa para el dueño de la solicitud |
| Dónde se negocia | `NegotiationEntry` | A dónde lleva una tarjeta de «Mis solicitudes»: la espera, si es una inmediata abierta, o el hilo, si es una agendada en negociación. Nada, si no queda qué negociar |
| Dirección elegida al volver | `selectionAfterRefresh` | Al volver de «Agregar otra dirección», queda elegida la que se acaba de agregar; si no hay una nueva, la que estaba; si se borró, la principal |
| Aviso de oferta al paciente | `patient_offers` / `notify_patient_offers` | El Broadcast sin datos al tema privado `patient_offers:<id del paciente>` que dispara un trigger al insertarse una oferta, para que el paciente re-consulte. La política `patient_offers_listen` deja a cada paciente escuchar solo su propio tema |
| Encolar el aviso de oferta | `queue_offer_push` / `OFFER_RECEIVED` | El trigger en `request_offers` (solo si el emisor es el profesional) que encola el push `OFFER_RECEIVED` con el tipo de servicio y el monto, nunca el mensaje |
| Pasar a negociación | `move_request_to_negotiating` | El trigger que mueve la solicitud de `PUBLISHED` a `NEGOTIATING` al insertarse la primera oferta. El guardia de ciclo de vida admite esa transición |
| Atención / Servicio | `Service` | Atención acordada y prestada |
| Tipo de servicio | `ServiceType` | Catálogo de tipos de atención |
| Servicio ofrecido | `ProfessionalService` | Unión profesional–tipo de servicio, con el precio que el profesional declaró |
| Servicios declarados | `DeclaredServices` | Lo que el profesional presta hoy junto con el catálogo que le queda por declarar |
| Tipos por declarar | `undeclaredTypes` | Los tipos del catálogo que el profesional todavía no ofrece. No se le ofrecen dos veces |
| Franja de disponibilidad | `AvailabilitySlot` | Horario declarado por el profesional |
| Disponibilidad semanal | `WeeklyAvailability` | La vista agrupada por día que la pantalla de disponibilidad maneja: las siete claves siempre vienen pobladas aun sin franjas |
| Borrador de franja | `SlotDraft` / `SlotEditor` | La franja que se está declarando y el editor que la muestra, dentro del día al que pertenece. Sin diálogo y sin selector de reloj: el día lo fija el botón desde el que se abrió (`docs/decisions.md`, 2026-10-15) |
| Rejilla de medias horas | `SlotTimes` | Las horas que el editor ofrece: cada 30 minutos, y las de «Hasta» siempre posteriores al inicio elegido |
| Error de disponibilidad | `AvailabilityError` | `EndNotAfterStart`, `OverlapsExistingSlot`, `NetworkUnavailable`, `Unexpected`. El solapamiento y el duplicado exacto llegan por el mismo errcode 23505 y presentan igual |
| Guarda contra solapamientos | `reject_overlapping_slot` | Función del motor que, en un `BEFORE INSERT/UPDATE`, usa `OVERLAPS` sobre `time` para rechazar una franja que cruce otra del mismo profesional en el mismo día |
| Dirección | `Address` | Domicilio georreferenciado |
| Documento de verificación | `VerificationDocument` | Respaldo de identidad o título |
| Documento adicional | `DocumentType.OTHER` | Adjunto opcional por persona, con leyenda obligatoria |
| Checklist de verificación | `VerificationChecklist` | Documentos requeridos según rol más el adjunto opcional |
| Leyenda del documento | `caption` / `DocumentCaption` | Línea de texto que acompaña al `OTHER` |
| Pago | `Payment` | Registro económico de una atención |
| Comisión de la plataforma | `platformFee` | Porción que retiene la plataforma |
| Calificación | `Review` | Puntaje y comentario cruzados |
| Comentario de la ficha | `ProfessionalReview` | Una calificación tal como la ficha pública la muestra: puntaje, comentario y fecha. **Sin su autor**, que no se publica |
| Proyección pública de calificaciones | `professional_reviews` | La vista que decide cuáles son públicas. El cliente nunca consulta `reviews` |
| Servicios de otro profesional | `ProfessionalServicesResult` | Lo que un visitante lee del catálogo ajeno: lo declarado, sin los tipos por declarar |
| Mensaje | `Message` | Comunicación dentro de la solicitud |
| Identificador de dispositivo | `DeviceToken` | Destino de notificaciones |
| Reputación | `averageRating` | Promedio de calificaciones recibidas |
| Total de calificaciones | `totalReviews` | Cuántas calificaciones sostienen el promedio |
| Distancia en metros | `distanceM` | Resultado de la búsqueda por cercanía, redondeada a la centena de metros antes de salir del motor |
| Búsqueda por cercanía | `ProfessionalSearch` | La característica entera: `features/search/` y la función `search_nearby_professionals` |
| Profesional cercano | `NearbyProfessional` | Una fila del resultado de la búsqueda: lo público del profesional más su distancia y el punto aproximado de su base |
| Criterios de búsqueda | `SearchCriteria` | Radio, tipo de servicio y disponibilidad con que el paciente recorta la búsqueda |
| Origen de la búsqueda | `SearchOrigin` | La dirección **principal** del paciente, desde la que se mide la distancia. Nunca su base profesional |
| Radio de búsqueda | `SearchRadius` | Las tres opciones que el paciente puede pedir: 2, 5 y 10 km |
| Punto aproximado de la base | `basePoint` / `base_latitude`, `base_longitude` | Dónde se dibuja el marcador: la base profesional redondeada a la manzana. Nunca la coordenada exacta |
| Bandeja de solicitudes | `request_inbox` / `InboxRequest` | Las solicitudes inmediatas publicadas dentro del radio del profesional y las agendadas dirigidas a él, leídas por la RPC de definidor. Cada fila lleva tipo, ciudad, presupuesto, modalidad, la hora de la cita si la hay y la distancia redondeada, nunca la ubicación exacta ni la identidad del paciente. Reemplazó a `immediate_inbox` en HU-16 |
| Cita agendada | `RequestModality.SCHEDULED` / `create_scheduled_request` | Solicitud dirigida a un profesional para una fecha y hora. Solo nace por la función de servidor, que valida la hora contra `bookable_start_times` |
| Horarios reservables | `bookable_start_times` / `BookableTimes` / `BookableStart` | Las horas de inicio que un profesional ofrece para un servicio en los próximos 14 días: cada 30 minutos desde el inicio de la franja, con 2 horas de anticipación y la atención completa dentro de una franja. Cada inicio trae el instante y su fecha y hora en La Paz |
| Parte del día | `PartOfDay` | `MORNING` antes de las 12:00, `AFTERNOON` hasta las 18:00, `EVENING` desde las 18:00. Agrupa las horas del carrusel. Vive en `core/vo/`: lo usan agendar y disponibilidad |
| Atención ahora | `ImmediateCare` / `ImmediateCareRoute` | La pantalla de la inmediata desde HT-23: el mapa de disponibles, el formulario y la espera de ofertas en una sola. Reemplazó a `CreateRequestScreen` |
| Disponibles cerca | `NearbyState` / `FindProfessionalsAvailableNowUseCase` | Quiénes muestra el mapa de «Atención ahora»: disponibles ahora, que prestan el servicio elegido, en el radio mayor, alrededor de la dirección elegida. Indicativo, no la lista de destinatarios |
| Solicitud inmediata activa | `ActiveRequestResult` / `GetActiveImmediateRequestUseCase` | La inmediata más reciente del paciente que sigue publicada o en negociación: `Found`, `None` o `Failure` |
| Selector desplegable | `ExpandableOptionPicker` | La lista que se abre para elegir entre muchas opciones de nombre largo, como los tipos de servicio |
| Agendar una cita | `ScheduleAppointment` | La pantalla y su modelo de vista; se abre desde la ficha del profesional |
| Zona de la cita | `AppointmentZone` | `America/La_Paz`. Solo para mostrar una `scheduled_at` guardada; el cálculo vive en el motor |
| Puede agendar | `CanScheduleAppointmentUseCase` / `showsScheduleAction` | Si la ficha ofrece «Agendar cita»: rol activo paciente, ficha ajena y profesional con servicios declarados |
| Carrusel de días | `DayCarousel` | Fila de tarjetas de día que se detiene en cada una al deslizar; reemplazó a `DatePickerRow` |
| Fila de horas | `TimeChipRow` | Fila de píldoras de hora, agrupadas por parte del día, que se detiene en cada una al deslizar. La comparten la pantalla de agendar y el editor de franjas |
| Barra de acción al pie | `BottomActionBar` | El botón principal fijo al pie, fuera del contenido que se desplaza |
| Aviso de la bandeja | `inboxSignals` / `notify_immediate_inbox` | El Broadcast sin datos que dispara un trigger al tema `immediate_inbox` para que el cliente re-consulte. No transporta la fila |
| Notificación push | `PushPayload` / `features/notifications/` | Lo que llega al teléfono: el evento, a quién y lo necesario para abrirlo. Nunca texto escrito por un usuario (RF-09.10). El texto lo compone el teléfono |
| Evento notificado | `NotificationEvent` | `REQUEST_NEARBY`, `APPOINTMENT_SCHEDULED`, `APPOINTMENT_CANCELLED`, `REQUEST_EXPIRED`, `OFFER_RECEIVED`, `COUNTER_OFFER_RECEIVED`. Los mismos nombres en SQL y en Kotlin; `NotificationEventParityTest` lo vigila, y solo mira la última migración que define `request_push_body`, así que esa función se reescribe entera cada vez |
| Sujeto del aviso | `PushSubject` | La solicitud, el destinatario, el rol con el que recibe y el tipo de servicio |
| Destino al tocar | `NotificationDestination` | `Inbox`, `MyRequests` u `OfferThread(requestId, professionalId)`. Una solicitud para el profesional abre la solicitud con `professionalId` nulo; una oferta o una contraoferta, el hilo de ese profesional. El rol con el que se recibe se activa antes de abrir (HT-24) |
| Avisos mostrados | `ShownNotificationsDataSource` | Los que siguen en la bandeja del sistema. Se retiran al cerrar sesión |
| Identificador de instalación | FID / `installationId` | Lo que guarda `device_tokens.token` desde HU-20: el ID de instalación de Firebase, que reemplazó al token de registro deprecado |
| Registrar el dispositivo | `register_my_device` / `unregister_my_device` | Las dos únicas vías de escritura de `device_tokens`. El registro le pasa el FID a la última cuenta que ingresó en el teléfono |
| Destinatarios de la inmediata | `immediate_request_recipients` | La regla de `request_inbox` más `available_now`. Solo la llama el motor |
| Cuerpo del envío | `request_push_body` | Arma el evento, los identificadores y la lista de destinatarios con sus FID. Lo encola `queue_request_push` con `pg_net` hacia la función `send-push` |
| Cobertura de un profesional dado | `professional_covers_for` | La regla de cobertura escrita una vez. Ningún cliente puede ejecutarla: con un profesional y un punto libres sería un oráculo sobre su base |
| Canal de notificación | `NotificationChannelSpec` | Un canal de Android por categoría. Cada historia agrega el de su evento; hoy solo `REQUESTS` |
| Explicación del permiso | `NotificationPermissionPrompt` / `NotificationRationale` | La tarjeta que explica para qué sirven los avisos antes de pedir el permiso. Se muestra una sola vez |
| Alcance de la aplicación | `ApplicationScope` | El `CoroutineScope` que sobrevive a las pantallas, donde el servicio de mensajería deja su trabajo |
| Cola de revisión | `PendingReviewSubject` | Persona con trabajo pendiente del administrador: documentos por revisar, la verificación por conceder, o las dos |
| Listo para verificar | `awaitingVerification` / `awaiting_verification` | Profesional con todos sus requeridos aprobados que todavía no es `APPROVED` |
| Esperando desde | `waitingSince` / `waiting_since` | Cuándo el turno pasó al administrador: la subida del documento o la aprobación del último requerido |
| Quién espera la verificación | `professionals_awaiting_verification` | Función del motor, solo para el administrador, que dice a quién le falta el acto de verificar |
| Criterios de la cola | `PendingReviewQuery` | Búsqueda, filtro por rol y orden con que el administrador recorta la cola |
| Filtro por rol | `ReviewRoleFilter` | `ALL`, `PATIENT` o `PROFESSIONAL` |
| Orden de la cola | `ReviewQueueOrder` | `OLDEST_FIRST` o `NEWEST_FIRST`, por la fecha del documento más antiguo |
| Documentos marcados | `checkedTypes` | Los que el administrador seleccionó para un veredicto conjunto |
| Visor a pantalla completa | `FullScreenDocumentImage` | El documento abierto sobre fondo oscuro, con zoom y arrastre |
| Transformación de la imagen | `ImageTransform` | La escala y el desplazamiento del visor, con su recorte |
| Marca | `brand` | El logo, el nombre dibujado y sus colores: `ic_brand_mark`, `ic_brand_wordmark`, `BrandColors` |
| Animación de arranque | `intro` / `playIntro` | Se reproduce al abrir la aplicación, nunca al volver del ingreso |
| Línea cardio | `HeartbeatTrace` | La línea que se traza bajo el nombre en el arranque, con sus ondas P, QRS y T |
| Barrido | `Sweep` | Hasta dónde llegó la cabeza de la línea, y si es una pasada repetida |
| Latido del logo | `beatScale` | El pulso doble del logo cuando la línea cruza el pico |
| Entrada de la bienvenida | `entrance` / `entranceFrom` | La marca va desde donde la dejó el arranque hasta su lugar en la bienvenida |
| Marca asentada del arranque | `SettledStartupBrand` / `settledStartupBrand` | Dónde deja el cuadro final del arranque el logo, el nombre y la línea |
| Puntos de valor | `WelcomeValues` | Lo que la bienvenida promete y la aplicación ya cumple: verificación, cercanía y rol doble |
| Botón de marca | `BrandColors.button` | El turquesa del botón de ingreso, el mismo en claro y en oscuro |
| Píldora de disponibilidad | `AvailabilityPill` | El punto y «Disponible ahora» sobre el contenedor positivo, en la tarjeta y en la ficha |
| Contenedor positivo | `positiveContainer` / `onPositiveContainer` | El fondo verde suave de lo disponible y lo verificado, y el texto que va encima |
| Cifras de la ficha | `Stats` / `Stat` | Calificación, años de experiencia y atenciones realizadas en la cabecera de la ficha |
| Profesión y especialidad | `occupation` | La línea que une el tipo de profesional y su especialidad bajo el nombre |
| Expediente de revisión | `DocumentReviewDossier` | La persona revisada, sus documentos y su lista de requeridos |
| Veredicto | `verdict` | Aprobación o rechazo de un documento por el administrador |
| Revisado | `DocumentReviewSubject` | La persona cuyos documentos revisa el administrador |
| Verificar al profesional | `approveProfessional` | Acto del administrador que lleva a `APPROVED` al profesional |
| Motivo de rechazo | `RejectionReason` | Objeto de valor del texto que el usuario lee al ser rechazado un documento |
| Documentos requeridos | `required_document_types` | Función del motor que dice qué debe tener aprobado una persona |

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
| Tope de ancho de una ficha | `filterChipMaxWidth` | Medida de `Spacing`: sin ella una etiqueta del catálogo no tiene ancho dentro del que envolver |
| Grosor del borde | `borderWidth` | Medida de `Spacing`: el trazo de un control que se lee como campo |
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
| Notas del profesional | `professionalNotes` / `professional_notes` |
| Monto total | `totalAmountBob` / `total_amount_bob` |
| Comisión | `platformFeeBob` / `platform_fee_bob` |
| Monto del profesional | `professionalAmountBob` / `professional_amount_bob` |
| Método de pago | `method` |
| Proveedor | `provider` |
| Referencia externa | `externalReference` / `external_reference` |
| Conciliado en | `settledAt` / `settled_at` |
| Confirmado por el paciente | `patientConfirmedAt` / `patient_confirmed_at` |
| Confirmado por el profesional | `professionalConfirmedAt` / `professional_confirmed_at` |
| Motivo de cancelación | `cancellationReason` / `cancellation_reason` |
| Cancelado por | `cancelledBy` / `cancelled_by` |
| Cancelado en | `cancelledAt` / `cancelled_at` |
| Autor | `authorId` / `author_id` |
| Leyenda | `caption` / `caption` |
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

## Tipos enumerados

Los valores se escriben en `SCREAMING_SNAKE_CASE`, tanto en Kotlin como en PostgreSQL.

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
| `payment_status` | `PENDING`, `PATIENT_CONFIRMED`, `BOTH_CONFIRMED`, `SETTLED`, `DISPUTED` |
| `device_platform` | `ANDROID`, `IOS` |

## Nota sobre `professionalNotes`

El término figura arriba, pero **no existe como columna en el esquema y no debe
crearse**. El detalle de lo realizado durante una atención vive en la
conversación de la solicitud. Una columna de notas en `services` sería
exactamente el «campo suelto para salir del paso» que `docs/decisions.md`
prohíbe el 2026-09-08: comprometería la incorporación posterior del registro
clínico estructurado (FA-01), que exige autoría, fecha y semántica de solo
agregar. El término se conserva aquí por si esa tabla llega a existir.

## Tablas del esquema

`profiles` · `profile_roles` · `patients` · `professionals` ·
`verification_documents` · `addresses` · `service_types` ·
`professional_services` · `availability_slots` · `service_requests` ·
`request_offers` · `messages` · `services` · `payments` · `reviews` ·
`device_tokens`

Dieciséis tablas. `profile_roles` se agregó en el Sprint 2.5, al dejar de ser
único el rol de una persona.

`professional_directory` es además **lo único que la contraparte lee de un profesional**:
`professionals_select_counterpart` se retiró en HU-11, de modo que nadie lee la fila completa
—con su `verification_status`— por compartir un servicio (`docs/decisions.md`, 2026-10-09).

Vistas de apoyo: `professional_directory`, la proyección pública del profesional
sin datos de contacto · `professional_reviews`, los comentarios que la ficha
muestra, que son los recibidos **como profesional** y nada más: la tabla `reviews`
responde distinto al dueño de la ficha que a un paciente, porque `reviews_select_own`
es una segunda política permisiva (`docs/decisions.md`, 2026-10-11) · `my_addresses`, las direcciones propias con el punto ya
proyectado a latitud y longitud · `my_roles`, los roles propios y el rol activo
en una sola fila · `document_review_subject_queue`, una fila por persona con
documentos pendientes, y `document_review_profiles`, las dos lecturas del
administrador en la revisión de documentos.

## Sufijo de moneda

Toda columna y propiedad monetaria lleva el sufijo `Bob` o `_bob`, porque el
sistema opera en bolivianos y una fase posterior podría incorporar otra moneda.
Nunca usar `amount` a secas para un valor monetario.

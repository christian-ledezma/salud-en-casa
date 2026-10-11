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
| Oferta | `RequestOffer` | Propuesta de monto sobre una solicitud |
| Contraoferta | Oferta con `parentOfferId` | No es un tipo aparte |
| Atención / Servicio | `Service` | Atención acordada y prestada |
| Tipo de servicio | `ServiceType` | Catálogo de tipos de atención |
| Servicio ofrecido | `ProfessionalService` | Unión profesional–tipo de servicio, con el precio que el profesional declaró |
| Servicios declarados | `DeclaredServices` | Lo que el profesional presta hoy junto con el catálogo que le queda por declarar |
| Tipos por declarar | `undeclaredTypes` | Los tipos del catálogo que el profesional todavía no ofrece. No se le ofrecen dos veces |
| Franja de disponibilidad | `AvailabilitySlot` | Horario declarado por el profesional |
| Dirección | `Address` | Domicilio georreferenciado |
| Documento de verificación | `VerificationDocument` | Respaldo de identidad o título |
| Documento adicional | `DocumentType.OTHER` | Adjunto opcional por persona, con leyenda obligatoria |
| Checklist de verificación | `VerificationChecklist` | Documentos requeridos según rol más el adjunto opcional |
| Leyenda del documento | `caption` / `DocumentCaption` | Línea de texto que acompaña al `OTHER` |
| Pago | `Payment` | Registro económico de una atención |
| Comisión de la plataforma | `platformFee` | Porción que retiene la plataforma |
| Calificación | `Review` | Puntaje y comentario cruzados |
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
| Modo de resultados | `ResultsMode` | `MAP` o `LIST`, las dos formas de ver el mismo resultado |
| Punto aproximado de la base | `basePoint` / `base_latitude`, `base_longitude` | Dónde se dibuja el marcador: la base profesional redondeada a la manzana. Nunca la coordenada exacta |
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
sin datos de contacto · `my_addresses`, las direcciones propias con el punto ya
proyectado a latitud y longitud · `my_roles`, los roles propios y el rol activo
en una sola fila · `document_review_subject_queue`, una fila por persona con
documentos pendientes, y `document_review_profiles`, las dos lecturas del
administrador en la revisión de documentos.

## Sufijo de moneda

Toda columna y propiedad monetaria lleva el sufijo `Bob` o `_bob`, porque el
sistema opera en bolivianos y una fase posterior podría incorporar otra moneda.
Nunca usar `amount` a secas para un valor monetario.

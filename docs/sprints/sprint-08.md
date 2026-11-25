# Sprint 8 — Pago de la atención y vigencia del profesional

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-25 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.

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
**Corridos el 2026-11-25**, al cerrar: ver HT-30.

**Orden.** HT-30 primero, porque HU-25 y HU-26 leen el pago que deja. HU-38 al final:
no depende del pago de la atención, y así el ciclo de la atención queda cerrado antes
de tocar la visibilidad del profesional.

### HT-30 · Retirar la comisión del modelo `[x]` — 2026-11-23; último criterio cerrado el 2026-11-25 — 3 puntos

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
- [x] `supabase db reset` reconstruye la base y `db diff --linked` no muestra más
      diferencia que esta migración. **Pendiente por decisión del autor, 2026-11-23:**
      exige Docker y lo enciende cuando él decida. Vence al cerrar el Sprint 8, como
      pide `.claude/rules/supabase.md` para todo sprint que toca el esquema. **Cumplido
      el 2026-11-25**, al cerrar el sprint.

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

**Último criterio, 2026-11-25.** Con Docker encendido por el autor, al cerrar el sprint y ya
con las cuatro migraciones que siguieron a esta: `supabase start` aplicó las 61 migraciones
desde una base vacía, `supabase db reset`, sin `--linked`, las volvió a aplicar sin errores, y
`supabase db diff --linked --schema public` respondió «No schema changes found». Para
entonces el remoto ya tenía aplicada esta migración, así que la diferencia esperada era
ninguna, y ninguna hubo: el remoto coincide con la cadena completa. En la base reconstruida,
las 18 tablas tienen seguridad por fila y al menos una política, `payment_status` tiene sus
ocho estados y queda una sola versión de `request_push_body`.

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

### HU-38 · Saber hasta cuándo aparezco ante los pacientes `[x]` — 2026-11-25 — 8 puntos

> Como **profesional**, quiero **saber hasta cuándo está vigente mi suscripción y qué
> pierdo sin ella**, para **no dejar de recibir solicitudes sin enterarme**.

**Criterios de aceptación**

- [x] Dado que se aprueba mi verificación, cuando consulto mi suscripción, entonces
      tengo 30 días de prueba con su fecha de fin.
- [x] Dado que mi suscripción está vigente, cuando abro mi Inicio, entonces veo hasta
      cuándo.
- [x] Dado que el cobro está activado y mi suscripción venció, cuando un paciente
      busca o publica una solicitud en mi radio, entonces no aparezco, no la recibo en
      mi bandeja y no me llega su aviso.
- [x] Dado que mi suscripción venció, cuando intento una primera oferta, entonces se
      rechaza y la aplicación me dice por qué.
- [x] Dado que mi suscripción venció con una negociación abierta o una atención
      acordada, cuando sigo, entonces puedo contraofertar en ese hilo y avanzar la
      atención.
- [x] Dado que el cobro está desactivado, cuando un profesional aprobado y activo
      opera, entonces todo funciona como antes de esta historia.
- [x] Dado que soy paciente, también con los dos roles, cuando pido atención,
      entonces nada me pide una suscripción.

**Requisitos:** RF-14.1, RF-14.2, RF-14.3, RF-14.5, RF-14.6, RN-01, INV-07, INV-18.

**Decisiones del autor, 2026-11-24** (`docs/decisions.md`, «HU-38: la vigencia la decide el motor donde empieza algo
nuevo»):
- **Lo ya iniciado.** La bandeja del profesional vencido conserva las solicitudes en las que ya ofertó; solo se ocultan
  las nuevas.
- **La ficha.** Sigue visible; «Agendar cita» muestra el aviso de «sin horarios» que ya existía, y crear la cita se
  rechaza como `professional_unavailable`. El paciente no se entera de la suscripción del profesional.
- **El interruptor y el vencimiento, por SQL.** Se encienden y se restauran a mano para el recorrido y la demostración,
  y cada cambio se anota aquí.
- **Con el interruptor apagado, nada se muestra.**
- **«Mi suscripción».** Una pantalla propia, desde una fila del Inicio y otra de Configuración; HU-39 le agrega la
  compra.
- **La primera oferta.** Se rechaza con su motivo, y la bandeja del vencido lo avisa arriba.
- **La fila del Inicio.** Neutra mientras vale; con el tono de pendiente al vencer. Sin aviso previo (FA-20).

Prototipo aprobado: [Suscripción del profesional HU-38](https://claude.ai/artifact/QBPccCjWxWJTLnmL251S6v). Dos
diferencias con él, por consistencia con lo que ya existía: la fila vencida usa el tono de pendiente de
«Verificación pendiente» (el ícono ámbar sobre gris, no el fondo ámbar entero), y la fila de Configuración lleva el
estado con su fecha en el detalle y no en una píldora, porque al 200 % la píldora partía «Suscripción» a la mitad.

**Hallazgo de la planificación.** La propuesta era sumar la vigencia a las reglas que ya filtran al profesional, pero
`request_offers_insert_participants` llama a `professional_may_offer_on` en toda oferta del profesional, contraofertas
incluidas, y `professional_directory` sostiene la ficha, los nombres en las ofertas del paciente, los avisos y las
calificaciones. Ponerla ahí habría cortado las negociaciones abiertas (criterio 5). La vigencia va en
`professional_has_valid_subscription`, que solo consultan los puntos donde empieza algo nuevo.

**Experimentos SQL, 2026-11-24.** Contra el remoto, antes del `db push`, en una sola transacción que aplica
`20261125021500_hu38_professional_subscription.sql` completa y termina en `rollback`. Paciente `299073bc…`, profesional
`fcab94c0…` (con los dos roles), administrador `08ddb28f…` y un profesional `PENDING` con tipo y tarifa, `7f435cb7…`.
Dentro, el paciente publica dos inmediatas de Aplicación de inyectables desde su dirección principal: en la primera el
profesional oferta Bs 60 y el paciente contraoferta Bs 50; la segunda queda nueva. El vencimiento se simula corriendo
el fin de la prueba a la hora de la transacción. Después se confirmó que el remoto quedó intacto: sin
`subscription_periods`, una sola solicitud abierta, la atención de Bs 100 asignada y el profesional `7f435cb7…`
pendiente.

| # | Experimento | Resultado |
|---|---|---|
| 0 | La migración sobre los datos reales | Una prueba de 30 días para el único profesional aprobado; 1 período en total |
| A | Interruptor encendido, prueba vigente | La búsqueda lo encuentra; 66 horarios reservables; es destinatario del aviso de la nueva; su bandeja tiene las dos; `my_subscription` dice `TRIAL`; la primera oferta y la contraoferta del paciente se insertan |
| B1 | `aProfessionalWithoutAValidSubscriptionIsHiddenFromSearchAndInbox`, búsqueda | 0 filas |
| B2 | La misma, horarios y cita | 0 horarios; crear la cita en un horario que valía: `professional_unavailable` |
| B3 | La misma, aviso | No es destinatario de la nueva |
| B4 | La misma, bandeja | Solo la solicitud donde ya ofertó; la nueva no |
| B5 | El directorio | Sigue en `professional_directory`: la ficha y los nombres no cambian |
| B6 | `aProfessionalWithoutAValidSubscriptionCannotMakeAFirstOffer` | Rechazado: `subscription_expired`. La nueva todavía se abre por su identificador, donde se ve el motivo |
| B7 | `anOpenNegotiationAndAnAgreedAttentionSurviveAnExpiredSubscription`, contraoferta | La contraoferta de Bs 55 al hilo abierto se inserta |
| B8 | La misma, atención acordada | La atención asignada de Bs 100 pasa a en curso: 1 fila |
| B9 | `my_subscription` vencida | `EXPIRED` |
| F | `aPatientNeverNeedsASubscription`, con los dos roles | El mismo profesional vencido publica una inmediata como paciente |
| D1 | `noClientCanCreateExtendOrShortenASubscriptionPeriod`, insertar | Rechazado: `permission denied for table subscription_periods` |
| D2 | La misma, extender y borrar el propio | Rechazado las dos veces: `permission denied` |
| D3 | Preguntar la vigencia de otro | Rechazado: `permission denied for function professional_has_valid_subscription` |
| D4 | Lectura de los períodos | El profesional lee 1, el suyo; el paciente 0; el período sigue terminando cuando se lo dejó |
| C | `withTheSwitchOffEveryApprovedActiveProfessionalIsValid`, con el mismo profesional vencido | La búsqueda lo encuentra; 66 horarios; es destinatario; su bandeja tiene las dos; la primera oferta se inserta; `my_subscription` dice `false / EXPIRED` |
| E | `approvingAVerificationGrantsExactlyOneThirtyDayTrial` | La aprobación crea 1 prueba de 30 días; volver a `PENDING` y aprobar de nuevo deja 1 |
| G | `everyTableHasRowLevelSecurityEnabledAndAtLeastOnePolicy` | Ninguna tabla sin seguridad por fila o sin política |

**Verificación de código, 2026-11-24.** `./gradlew build` en verde: 1258 pruebas, 0 fallas, 0 omitidas (25 nuevas). Una
prueba nueva falló la primera vez por la aserción y no por la pantalla: el motivo de la primera oferta rechazada está
en la barra fija del pie, que no se desplaza, y la aserción correcta es la de lo que cabe sin desplazar.

**Aplicada, 2026-11-24.** `db push` de `20261125021500_hu38_professional_subscription.sql`; `send-push` no cambia.
Consulta de solo lectura después: `subscription_required` en falso, `trial_days` en 30, y un período `TRIAL` para
`chris.ledezma.s` del 24/11 22:56:44 al 24/12 22:56:44, hora de La Paz.

**Recorrido en emuladores, 2026-11-25.** Profesional `chris.ledezma.s` y paciente `ledezma.aramayo.73`. Con el
interruptor apagado, el Inicio y Configuración del profesional no mostraron nada de la suscripción y la paciente lo
encontró al agendar. Encendido por SQL, el Inicio mostró «Visible para pacientes» con el fin de la prueba, y «Mi
suscripción» y Configuración lo mismo. La paciente pidió Aplicación de inyectables, el profesional ofertó Bs 60 y ella
contraofertó Bs 50; después le agendó una cita. Con la prueba vencida por SQL a las 00:39:10, el Inicio pasó a «Tu
suscripción venció», «Mi suscripción» a «Vencida» y la bandeja mostró el aviso y solo la inmediata en negociación, sin
la cita. El profesional contraofertó Bs 55 en ese hilo, la paciente aceptó, y él marcó la llegada y la completó. La
paciente ya no lo encontró al agendar, publicó otra inmediata sin que a él le llegara nada y la canceló, y canceló la
cita. El profesional, como paciente, publicó una inmediata sin que nada le pidiera suscripción, y la canceló.

El criterio 4 se recorrió aparte: el autor había descartado el aviso de la cita y ya no había cómo abrir una solicitud
nueva. Con la prueba de vuelta vigente, la paciente publicó otra inmediata; el profesional abrió su formulario y lo dejó
abierto; se venció la prueba (00:55:09 y otra vez a las 00:57:52) y al tocar «Enviar oferta» apareció «Tu suscripción
venció: no puedes hacer ofertas nuevas. Las negociaciones que ya abriste siguen.» Es el caso de la suscripción que vence
con la pantalla abierta. Después se restauraron el período y el interruptor. El autor lo recorrió sin observaciones.

Después, en la base, a la hora de La Paz del 25/11:

| Solicitud | Quién la pidió | Estado | Ofertas |
|---|---|---|---|
| Aplicación de inyectables, 00:36:51 | Paciente | Completada: atención de Bs 55,00, llegada 00:46:07, fin 00:46:14, pago pendiente | Bs 60 del profesional, Bs 50 de la paciente, Bs 55 del profesional (00:45:01, vencido) aceptada |
| Cita de Aplicación de inyectables para el 30/11 11:00, 00:38:51 | Paciente | Cancelada | Ninguna |
| Aplicación de inyectables, 00:47:22 | Paciente | Cancelada | Ninguna |
| Aplicación de inyectables, 00:50:14 | El profesional, como paciente | Cancelada | Ninguna |
| Aplicación de inyectables, 00:54:49 | Paciente | Cancelada | Ninguna: la oferta enviada con la prueba vencida no se insertó |

`net._http_response` registra diez envíos, todos 200. Coinciden con la solicitud de las 00:36:51, las ofertas de Bs 60 y
Bs 50, la cita, la contraoferta de Bs 55, la aceptación, la llegada, el fin, una cancelación a las
00:48:41 y la solicitud de las 00:54:49, que se publicó con la prueba vigente. **Ninguno a las 00:47:22**: la
inmediata publicada con la prueba vencida no avisó a nadie. Estado final: interruptor apagado y período del 24/11
22:56:44 al 24/12 22:56:44, como lo dejó la migración.

**Previsualizaciones.** Quince nuevas y tres cambiadas: 8 en `SubscriptionScreen.kt`; 4 nuevas del Inicio en
`HomeShell.kt`, cuyas tres de Configuración ahora muestran la fila; y 3 en `InboxScreen.kt`. El autor las miró el
2026-11-25, sin observaciones (punto 9 de la Definición de Terminado).

## Incremento del sprint

Una atención se completa, ambas partes confirman su pago, íntegro para el
profesional, y el administrador resuelve las disputas. **El ciclo de negocio de la
atención está completo de punta a punta.** La visibilidad del profesional depende de
una suscripción vigente, demostrable con la prueba de 30 días y el interruptor
encendido. La compra de la suscripción llega en el Sprint 11.

## Retrospectiva

> El Sprint 8 se ejecutó del **22/11/2026** al **25/11/2026**.

Entregó el pago de la atención de punta a punta y la vigencia del profesional. El motor dejó de
calcular una comisión que la plataforma ya no cobra (HT-30). Las dos partes declaran el pago en
cualquier orden y el motor exige que coincidan (HU-25, HU-26, HU-28). El administrador resuelve
una disputa una sola vez y en cualquiera de los dos sentidos (HU-28). Y el profesional sabe hasta
cuándo aparece ante los pacientes, con una prueba de 30 días y un interruptor que decide si la
vigencia se exige (HU-38). Los 29 puntos se cerraron en cuatro días.

**Qué se aprendió.**

1. **Un cambio de modelo de negocio antes de empezar costó una historia de 3 puntos, no un
   sprint.** La comisión vivía solo en el motor: ninguna línea de Kotlin la leía. Retirarla fue
   una migración (HT-30), y el resto del sprint se replanificó sin reescribir el objetivo
   específico ante el tribunal. Que la regla viva en un solo lugar es lo que hizo barato
   cambiarla.
2. **La planificación encontró dos huecos que ningún recorrido habría mostrado, leyendo la
   definición viva y no la migración recordada.** En HU-28, la guarda de pagos dejaba al
   administrador reescribir cualquier pago desde el primer esquema. En HU-38, la política de
   ofertas llamaba a `professional_may_offer_on` en toda oferta, contraofertas incluidas: la
   propuesta de `plan.md`, sumar la vigencia ahí, habría cortado las negociaciones abiertas.
   Los dos salieron de leer `pg_get_functiondef` en el remoto antes de proponer nada.
3. **El alcance volvió a crecer en la planificación, no después del recorrido.** HU-28 pasó de
   3 a 8 puntos cuando el autor vio que el orden de HU-25 y HU-26 hacía esperar al profesional
   que ya había cobrado. Como se planteó antes de programar, las preguntas con recomendación
   dejaron la decisión tomada en una ronda, sin rehacer código.
4. **Un recorrido no debe depender de un aviso que se puede perder.** En HU-38, el criterio 4 se
   iba a recorrer tocando la notificación de una cita, y al descartarla no quedó camino. El
   rodeo, vencer la prueba con el formulario abierto, probó un caso más difícil. Desde ahora,
   cada paso que nace de un aviso lleva también su ruta por la pantalla.
5. **Las previsualizaciones siguen viendo lo que las pruebas no ven.** Al 200 %, la píldora de
   la fila nueva de Configuración partía «Suscripción» a la mitad, y todas las pruebas pasaban.
   Se encontró mirando el catálogo de pantallas antes de entregar, no en la revisión del autor.
6. **Varios experimentos en una sola transacción.** HU-38 corrió sus 37 casos en una tabla
   temporal dentro de un bloque `do`, cambiando de identidad con `set local role`, en vez de un
   comando por caso. Se mantiene la garantía de `rollback` y el resultado sale en una lectura.

**Verificación de cierre.** `./gradlew build` en verde: 1258 pruebas, 0 fallas, 0 omitidas.
Experimentos SQL de política y disparadores anotados en cada historia. El 2026-11-25, con
Docker: `supabase start` y `supabase db reset` aplicaron las 61 migraciones desde cero, y
`supabase db diff --linked --schema public` no encontró ninguna diferencia contra el remoto (ver
HT-30). La pila local se levantó sin Studio, postgres-meta, logflare, vector, imgproxy ni mailpit,
y al terminar se borraron sus contenedores, sus volúmenes y sus imágenes: el disco virtual de
Docker volvió a 255 MB (`docs/decisions.md`, 2026-11-25). El incremento se recorrió de punta a
punta a lo largo de las historias, con el paciente, el profesional y el administrador de prueba. **Control de acceso de
cierre:** pendiente del autor, que debe entrar a Google Cloud con las dos cuentas propietarias y a
Supabase con sus dos miembros, y revisar la fecha de la prueba de facturación (2027-01-18).

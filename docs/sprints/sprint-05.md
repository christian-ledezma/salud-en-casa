# Sprint 5 — Solicitudes de atención

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-22 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.


**Estado:** `[x]` cerrado el **2026-10-15**. Las cuatro historias terminadas
—HU-13, HU-14, HU-15 y HU-16— y los 24 puntos planificados entregados. Ninguna se
trasladó. Cuatro migraciones aplicadas; las 38 del proyecto reconstruyen el esquema
en local y no hay deriva contra el remoto.

**Objetivo del sprint.** El paciente solicita atención en las dos modalidades y el
profesional ve las solicitudes de su zona.

**Objetivo específico.** 3 · Comunicación, coordinación y agenda.

**Puntos:** 24.

### HU-13 · Solicitar atención inmediata `[x]` — 8 puntos

> Como **paciente con una necesidad urgente**, quiero **publicar una solicitud que
> vean los profesionales cercanos**, para **recibir atención lo antes posible**.

**Criterios de aceptación**

- [x] Dado que necesito atención, cuando creo una solicitud inmediata, entonces
      indico tipo de servicio, dirección, descripción y presupuesto sugerido.
- [x] Dado que publico la solicitud, cuando consulto su estado, entonces figura
      como publicada.
- [x] Dado que publico la solicitud, cuando la consultan los profesionales del
      radio, entonces la ven.
- [x] Dado que nadie oferta en el plazo definido, cuando el plazo vence, entonces
      la solicitud queda expirada.
- [x] Dado que la solicitud no fue aceptada, cuando la cancelo, entonces deja de
      estar publicada.

**Requisitos:** RF-07.1, RF-07.2, RF-07.5, RF-07.6, INV-03.

**Cerrada (2026-10-13).** Los cinco criterios están verificados de punta a punta.
La migración se aplicó al remoto con `db push` (tras `--dry-run`); `pg_cron` 1.6.4
estaba disponible y el job `expire_immediate_requests` quedó **activo, cada
minuto**. Se confirmó en vivo que existen `platform_settings` (RLS, plazo 30),
`my_service_requests` (`authenticated` con SELECT y sin UPDATE) y la función.

- **Migración `20261012120000_immediate_requests.sql`** (aplicada 2026-10-13): tabla
  `platform_settings` (una fila, plazo inicial 30 min, con RLS y política de
  lectura), función `create_immediate_request` (definidor, snapshot + `expires_at`),
  endurecimiento de `service_requests_insert_own` a `modality <> 'IMMEDIATE'`, vista
  de definidor `my_service_requests`, guard de tiempo en el inbox del profesional y
  el job de `pg_cron` que marca `EXPIRED`. Razonamiento en `docs/decisions.md`,
  2026-10-12.
- **Característica nueva `features/requests/`** (dominio, datos, presentación),
  espejo de `features/search/`: casos de uso `CreateImmediateRequestUseCase`,
  `GetMyRequestsUseCase`, `CancelRequestUseCase`; reutiliza
  `GetServiceTypeCatalogUseCase` (catalog) y `GetMyAddressesUseCase` (location) por
  sus casos de uso. Dos pantallas: «Solicitar atención» y «Mis solicitudes», con sus
  cuatro estados y las tres previsualizaciones (claro, oscuro, 200 %), revisadas en
  las capturas de `ScreenCatalog`: el nombre largo del servicio envuelve, nada se
  recorta a 320 dp. Puntos de entrada en «Mi cuenta» con el rol activo `PATIENT`.
- **Pruebas en verde (JVM):** objeto de valor `RequestDescription`; casos de uso;
  mapeadores; modelos de vista (incluido «sin dirección», cancelación con recarga y
  paginación); interfaz a 200 %/320 dp de ambas pantallas. `./gradlew test`,
  `assembleDebug` y `ktlintCheck` concluyen sin error.
- **Experimentos SQL de política/disparador — ejecutados el 2026-10-13 contra el
  remoto y todos en verde.** Se corrieron con `supabase db query --linked -f`, cada
  uno en una transacción que **crea los objetos de la migración y termina en
  `rollback`**, de modo que verifican la lógica real sobre los datos reales sin
  persistir nada (verificado después: cero residuo; `platform_settings` y la vista
  no existen en el remoto). Cada rechazo se probó junto con el caso que sí pasa, y
  todo objeto nuevo incluyó un intento de escritura (retro Sprint 5 #2):
  - **a.** `create_immediate_request` como paciente A → `PUBLISHED`, `IMMEDIATE`,
    snapshot de ubicación correcto, `expires_at` a 30 min, presupuesto `150.00`.
  - **b.** A con la dirección de otro paciente → `address_not_owned`; con la propia
    (a) sí pasa. Discrimina.
  - **c.** inserción directa de una `IMMEDIATE` desde el cliente → *new row violates
    row-level security policy*; una `SCHEDULED` directa sí pasa. Discrimina.
  - **d.** el paciente cancela `PUBLISHED → CANCELLED` (pasa) pero no una `ACCEPTED`
    → `request_status_change_requires_the_atomic_operation`. Discrimina (RF-07.6).
  - **e.** el profesional verificado que cubre ve solo la `PUBLISHED` vigente de un
    tipo que presta; no ve la vencida por tiempo (guard `expires_at > now()`) ni la
    de un tipo que no presta.
  - **f.** el `update` del barrido marcó `EXPIRED` solo la vencida sin oferta; dejó
    `PUBLISHED` la no vencida y la que ya tenía una oferta.
  - **g.** `my_service_requests`: para `authenticated`, `SELECT` sí, `UPDATE` y
    `DELETE` no; un `update` por la vista → *cannot update view … not automatically
    updatable*.
  - **h.** `platform_settings` tiene RLS habilitada y una política (red de INV-02).
- **Queda para el cierre del Sprint 5** (tarea de sprint, no de historia): confirmar
  que `supabase db reset` en local reconstruye el esquema completo sin error, como
  pide la Definición de Terminado para un sprint que toca el esquema. La migración
  directa ya se aplicó limpia sobre el remoto.

### HU-14 · Ver las solicitudes de mi zona `[x]` — 8 puntos

> Como **profesional**, quiero **ver en tiempo real las solicitudes publicadas
> dentro de mi radio**, para **ofertar por las que me interesan**.

**Criterios de aceptación**

- [x] Dado que soy profesional verificado, cuando abro mi bandeja, entonces veo las
      solicitudes publicadas dentro de mi radio de cobertura.
- [x] Dado que se publica una solicitud nueva en mi radio, cuando estoy en la
      bandeja, entonces aparece sin que yo recargue.
- [x] Dado que una solicitud es de un tipo que no presto, cuando abro la bandeja,
      entonces no la veo.
- [x] Dado que una solicitud ya fue aceptada, cuando abro la bandeja, entonces
      desaparece.
- [x] La bandeja está paginada.

**Requisitos:** RF-07.4, RNF-08, RN-04.

**Tareas técnicas.** Suscripción en tiempo real expuesta como flujo desde el
la capa de datos · filtrado por radio y tipo de servicio.

**Cerrada (2026-10-13).** Migración `20261013120000_professional_inbox.sql`
aplicada al remoto (`db push` tras `--dry-run` limpio). La bandeja se lee por la
RPC `immediate_inbox` (definidor), que proyecta tipo, ciudad, descripción,
presupuesto y **distancia desde la base redondeada a 100 m**, sin la ubicación
exacta, la dirección ni la identidad del paciente (RF-08.6). El tiempo real es un
Broadcast sin datos desde un trigger en `service_requests` al tema compartido
`immediate_inbox`, con una política en `realtime.messages` que solo deja escuchar
a un profesional aprobado; el cliente re-consulta la RPC en cada aviso (motivo:
supabase-kt no recorta columnas del evento, ver `docs/decisions.md` 2026-10-13).
De paso se corrigió una **regresión de HU-13**: el inbox había perdido la
exclusión `patient_id <> auth.uid()` (RN-13/INV-16) al recrearse para el guard de
tiempo; ahora lleva ambos guards.

Pruebas JVM (incluida `InboxViewModelTest`, que prueba que un aviso trae la
solicitud nueva sin recargar), `ktlintCheck` y las tres previsualizaciones, todas
en verde. Experimentos SQL contra el remoto (`begin…rollback`, 2026-10-13, todos
en verde):

1. `immediate_inbox` como profesional aprobado que cubre devuelve **solo** la
   `PUBLISHED` inmediata en radio de un tipo que presta, con `distance_m` = 0;
   descarta a la vez el tipo no ofrecido, la de fuera de radio, la vencida, la
   `ACCEPTED` y **la propia** del profesional de doble rol.
2. La firma de retorno de la RPC es `TABLE(id, service_type_name, city,
   description, suggested_budget_bob, distance_m, status, created_at)`: sin
   `location`, `address_text`, `reference` ni `patient_id`.
3. El predicado de escucha (`existe en professional_directory`) es verdadero para
   el profesional aprobado y falso para el paciente.
4. La política sobre `realtime.messages`, los dos triggers y `realtime.send` se
   crean y ejecutan sin error; el insert y el cambio de estado disparan el aviso.
5. La política `service_requests_select_inbox` recuperada, leída de `pg_policies`,
   contiene a la vez `patient_id <> auth.uid()` y `expires_at > now()`.

Verificación en vivo tras el push: RPC, función y dos triggers presentes, política
del inbox con ambos guards y política de `realtime.messages` creadas. No se crearon
tablas nuevas, así que `everyTableHasRowLevelSecurityEnabledAndAtLeastOnePolicy` no
cambia. El `supabase db reset` local que quedaba a nivel de sprint se corrió al
cerrar el Sprint 5: las 38 migraciones reconstruyen el esquema sin deriva contra
el remoto.

### HU-15 · Declarar mi disponibilidad horaria `[x]` — 3 puntos

> Como **profesional**, quiero **indicar en qué franjas horarias atiendo**, para
> **recibir solicitudes agendadas solo cuando puedo**.

**Criterios de aceptación**

- [x] Dado que abro mi disponibilidad, cuando agrego una franja, entonces indico
      día de la semana, hora de inicio y hora de fin.
- [x] Dado que la hora de fin es anterior a la de inicio, cuando guardo, entonces
      se rechaza.
- [x] Dado que declaro mis franjas, cuando un paciente intenta agendar, entonces
      solo puede elegir dentro de ellas.

**Requisitos:** RF-02.4.

**Cerrada (2026-10-14).** Migración `20261014120000_availability_overlap_guard.sql`
aplicada al remoto (`db push` tras `--dry-run` limpio). La tabla
`availability_slots` ya existía desde el Sprint 1 con `end_time > start_time`
y el único `(professional_id, day_of_week, start_time, end_time)`; HU-15 agrega
el trigger `availability_slots_reject_overlap` que llama a
`reject_overlapping_slot`, una función `security definer` con `search_path = ''`
que rechaza por `OVERLAPS` sobre `time` toda franja que se cruce con otra del
mismo profesional en el mismo día, lanzando `errcode = '23505'` para que el
cliente reutilice el manejador de duplicado existente. El tercer criterio lo
sostiene la política pública `availability_slots_select_public` que ya existía;
HU-16 construirá la pantalla de agendamiento que la ejerce.

Lado cliente: nueva característica `features/availability/` con dominio
(`AvailabilitySlot`, `WeeklyAvailability` con las siete claves siempre
pobladas, `AvailabilityError`, tres casos de uso), datos (DTO con `LocalTime`
como `"HH:MM:SS"`, mapeo de mensaje a `OverlapsExistingSlot`, datasource con
`select/insert/delete`, repositorio) y presentación (`AvailabilityScreen` con
lista por día en `LazyColumn`, editor de franja dentro del día y diálogo de
confirmación para eliminar; ver la corrección del 2026-10-15 más abajo).
Entrada en `AccountScreen` solo cuando el rol activo es `PROFESSIONAL`, junto a
la bandeja de HU-14. `availabilityModule` registrado en Koin y
`AvailabilityRoute` en el `NavHost`.

Pruebas JVM (`AddAvailabilitySlotUseCaseTest` incluida con el rechazo local
`EndNotAfterStart`, `GetMyAvailabilityUseCaseTest` sobre las siete claves,
`AvailabilityMapperTest` con 23505 y transporte, `AvailabilityViewModelTest`
con reload tras alta y error que mantiene el editor abierto), `AvailabilityScreenLargeFontTest`
(Robolectric 200%/320 dp), `ktlintCheck` y las tres previsualizaciones, todas
en verde. Experimentos SQL contra el remoto (`begin…rollback`, 2026-10-14, todos
en verde) antes del push:

1. (overlap) Insertar `(pro, LUN, 10:00, 12:00)` y luego `(pro, LUN, 11:00, 13:00)`:
   el segundo falla con `23505 availability_slot_overlaps`.
2. (adyacente) Insertar `(pro, LUN, 10:00, 12:00)` y luego `(pro, LUN, 12:00, 13:00)`:
   coexisten (ambas franjas visibles en el `SELECT` final).
3. (end > start) Insertar `(pro, LUN, 12:00, 12:00)`: falla con
   `23514 slot_ends_after_it_starts`.
4. (lectura pública) Autenticado como paciente, un `SELECT` sobre las franjas
   del profesional aprobado devuelve una fila: la política
   `availability_slots_select_public` ya cumple el tercer criterio.

Transversal: ninguna tabla del esquema público queda sin RLS o sin política.
Verificación en vivo tras el push: la función y el trigger existen en el remoto.
El `supabase db reset` local que quedaba a nivel de sprint se corrió al cerrar el
Sprint 5, con las 38 migraciones y sin deriva.

#### Corrección del 2026-10-15: fuera el diálogo, fuera los dos relojes, días en español

**Qué encontró el autor al usar la pantalla.** El alta abría un diálogo con **dos**
`TimePicker` de Material 3, uno encima del otro, y una fila de fichas circulares
para volver a elegir el día al que ya se había entrado. Y los siete títulos salían
en inglés: el nombre venía de `DayOfWeek.getDisplayName`, que sigue al idioma del
teléfono, dentro de una aplicación que está en español cualquiera sea ese idioma.

**Qué quedó.** El diálogo de alta desaparece. «Agregar franja» abre un editor
**dentro del día**, con dos filas de píldoras de hora —`TimeChipRow`, el mismo
control que HU-16 usa para las horas agendables— en pasos de 30 minutos, el botón
«Guardar franja» y «Cancelar». El día ya no se elige: lo fija el botón desde el que
se abrió el editor. Las horas de «Hasta» son siempre posteriores al inicio, y mover
el inicio más allá del fin arrastra el fin, de modo que la pantalla no deja componer
el rango que el motor rechaza. El nombre del día pasa a siete claves de recurso. El
diálogo de eliminar se conserva: borrar es destructivo y la papelera está a un toque.
Razonamiento en `docs/decisions.md`, 2026-10-15.

**Lo verificado.** 663 pruebas en 114 suites, 0 fallos, 0 omitidas; `ktlintCheck`,
`staticAnalysis` y `./gradlew build` —con Android lint— en verde. Las seis
previsualizaciones de la pantalla se renderizaron con el capturador de
`ScreenCatalog` y **se miraron**, y se volvieron a mirar después de fijar el reloj
de 24 horas: contenido en claro, contenido al 200 %, editor en
claro, editor al 200 % en oscuro con el error de solapamiento, vacía en oscuro y
error al 200 %. Tres mutaciones, cada una con su resultado:

| Mutación | Resultado |
|---|---|
| El nombre del día vuelve al calendario de la plataforma | **Falla** `theDayNameIsTheSpanishOneFromTheResources`, y solo esa |
| La página deja de desplazar (`userScrollEnabled = false`) | **Fallan 7 de 9** pruebas de disposición; las dos que no dependen del desplazamiento —el botón de volver y el diálogo— siguen pasando |
| El fin deja de seguir al inicio, y «Hasta» vuelve a ofrecer la hora del inicio | **Fallan** `aStartMovedPastTheEndCarriesTheEndWithIt` y tres de `SlotTimesTest` |

**Dos cosas que la pantalla dejó a la vista.** La **hora** se leía «9:00 AM» en un
teléfono en inglés. Se le presentó al autor, que el mismo 2026-10-15 decidió
conservar el formateador de la plataforma y fijar el reloj de 0 a 24: el esqueleto
pasa de `j` a `H` en `formatTime` y en `formatAppointment`, de modo que el cambio
alcanza también a las horas agendables y a la hora de la cita. Dos pruebas nuevas
en `AppointmentFormatTest` lo sostienen, y volver a `j` las pone en rojo. Lo
segundo no se tocó: en las previsualizaciones la flecha de volver se dibuja oscura
sobre fondo oscuro, porque `IconButton` toma el color de contenido del `Scaffold` y
la previsualización no tiene ninguno; en el teléfono es correcta.

### HU-16 · Agendar una cita `[x]` — 5 puntos

> Como **paciente que no tiene urgencia**, quiero **agendar una atención con un
> profesional específico**, para **coordinar con anticipación**.

**Criterios de aceptación**

- [x] Dado que abro la ficha de un profesional, cuando elijo agendar, entonces veo
      sus franjas disponibles.
- [x] Dado que elijo fecha y hora, cuando confirmo, entonces se crea una solicitud
      agendada dirigida a ese profesional.
- [x] Dado que elijo un horario fuera de su disponibilidad, cuando confirmo,
      entonces se rechaza.
- [x] Dado que creo la solicitud, cuando el profesional abre su bandeja, entonces
      la ve identificada como agendada.

**Requisitos:** RF-07.3.

**Cerrada (2026-10-15).** El autor decidió en la planificación todas las reglas,
registradas en `docs/decisions.md` el 2026-10-15:
- creación por función de servidor;
- la atención completa dentro de una franja, en hora de `America/La_Paz`;
- pedir no reserva;
- cobertura exigida;
- 14 días, pasos de 30 minutos y 2 horas de anticipación mínima;
- solo los servicios declarados;
- vence a la hora de la cita;
- una sola bandeja con insignia;
- dos carruseles, una pantalla con botón fijo al pie y «Agendar cita» fijo al pie de
  la ficha.

Entre dos planos, uno con la regla en el servidor y otro con la regla en el
cliente, se eligió el del servidor: las horas las calcula una sola función.

- **Migración `20261015120000_scheduled_requests.sql`.** Se verificó con
  `begin…rollback` contra el remoto y se aplicó con `db push` el 2026-10-15, tras un
  `--dry-run` limpio. Contiene:
  - `bookable_start_times` y `create_scheduled_request`, ambas de definidor;
  - retiro de `service_requests_insert_own`;
  - `UPDATE` sobre `service_requests` limitado a la columna `status`;
  - `scheduled_at` y el nombre del profesional en `my_service_requests`;
  - `request_inbox` en lugar de `immediate_inbox`;
  - aviso en tiempo real y barrido de `pg_cron` extendidos a las agendadas.
- **Cliente:**
  - en `features/requests`: dominio (`BookableTimes`, `PartOfDay`, dos casos de uso
    y la validación de campos opcionales compartida con la inmediata), datos y la
    pantalla «Agendar una cita»;
  - en la ficha: `CanScheduleAppointmentUseCase` y la barra fija;
  - componentes nuevos: `DayCarousel`, que reemplaza a `DatePickerRow`, y
    `BottomActionBar`;
  - en la bandeja y en «Mis solicitudes»: insignia de modalidad, hora de la cita y,
    para el paciente, el nombre del profesional.
- **Corrección de HU-15:** `findMySlots` pasó a `findSlotsOf(professionalId)`. Sin
  el filtro, «Mi disponibilidad» mezclaba las franjas de todo profesional aprobado
  (lo prueba e36 abajo). No tiene prueba JVM: ninguna fuente de datos del proyecto
  se prueba sin un simulador HTTP, que no se agregó.

**Pruebas JVM, todas en verde (640 en la suite).** Más `ktlintCheck` y
`assembleDebug`. Las nuevas:
- `BookableTimesTest`;
- `CreateScheduledRequestUseCaseTest`;
- casos nuevos en `RequestMapperTest` y `RequestErrorMapperTest`;
- `AppointmentFormatTest`: 02:00 UTC del sábado es viernes 22:00 en La Paz;
- `ScheduleAppointmentViewModelTest`, con el caso de la respuesta tardía de un
  servicio abandonado;
- cinco casos de la ficha en `PublicProfileViewModelTest`;
- `ScheduleAppointmentScreenLargeFontTest` y casos agendados en las pruebas al
  200 % de la bandeja, de «Mis solicitudes» y de la ficha.

Cinco aserciones nuevas se **mutaron** y las cinco fallaron con la mutación. Una
sexta sobrevivió: la respuesta tardía se descartaba por dos mecanismos a la vez.
Se retiró el redundante y la prueba mata la mutación del que queda.

**Revisión de cierre.** Dos revisores, uno de correctitud y seguridad y otro de
convenciones, no encontraron defectos nuevos. A partir de la revisión:
- se recortaron los comentarios nuevos que repetían razonamientos o describían
  el código;
- se agregaron a la prueba al 200 % los cuatro estados que faltaban;
- se agregó el `distinct` de e37;
- las dos deudas de privacidad y cobertura pasaron a HU-17.

Quedan como deuda: la solicitud repetida (mismo paciente, profesional y hora), para
HU-19, y que la pantalla no se refresca al volver de registrar una dirección, igual
que la inmediata.

**Previsualizaciones abiertas y miradas** en el catálogo: claro, oscuro, 200 %, sin
horarios, hora vencida, no agendable, sin dirección, error y cargando. En oscuro la
pantalla no pintaba su fondo; se corrigió envolviéndola en un `Surface` del tema.
La bandeja de HU-14 tiene el mismo defecto en su vista previa oscura y queda
anotado, sin tocar.

**Experimentos SQL contra el remoto (2026-10-15, `begin…rollback`, todos en
verde).** Fixture: franjas lunes 9:00–12:00, martes 9:15–12:00 y jueves
8:00–20:00 del único profesional aprobado; tres direcciones de prueba del paciente.

1. **e01 · Grilla.** El lunes, con 60 min, ofrece 9:00 a 11:00, sin 11:30. El
   martes ofrece 9:15, 9:45, 10:15 y 10:45.
2. **e02 · Ventana.** La primera hora de hoy está a 2 h 16 min (12:30 queda
   fuera). La última es el 29/10 a las 10:30, a 13 min del borde de 14 días.
3. **e03–e05 · Filtros de `bookable_start_times`.** Cero filas para:
   - un servicio no declarado;
   - un profesional pendiente;
   - el propio profesional;
   - un perfil inactivo.
4. **e06 · Creación aceptada.** `SCHEDULED`, `PUBLISHED`, `expires_at =
   scheduled_at`, instantánea igual a la dirección y presupuesto `150.00`.
5. **e07–e19 · Rechazos, cada uno con su etiqueta:**
   - `time_not_bookable`: excede la franja, desalineada, día sin franja, menos de
     2 h y más de 14 días;
   - `address_not_owned`;
   - `professional_unavailable`;
   - `service_not_offered`;
   - `service_type_unavailable`;
   - `outside_coverage`;
   - `cannot_schedule_with_yourself`.
6. **e18 y e19 · Cobertura contra la base redondeada.** Una dirección a 7.990 m de
   la base exacta se rechaza y una a 8.005 m se acepta.
7. **e20–e25 · Escrituras directas.**
   - La inserción directa de las dos modalidades da 42501.
   - Reescribir `scheduled_at`, `expires_at` o `modality` da permiso denegado.
   - Cancelar sigue funcionando.
8. **e26–e29 · Bandeja.**
   - El destinatario ve la agendada y la inmediata, con modalidad, hora y
     distancia.
   - El profesional pendiente y el paciente no ven nada.
   - Una agendada vencida desaparece.
   - La firma de `request_inbox` no tiene ubicación ni paciente, y `immediate_inbox`
     ya no existe.
9. **e30–e32 · Vista.** El dueño ve `scheduled_at` y el nombre del profesional, otro
   paciente no ve nada, y escribir por la vista da *cannot update view*.
10. **e34 · Barrido.** Ejecutando el comando del job ya reprogramado, la cita pasada
    queda `EXPIRED` y la futura queda `PUBLISHED`.
11. **e35 · Triggers.** Ninguno de los dos triggers de aviso filtra por modalidad.
12. **e36 · HU-15.** Un profesional que lee `availability_slots` sin filtro recibe
    las 3 franjas de otro profesional.
13. **e37 · Franjas solapadas.** Con dos franjas solapadas forzadas, la guarda de
    HU-15 no toma un lock y dos inserciones concurrentes pueden pasarla. Aun así,
    `bookable_start_times` devuelve 5 horas distintas. Sin el `distinct` devuelve 8
    filas para esas 5 horas, y una clave repetida tira el carrusel. El experimento
    discrimina.

No se crearon tablas, así que la transversal de RLS no cambia.

**Verificación en vivo tras el push:**
- existen `bookable_start_times`, `create_scheduled_request` y `request_inbox`, y
  `immediate_inbox` ya no existe;
- no queda `service_requests_insert_own`;
- `authenticated` puede actualizar `status` y no `scheduled_at`;
- `anon` no puede ejecutar `bookable_start_times`;
- `my_service_requests` no admite `UPDATE`;
- ningún trigger de aviso filtra por modalidad;
- ninguna tabla pública queda sin RLS o sin política.

La aplicación instalada antes de esta versión llama a `immediate_inbox`, así que su
bandeja deja de funcionar hasta instalar la nueva.

## Incremento del sprint

Un paciente publica una solicitud inmediata y el profesional la ve aparecer en su
bandeja sin recargar. Otro paciente agenda una cita para el jueves.

> **Cumplido, con una precisión.** Las dos mitades están verificadas de punta a
> punta contra el remoto: la inmediata con los experimentos de HU-13 y HU-14, la
> agendada con los 37 de HU-16. Lo que todavía no se recorrió en el emulador es la
> agendada, porque su migración se aplicó el mismo día del cierre y la aplicación
> instalada llama todavía a `immediate_inbox`. Ninguna de las cuatro historias
> figura en «Cuándo hace falta un dispositivo», así que no es una deuda de la
> Definición de Terminado. Es lo primero que conviene mirar al instalar la versión
> nueva.

## Retrospectiva

**La aplicación ya conecta a dos personas.** Hasta el Sprint 4 cada pantalla
servía a quien la abría. Desde este sprint, lo que hace un paciente aparece en la
pantalla de un profesional: la solicitud inmediata llega a la bandeja sin recargar
y la cita agendada llega a quien va dirigida. Es la primera mitad de la cadena que
termina en la calificación del Sprint 9.

### Velocidad medida: 24 puntos, los 24 planificados

HU-13 (8) + HU-14 (8) + HU-15 (3) + HU-16 (5) = 24. Las cuatro cumplen la
Definición de Terminado y ninguna se trasladó. **Cuatro migraciones** aplicadas,
**111 suites y 645 pruebas** en verde al cierre (eran 83 suites y 491 pruebas al
cerrar el Sprint 4) y **54 experimentos SQL** contra el remoto: 8 de HU-13, 5 de
HU-14, 4 de HU-15 y 37 de HU-16.

> El Sprint 5 se ejecutó del **12/10/2026** al **15/10/2026**.

**Cuatro días, y la sexta medición seguida que no mide capacidad.** Los 24 puntos
cupieron, y además cupieron tres tareas que no estaban planificadas: la marca y el
arranque (HT-19), la bienvenida (HT-20) y el rediseño de la búsqueda y la ficha
(HT-21). Las pidió el autor como Product Owner en medio del sprint y ninguna tiene
puntos. Así que la capacidad real de estos cuatro días fue **mayor** que 24, y el
número no lo dice.

La retrospectiva del Sprint 4 pedía planificar más alcance del que se cree que
cabe y dejar que el corte muerda. No se hizo: el sprint pasó de 23 a 24 puntos y
ni siquiera llevó escrita su fecha de corte, que el Sprint 4 sí tenía. La serie
completa es: 21 en 4 días, 21 en 2, 23 en 4, 21 en 2, 23 en 3 y 24 en 4. **Ningún
sprint agotó su plazo.**

### Qué se cumplió de lo propuesto para el Sprint 5

De los seis cambios que pedía la retrospectiva del Sprint 4:

1. **Planificar más alcance del que cabe: no.** Ver arriba. Las tareas que el autor
   agregó en el camino demuestran que había espacio, pero entraron como trabajo sin
   medir y no como alcance planificado.
2. **Toda prueba de política incluye un intento de escritura: sí, en las cuatro
   historias.** HU-13 intentó escribir por `my_service_requests`. HU-16 lo hizo
   sobre la tabla, las dos inserciones directas, tres columnas reescritas y la
   vista, y comprobó en vivo `has_table_privilege` y `has_column_privilege`.
3. **Mutar la aserción nueva antes de darla por buena: sí en HU-16, sin registro en
   las otras tres.** En HU-16 se mutaron seis aserciones y una sobrevivió. Al
   mirar por qué, apareció un mecanismo duplicado en el modelo de vista, que se
   retiró. En `plan.md` no consta que HU-13, HU-14 y HU-15 lo hicieran.
4. **Enumerar los escritores también cuando no son una columna: sí, y pagó.** Al
   planificar HU-16 se listó quién puede escribir en `service_requests`, y apareció
   que `service_requests_update_own` dejaba reescribir `scheduled_at` después de
   crear la solicitud. Eso habría anulado toda la validación de la historia. La
   lista de INV-09 e INV-11 sigue siendo trabajo del Sprint 8 y del 9.
5. **Programar HT-17: no.** Cuarta retrospectiva que lo pide. Sigue sin bloquear.
6. **Anotar en HU-29 la verificación pendiente de HU-12: sí.** Está en la historia.

### Qué funcionó

- **Preguntar antes de diseñar.** HU-16 se planificó con tres rondas de preguntas
  al autor, cada una con su recomendación. Fueron once decisiones de producto y de
  regla: cuándo una hora cae dentro de la disponibilidad, si pedir reserva, la
  cobertura, el horizonte, el vencimiento, la bandeja y la forma de la pantalla.
  Ninguna hubo que revisarla después, y cada una quedó escrita en `decisions.md`
  antes del código.
- **Comparar dos planos antes de elegir uno.** Se diseñaron en paralelo la regla en
  el servidor y la regla en el cliente. Los dos coincidieron en casi todo, y cada
  uno encontró algo que el otro no vio: el primero, el canal de bisección de la
  cobertura; el segundo, la reescritura de `scheduled_at` y la fuga de franjas de
  HU-15. Las tres correcciones entraron en la historia con el visto bueno del
  autor.
- **Una sola copia de la regla.** Las horas reservables las calcula una función
  del motor, que la pantalla dibuja y la creación vuelve a consultar. Sus bordes se
  probaron uno por uno contra el remoto: la grilla desde las 9:15, la última hora
  de la franja, las dos horas de anticipación, el día catorce y la cobertura
  decidida a 31 m de diferencia.
- **Los experimentos siguen siendo la herramienta más barata.** Cincuenta y cuatro
  en el sprint, todos dentro de `rollback` y sin residuo. Los de HU-16 corrieron
  sobre la migración sin aplicar, de modo que el `db push` llegó después de que
  todo hubiera pasado.
- **Mirar las previsualizaciones encontró un defecto real otra vez.** En oscuro la
  pantalla de agendar no pintaba su fondo, y el título y las opciones quedaban
  ilegibles. Ninguna prueba lo habría visto.
- **La revisión de cierre con dos revisores** encontró cuatro cosas concretas: los
  comentarios fuera de regla, cuatro estados sin prueba al 200 %, la hora repetida
  que podía tumbar el carrusel, y dos deudas que convenía fijar en HU-17. Las
  cuatro se corrigieron antes de cerrar.

### Qué no funcionó

- **Descarté por error dos observaciones correctas en la revisión del pull request
  de HU-15, y el defecto llegó a `master`.** Copilot señaló que la consulta de
  franjas exponía las de otros profesionales. Lo di por falso positivo razonando
  sobre una política, sin probarla: las dos políticas de lectura se suman, y «Mi
  disponibilidad» mezclaba las franjas de todo profesional aprobado. Además afirmé
  que el disparador de solapamientos serializaba las inserciones concurrentes, y
  no es así. Las dos cosas se vieron en HU-16 y se corrigieron ahí. Es el error más
  serio del sprint y es mío. La regla que queda: una observación de revisión se
  descarta con un experimento, no con un razonamiento.
- **La primera versión de HU-16 no cumplía la regla de comentarios.** Muchos
  comentarios repetían el razonamiento que ya estaba en `decisions.md`, o
  describían lo que el código hace. Lo encontró la revisión, no yo al escribirlos.
- **La prueba al 200 % llegó incompleta a la revisión.** Le faltaban cuatro estados
  de la pantalla nueva. La regla de `testing.md` dice «cada control», y la prueba
  cubría los del estado principal.
- **Ejecuté `git status` otra vez**, en HU-16, para listar archivos. Es la segunda
  retrospectiva seguida con este mismo apunte. La lista de archivos sale de lo que
  uno mismo escribió, no del repositorio.
- **Tres pedidos sin medir dentro del sprint.** HT-19, HT-20 y HT-21 eran trabajo
  real, pero sin puntos ni lugar en el plan. Las tres quedaron cerradas —el autor
  miró las previsualizaciones y recorrió el emulador—, y aun así el trabajo no
  planificado es lo que vuelve a dejar la velocidad sin significado: se hizo, pero
  no se contó.
- **Defectos anteriores que siguen abiertos.** El capturador de previsualizaciones
  todavía no dibuja las tres pantallas con diálogo. Ahora consta además que la
  bandeja de HU-14 tampoco pinta su fondo en la previsualización oscura. Ninguno
  afecta a la aplicación instalada, pero los dos restan valor al control que
  sustituye al recorrido de cada pantalla.

### Qué cambiar en el Sprint 6

1. **Escribir la fecha de corte en el encabezado del sprint antes de empezar, y
   planificar por encima de lo que cabe.** El Sprint 6 tiene 26 puntos. Si las
   cuatro historias se cierran antes del corte, se toma la siguiente del Sprint 7
   en lugar de cerrar.
2. **Lo que el autor pida en medio del sprint entra con puntos.** Así la velocidad
   cuenta todo lo que se hizo, y no solo lo que se planificó.
3. **Una observación de revisión se descarta con un experimento.** Si se dice
   «falso positivo», va con la consulta o la prueba que lo demuestra.
4. **HU-17 empieza por lo que HU-16 le dejó.** Retirar
   `service_requests_select_addressee` tras llevar su lectura a una función de
   definidor, y decidir la rama agendada de la política de ofertas por
   `professional_id`. Las dos están escritas en la historia y no se cierra sin
   ellas.
5. **Repasar los comentarios nuevos antes de entregar**, contra la regla de
   `CLAUDE.md`, y la prueba al 200 % contra cada estado de la pantalla, no solo el
   principal.
6. **Programar HT-17.** Quinta retrospectiva que lo pide.

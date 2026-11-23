# Sprint 2.5 — Rol múltiple con rol activo

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-22 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.


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

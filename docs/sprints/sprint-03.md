# Sprint 3 — Verificación de usuarios

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-22 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.


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
      La paginación la cubren `GetPendingReviewSubjectsUseCaseTest` (página corta
      contra página llena, el borde exacto) y `DocumentReviewQueueViewModelTest`.
      **Rehecho el 2026-10-08:** la cola agrupa por persona y la antigüedad pasó a
      ser el orden por omisión, elegible. Reverificado en local con la vista nueva,
      en los dos sentidos. Ver «Rediseño de las dos pantallas» más abajo.
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
      **Recorrido en dispositivo el 2026-10-09 con las dos cuentas:** el autor aprobó los
      cinco documentos desde el panel, pulsó «Verificar profesional» y comprobó que la
      ficha pública del profesional se publica. En el remoto quedó
      `verification_status = APPROVED` y 1 fila en `professional_directory`. Ese recorrido
      es el que destapó el hueco de la puerta al botón, corregido el mismo día.
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

> **Advertencia sobre los datos de prueba.** La cola real contiene los 4 documentos
> `PENDING` de la verificación en dispositivo de HU-07. **Aprobarlos o rechazarlos desde la
> aplicación los convierte en otra cosa** y esa evidencia ya no coincide con lo anotado en
> HU-07. Desde el 2026-10-08 hay una segunda tanda con la que sí se puede recorrer el
> panel: los **5 documentos** que el autor subió con su cuenta de rol doble, que es además
> el profesional que el Sprint 4 necesita ver aprobado para que HU-11 devuelva algo. La
> cola agrupada muestra hoy exactamente esas dos personas, con 4 y 5 pendientes.

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
> Se demostró primero **contra la base**, con experimentos SQL en el remoto: el
> administrador aprueba los documentos, promueve, y el profesional aparece en
> `professional_directory`; si se le rechaza un documento requerido o cambia su tipo,
> desaparece.
>
> **Y el 2026-10-09 se demostró en pantalla**, con las dos cuentas en un dispositivo: el
> profesional carga sus cinco documentos, el administrador los aprueba desde el panel,
> pulsa «Verificar profesional», y la ficha pública del profesional pasa a publicarse.
> Lo hizo el autor, y en el camino encontró el hueco que ese recorrido existía para
> encontrar: el botón de verificar no tenía puerta una vez aprobado el último documento
> (ver «El hueco del 09/10»).
>
> **Lo único que sigue sin demostrarse en pantalla es la frase «aparece en las
> búsquedas»**, porque la pantalla de búsqueda llega con HU-11, en el Sprint 4. Hasta
> entonces la visibilidad se comprueba en la ficha pública y en `professional_directory`,
> no en un resultado de búsqueda.

## Retrospectiva

> **Nota del 2026-10-09.** Esta retrospectiva se escribió el 06/10, cuando el circuito
> solo estaba demostrado contra la base. El 09/10 el autor lo recorrió en un dispositivo
> con las dos cuentas y funcionó, así que el párrafo siguiente quedó parcialmente
> superado: lo que falta ya no es «la pantalla», es solo la de búsqueda.

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

HT-17 sigue pendiente: vive en `plan.md`, «Pendiente sin sprint».

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

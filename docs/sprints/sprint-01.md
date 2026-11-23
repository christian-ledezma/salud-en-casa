# Sprint 1 — Ingreso e identidad

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-22 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.


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

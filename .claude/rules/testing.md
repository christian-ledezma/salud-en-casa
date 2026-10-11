# Casos de prueba obligatorios

Se carga al trabajar con archivos de prueba.

## Principio

**Una prueba vale por el error que puede atrapar, no por la línea que ejecuta.**

Antes de escribir una prueba, responder: *¿qué error del mundo real haría fallar
esto?* Si no hay respuesta, la prueba sobra y solo agrega tiempo de ejecución y
mantenimiento.

Pruebas que no aportan y no deben escribirse:

- La que confirma que un `getter` devuelve lo que el constructor recibió.
- La que simula todas las dependencias y termina verificando que se llamó al
  simulador, no que ocurrió algo correcto.
- La que repite la lógica de la implementación en la aserción. Si la prueba
  calcula el resultado igual que el código, ambos se equivocan juntos.
- La que existe para subir un porcentaje de cobertura.

Pruebas que sí aportan:

- La que fija una **regla de negocio** y falla si alguien la relaja.
- La que cubre el **caso límite exacto**: el cero, el máximo, el valor vacío, el
  borde del rango.
- La que verifica una **condición de carrera** con sincronización real.
- La que comprueba que una **política de seguridad** no filtra datos ajenos.
- La que confirma que una operación es **atómica**: falla a la mitad y no deja
  registro parcial.
- La que verifica que un dato **congelado** no se recalcula al leerlo.

## La suite completa pasa en cada iteración

**Ninguna historia se cierra con una prueba en rojo.** No existe la prueba
deshabilitada temporalmente, la anotada como ignorada, ni la comentada «para
arreglar después».

Si una prueba falla, hay dos posibilidades y ambas exigen acción: el código está
mal y se corrige, o la prueba expresaba mal la regla y se corrige la prueba
explicando por qué estaba equivocada. Nunca se apaga.

Una prueba intermitente es un defecto, no una molestia. Se corrige o se elimina la
causa de la intermitencia; no se reintenta hasta que pase.

## Dónde vive cada prueba

| Qué se prueba | Ubicación | Se ejecuta en |
|---|---|---|
| Objetos de valor | `app/src/test` | Máquina virtual de Java |
| Casos de uso | `app/src/test` | Máquina virtual de Java |
| Transformadores | `app/src/test` | Máquina virtual de Java |
| Modelos de vista | `app/src/test` | Máquina virtual de Java |
| Interfaz | `app/src/test`, con Robolectric | Máquina virtual de Java |
| Forma de una pantalla | Previsualizaciones `@Preview` | Claro, oscuro y fuente al 200 % |
| Políticas de seguridad y disparadores | Experimento SQL contra la base | Proyecto remoto, dentro de una transacción con `rollback` |
| Que las migraciones reconstruyan el esquema | `supabase db reset` y `db diff --linked` | Entorno local con Docker |

La estructura de paquetes de las pruebas replica la del código fuente.

**Recorrer la pantalla a mano dejó de ser obligatorio para toda historia el
2026-10-04.** Solo lo exigen las historias críticas, en emulador, y las de
criticidad alta, en dispositivo físico; `plan.md` las enumera en «Cuándo hace falta
un dispositivo». Para el resto, la forma de la pantalla se verifica con las tres
previsualizaciones, que por eso pasaron de recomendación a obligación. Ver
`docs/decisions.md`, 2026-10-04.

**Las pruebas de interfaz corren en la máquina virtual de Java, no en un emulador.**
Robolectric compone la pantalla con `createComposeRule`, y `graphicsMode=NATIVE`
—en `app/src/test/resources/robolectric.properties`— es lo que hace que el texto se
mida de verdad: sin él, cada `Text` mide tres o cuatro dp y ninguna aserción de
disposición significa nada. La configuración fija un teléfono de **320 dp**, el ancho
más angosto que Android todavía envía, porque es donde el 200 % de fuente recorta.
Se prueba la función de contenido, que por eso es `internal` y no `private`.

**Una trampa de mecánica.** Un `LazyColumn` no compone nada por debajo del pliegue, de
modo que una fila fuera de la vista no está en el árbol semántico y `assertFitsTheScreen`
falla con «could not find any node» en vez de decir algo sobre la disposición. Se mueve la
lista al índice de esa fila primero, con `performScrollToIndex` sobre el nodo que tiene
`hasScrollToIndexAction()`. No es un defecto de la pantalla. Ver `docs/decisions.md`,
2026-10-08.

**Qué atrapan y qué no.** Atrapan: un control que deja de componerse, uno que queda
fuera de los bordes de la pantalla, y contenido al que no se puede llegar porque la
pantalla dejó de desplazarse. Las tres se comprobaron por mutación el 2026-10-08.
**No atrapan el texto recortado dentro de su propio contenedor**: Compose mide ese
`Text` al ancho del contenedor y ni los límites ni la semántica delatan el recorte.
Esa forma —la del rótulo del control segmentado del Sprint 2.5 y la del chip de
HU-07— **sigue dependiendo de mirar las previsualizaciones**, que por eso no se
reemplazan. Ver `docs/decisions.md`, 2026-10-08.

**Hay un caso en que sí se puede afirmar algo del recorte interior, y se escribe
comparando.** Si la pantalla muestra, al lado del elemento largo, otro del mismo
tipo con una etiqueta corta, el largo tiene que medir **más alto** que el corto:
si su etiqueta envolvió, creció; si quedó recortada en una línea, los dos miden
lo mismo. Lo que **no** sirve es medir contra una constante —«más alto que los
32 dp que Material da a una ficha»—: al 200 % de fuente una sola línea ya supera
cualquier altura de reposo, de modo que la aserción pasa sin sostener nada. En
HU-11 se escribió primero la versión absoluta, se mutó el código fijando la
etiqueta a una línea y **la prueba siguió pasando**; la versión relativa falla con
«Both rows are 48.0.dp tall». Mutar la aserción es lo único que distingue una de
otra. Ver `docs/decisions.md`, 2026-10-09.

**Las pruebas de política no tienen archivo.** Una política de seguridad a nivel de
fila y un disparador se ejecutan dentro de PostgreSQL, así que ninguna prueba de
JUnit puede ejercerlos: no hay base de datos en la máquina virtual de Java. Se
verifican con un experimento SQL contra el proyecto remoto, simulando al usuario
autenticado con
`set local role authenticated` y `set local request.jwt.claims`, **dentro de una
transacción que termina en `rollback`** para no dejar residuo. Es el método con el
que se verificaron los disparadores de dirección en HU-05 y con el que se encontró
la causa del defecto de HU-06.

**Cómo se corre, en concreto.** Con `supabase db query --linked`, que ejecuta SQL
contra el proyecto remoto por la Management API y exige `npx supabase login` una
sola vez:

```
npx supabase db query --linked 'begin; set local role authenticated; select set_config($$request.jwt.claims$$, $${"sub":"<id del perfil>","role":"authenticated"}$$, true); <la sentencia que se prueba>; rollback;'
```

Tres detalles que ahorran tiempo:

- **Todo el SQL va entre comillas dobles `$$`**, nunca simples, para que no pelee
  con las comillas del shell. `set_config(..., true)` equivale a `set local` y
  acepta cualquier expresión, mientras que `set` quiere un literal.
- **Una sentencia por comando.** Un error aborta la transacción, así que dos
  pruebas en el mismo comando solo muestran la primera. Y el error suele **ser**
  el resultado esperado.
- **Un rechazo no deja residuo aunque falte el `rollback`**, porque una excepción
  de PostgreSQL aborta la transacción entera. El `rollback` cubre el caso que no
  se quiere ver: que la sentencia no falle.

Cuando lo que se prueba es que algo se rechaza, el experimento **debe incluir
también el caso que sí debe pasar**, o uno que ataque la misma regla por otro
camino. Es la diferencia entre comprobar que deniega y comprobar que discrimina:
así se encontró la recursión de `request_offers_insert_participants` el
2026-10-02, y así se vio el 2026-10-04 que `add_my_role` rechaza un rol repetido
antes de llegar a la tabla, de modo que llamarla sola no prueba la clave primaria.

Una prueba obligatoria de esta clase se da por cumplida cuando el experimento está
ejecutado y su resultado anotado en `plan.md`, con la fecha. Mientras el
experimento no se haya corrido, la historia no está terminada, igual que con
cualquier otra prueba obligatoria ausente. La transversal
`everyTableHasRowLevelSecurityEnabledAndAtLeastOnePolicy` pertenece a esta misma
categoría.

La capa de dominio no depende de Android, de modo que sus pruebas corren en la
máquina virtual de Java: sin emulador, en segundos, y ejecutables en integración
continua. Esa es la razón práctica por la que la separación de capas importa.

## Nomenclatura

Nombre descriptivo en inglés, que enuncie el comportamiento esperado. No
`testCreateRequest`, sino `rejectsRequestWhenPatientHasNoPrimaryAddress`.

## Pruebas obligatorias por característica

Estas no son sugerencias. Una historia no está terminada si le falta la prueba
obligatoria que le corresponde.

### Objetos de valor

Para cada uno: el caso válido, cada caso inválido por separado, y los valores
límite exactos.

- `rejectsAmountEqualToZero`, `rejectsNegativeAmount`, `rejectsAmountWithMoreThanTwoDecimals`
- `rejectsRatingBelowOne`, `rejectsRatingAboveFive`, `acceptsRatingAtBothBounds`
- `rejectsLatitudeOutOfRange`, `rejectsLongitudeOutOfRange`
- `rejectsCoverageRadiusOutOfRange`
- `normalizesEmailToLowercaseAndTrimmed`

### Autenticación y perfiles

- `createsProfileOnFirstSignIn`
- `adminRoleIsNeverSelfAssignable`
- `sessionPersistsAcrossApplicationRestart`

Roles múltiples, desde el Sprint 2.5:

- **Obligatoria:** `activeRoleMustBeOneOfTheHeldRoles`
- **Obligatoria:** `rejectsAddingARoleThePersonAlreadyHolds`
- **Obligatoria:** `rejectsSwitchingToARoleThePersonDoesNotHold`
- **Obligatoria:** `aPersonWithBothRolesEditingAsPatientWritesNoProfessionalData`
- `switchFailureKeepsThePreviousActiveRole`
- `theSwitchIsHiddenForSomeoneWithASingleRole`

> **`assignsRoleOnlyOnceAndRejectsSecondAssignment` se retiró el 2026-10-01.**
> Expresaba que el rol se elige una sola vez, y esa dejó de ser la regla cuando el
> Sprint 2.5 hizo que una persona pueda tener paciente y profesional a la vez.
> `rejectsAddingARoleThePersonAlreadyHolds` ocupa su lugar: la regla que queda no
> es que no haya un segundo rol, sino que no haya dos veces el mismo. No se apagó
> una prueba en rojo; se corrigió una prueba que expresaba una regla derogada, que
> es el segundo de los dos caminos que esta misma regla admite. Ver
> `docs/decisions.md`, 2026-10-01.

### Ubicación

- **Obligatoria:** `nearbySearchReturnsOnlyProfessionalsWithinRadius`
- **Obligatoria:** `nearbySearchOrdersResultsByAscendingDistance`
- **Obligatoria:** `nearbySearchExcludesUnverifiedAndInactiveProfessionals`
- **Obligatoria:** `nearbySearchUsesTheProfessionalBaseAndNotThePrimaryAddress`
- **Obligatoria:** `nearbySearchExcludesTheCallerFromTheirOwnResults`
- `nearbySearchUsesSpatialIndexAndNotSequentialScan`
- `geocodingIsCachedAndNotRequestedTwiceForTheSameAddress`
- `markingAddressAsPrimaryUnmarksThePreviousOne`
- `markingAddressAsProfessionalBaseUnmarksThePreviousOne`
- **Obligatoria:** `deletingAMarkedAddressNeverLeavesThePersonWithoutOne` — quien
  conserva direcciones conserva una principal. El disparador que la marca solo
  actúa al insertar, de modo que eliminarla no promovía a nadie; se reprodujo en
  dispositivo el 2026-10-03 y es el estado en el que estaban las filas del autor
- `deletingAMarkedAddressWithSeveralSurvivorsAsksWhoInheritsIt` — con más de una
  superviviente la heredera la elige la persona, no la aplicación. Vale sobre todo
  para la base profesional: elegirla por ella la pone en búsquedas centradas en una
  dirección que nunca declaró para eso

### Verificación

- **Obligatoria:** `userCannotReadVerificationDocumentsOfAnotherUser`
- `professionalBecomesVisibleOnlyAfterAllDocumentsApproved`
- `rejectedDocumentExposesReasonToItsOwner`

### Solicitudes y negociación

- **Obligatoria:** `counterOfferReferencesPreviousOfferAndPreservesTheWholeThread`
- **Obligatoria:** `acceptingOfferCreatesServiceAndPendingPaymentInASingleTransaction`
- **Obligatoria:** `failedAcceptanceLeavesNoPartialRecord`
- **Obligatoria:** `aDualRoleUserNeverSeesTheirOwnRequestInTheProfessionalInbox`
- **Obligatoria:** `aDualRoleUserCannotOfferOnTheirOwnRequest`
- `issuedOfferCannotBeEdited`
- `unverifiedProfessionalCannotIssueOffer`
- `acceptedRequestRejectsNewOffers`
- `expiredRequestNoLongerAppearsInProfessionalInbox`
- `contactDetailsAreHiddenBeforeAcceptanceAndVisibleAfter`
- `acceptedAmountIsFrozenAndUnaffectedByLaterRateChanges`

### Comunicación

- **Obligatoria:** `adminCannotReadMessages`
- `messageIsDeliveredToTheOtherPartyInRealTime`
- `readReceiptIsVisibleToTheSender`

### Ciclo del servicio

- **Obligatoria:** `serviceStateTransitionsRejectInvalidJumps`
- `cannotMarkArrivalOnCancelledService`
- `cannotCancelCompletedService`
- `cancellationRequiresReason`

### Pagos

- **Obligatoria:** `paymentReachesConfirmedOnlyWhenBothPartiesConfirm`
- **Obligatoria:** `totalAlwaysEqualsPlatformFeePlusProfessionalAmount`
- `mismatchedConfirmationsLeavePaymentInDispute`
- `commissionIsComputedFromTheConfiguredPercentage`
- `settlementReducesTheProfessionalPendingBalance`

### Calificaciones e historial

- **Obligatoria:** `ratingIsUniquePerServiceAndAuthor`
- **Obligatoria:** `authorAndRecipientCanNeverBeTheSamePerson`
- **Obligatoria:** `reputationAsAProfessionalExcludesRatingsReceivedAsAPatient`
- **Obligatoria:** `ratingsReceivedAsAPatientNeverBecomePublic`
- `ratingOutsideOneToFiveIsRejected`
- `reputationIsRecalculatedOnEachNewRating`
- `cancelledServiceCannotBeRated`
- `historicalServiceKeepsItsFrozenAmountAfterRateChange`

### Internacionalización

- **Obligatoria:** `everyStringKeyUsedInCodeExistsInAllLocales`
- `noVisibleTextIsHardcodedInScreens` — verificado por análisis estático
- `domainErrorsCarryTypesNotMessages` — ningún resultado de la capa de dominio
  transporta una frase destinada al usuario

### Interfaz

- `screenRendersAllFourStates` — cargando, vacío, con contenido y error
- `contentDescriptionsArePresentOnMeaningfulIcons`
- **Obligatoria para toda pantalla nueva:** una prueba de interfaz que, al 200 % de
  fuente y a 320 dp, afirme que cada control de la pantalla se alcanza, se muestra y
  no se sale de los bordes. El ayudante está en `app/src/test/java/bo/saludencasa/ui/LargeFont.kt`
- **Obligatoria, como previsualización y no como prueba:** toda pantalla tiene
  `@Preview` en claro, en oscuro y con `fontScale = 2f`. Desde el 2026-10-08 la
  prueba de interfaz cubre las filas que dejan de componerse y los controles que se
  salen de la pantalla, pero **el texto recortado dentro de su contenedor solo se ve
  mirando**, así que la previsualización sigue siendo obligatoria y hay que abrirla:
  compilar no es mirar, y el cierre de HU-09 declaró cumplido ese punto antes de que
  nadie la hubiera abierto

## Pruebas transversales obligatorias

Estas verifican invariantes del sistema y deben existir aunque ninguna historia
las mencione.

- **`everyTableHasRowLevelSecurityEnabledAndAtLeastOnePolicy`** — consulta el
  catálogo del sistema y falla si alguna tabla del esquema público queda
  descubierta. Es la red de seguridad de INV-02.
- **`userCannotReadRowsOfAnotherUser`** — parametrizada sobre las tablas que
  contienen datos personales.
- **`domainLayerHasNoPlatformImports`** — recorre los archivos bajo `domain/` y
  falla si alguno importa `androidx.*`, `android.*` o el cliente de Supabase. Es
  la red de seguridad de la regla de dependencia, que en un proyecto de un solo
  módulo no la hace cumplir el compilador.

## Qué no se prueba

- Código generado.
- Configuración de bibliotecas de terceros.
- Getters y setters triviales.
- La biblioteca de un tercero. Se prueba el código propio, no el ajeno.

## Estilo

- Sin `Thread.sleep`. Para concurrencia, sincronización explícita; para flujos,
  Turbine.
- Cada prueba verifica un comportamiento. Si el nombre necesita un «y», son dos
  pruebas.
- Los datos de prueba se construyen con funciones de fábrica, no repitiendo
  constructores completos en cada prueba.
- Una prueba que falla debe indicar por qué con solo leer su nombre y su aserción.

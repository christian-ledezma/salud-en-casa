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
| Interfaz | `app/src/androidTest` | Dispositivo o emulador |

La estructura de paquetes de las pruebas replica la del código fuente.

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
- `assignsRoleOnlyOnceAndRejectsSecondAssignment`
- `adminRoleIsNeverSelfAssignable`
- `sessionPersistsAcrossApplicationRestart`

### Ubicación

- **Obligatoria:** `nearbySearchReturnsOnlyProfessionalsWithinRadius`
- **Obligatoria:** `nearbySearchOrdersResultsByAscendingDistance`
- **Obligatoria:** `nearbySearchExcludesUnverifiedAndInactiveProfessionals`
- `nearbySearchUsesSpatialIndexAndNotSequentialScan`
- `geocodingIsCachedAndNotRequestedTwiceForTheSameAddress`
- `markingAddressAsPrimaryUnmarksThePreviousOne`

### Verificación

- **Obligatoria:** `userCannotReadVerificationDocumentsOfAnotherUser`
- `professionalBecomesVisibleOnlyAfterAllDocumentsApproved`
- `rejectedDocumentExposesReasonToItsOwner`

### Solicitudes y negociación

- **Obligatoria:** `counterOfferReferencesPreviousOfferAndPreservesTheWholeThread`
- **Obligatoria:** `acceptingOfferCreatesServiceAndPendingPaymentInASingleTransaction`
- **Obligatoria:** `failedAcceptanceLeavesNoPartialRecord`
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

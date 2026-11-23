# Sprint 4 — Catálogo y búsqueda por cercanía

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-22 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.


**Estado:** `[x]` cerrado el **2026-10-11**, cuatro días antes de su fecha de corte.
Las tres historias terminadas —HU-10, HU-11 y HU-12— y los 23 puntos planificados
entregados. Ninguna se trasladó. Cinco migraciones aplicadas, y las 34 del proyecto
reconstruyen el esquema sin deriva.

**Objetivo del sprint.** El paciente encuentra profesionales verificados cerca de
su domicilio, filtrados por el tipo de atención que necesita.

**Objetivo específico.** 3 · Comunicación, coordinación y agenda.

**Puntos:** 23.

**Fecha de corte: 7 días corridos desde el primer día del sprint** (decidido el
2026-10-08). El sprint se cierra ese día aunque queden historias abiertas: lo que no esté
cerrado se traslada, y lo cerrado se cuenta. Es la primera vez que un sprint de este
proyecto cierra por plazo y no por alcance, y por eso es la primera medición de velocidad
que mide capacidad. **Primer día del sprint: 2026-10-09. Fecha de corte: 2026-10-15**,
siete días corridos contando los dos extremos.

### HU-10 · Declarar los servicios que presto `[x]` — 5 puntos

> Como **profesional**, quiero **indicar qué tipos de atención presto y a qué
> precio**, para **aparecer en las búsquedas correctas**.

**Criterios de aceptación**

- [x] Dado que abro mis servicios, cuando consulto el catálogo, entonces veo los
      tipos de atención disponibles. **Verificado.** La pantalla lista el catálogo
      completo en su segunda sección, con nombre, descripción, precio de referencia y
      duración estimada de cada tipo. `CatalogMapperTest` fija que el emparejamiento de
      las dos lecturas conserva el precio propio y no el de referencia;
      `MyServicesScreenLargeFontTest.theCatalogEntryLeftToDeclareKeepsItsActionOnScreen`
      fija que la tarjeta del catálogo y su acción se alcanzan a 320 dp y 200 % de fuente.
      La política `service_types_select_all` ya filtra por `active`, de modo que un tipo
      desactivado no llega: no hay condición equivalente en el cliente.
- [x] Dado que selecciono un tipo, cuando fijo su precio de referencia, entonces
      queda asociado a mi perfil. **Verificado** por
      `MyServicesViewModelTest.confirmingAPriceDeclaresTheTypeAndReadsTheListAgain`
      —el tipo y el precio confirmados llegan al repositorio y la lista se relee— y por
      `theDialogOpensWithTheCatalogReferenceAlreadyFilled`, que fija que el campo abre con
      el precio de referencia del catálogo. **Y contra el proyecto remoto** (experimento
      1b): dos tipos distintos se insertan con `active = true`.
- [x] Dado que intento agregar dos veces el mismo tipo, cuando guardo, entonces se
      rechaza. **Verificado el 2026-10-09 con experimento SQL** contra el proyecto
      remoto, dentro de transacciones con `rollback`: el segundo insert del mismo tipo
      falla con `23505` sobre
      `professional_services_professional_id_service_type_id_key`, y **el control
      discrimina**, porque dos tipos distintos en la misma transacción pasan. En el
      cliente lo cubren
      `DeclareServiceUseCaseTest.refusesATypeThatIsAlreadyDeclaredAndWritesNothing`
      —y su par `declaresATypeTheProfessionalDoesNotOfferYet`, para que el rechazo no sea
      un «deniega todo»—, `DeclaredServicesTest` sobre los tipos por declarar, y
      `CatalogErrorMapperTest`, que traduce la violación del índice y **no** confunde con
      ella la restricción del precio de la misma tabla.
- [x] Dado que no declaro ningún servicio, cuando un paciente filtra por tipo,
      entonces no aparezco. **Verificado el 2026-10-09 con experimento SQL** contra el
      proyecto remoto. Es el criterio más importante de la historia porque es el que
      conecta con HU-11, y el control discrimina en las dos direcciones (experimentos 3a
      y 3b).

**Requisitos:** RF-02.3, RF-05.1, RF-05.2.

> **Sobre RF-05.2.** «El paciente selecciona el tipo de servicio que requiere al crear una
> solicitud» no se cumple aquí: su consumidor es HU-13, en el Sprint 5. Lo que HU-10 deja
> listo es el catálogo leíble por cualquier usuario autenticado y el caso de uso que lo
> entrega. Se anota para que el criterio no se dé por cubierto al revisar el requisito.

**Qué cambió al cerrarla.**

- **El esquema no cambió, y eso se comprobó antes de escribir código.**
  `service_types` y `professional_services` existen desde el Sprint 0 con sus tres
  políticas y con el índice único que sostiene el tercer criterio. La única migración de
  la historia es de dato: `20261009130000_accent_service_type_catalog.sql`, que acentúa
  el nombre y la descripción de los doce tipos sembrados. HU-10 es la primera historia
  que los muestra a un usuario. Aplicada con `db push` tras `--dry-run`, con una sola
  migración pendiente.
- **Característica nueva, `features/catalog/`**, dueña de las dos tablas. El catálogo es
  dato de plataforma, no dato del perfil de nadie, y HU-11, HU-12 y HU-13 lo necesitan.
  Razonamiento en `docs/decisions.md`, 2026-10-09.
- **La historia incluye corregir el precio y quitar un servicio**, que los criterios no
  piden. Decidido con el autor al empezar: con solo declarar y el rechazo del duplicado,
  un precio mal escrito queda publicado en las búsquedas y no hay puerta para
  arreglarlo. Quitar es un `delete`, no un `active = false`: el índice único es sobre el
  par profesional–tipo y no sobre las filas activas, así que una fila apagada impediría
  volver a declarar ese tipo. Registrado en `docs/decisions.md`, 2026-10-09.
- **Puerta de entrada.** «Mis servicios», desde la sección profesional de «Mi perfil»,
  que ya está condicionada al rol activo `PROFESSIONAL`. Declarar un servicio es una
  afirmación que un paciente no puede hacer, igual que la disponibilidad inmediata.
- **Experimentos SQL sobre el proyecto remoto**, 2026-10-09, todos dentro de
  transacciones con `rollback`. Sin residuo: `professional_services` sigue con **0**
  filas al terminar, comprobado.

  | # | Qué se comprobó | Resultado |
  |---|---|---|
  | 1a | Declarar dos veces el mismo tipo | **Rechazado**, `23505` sobre `professional_services_professional_id_service_type_id_key` |
  | 1b | Declarar dos tipos distintos | **Pasa**, 2 filas con `active = true`. El control discrimina |
  | 2 | Declarar en nombre de otro profesional | **Rechazado por la política**, `42501` |
  | 2b | Declarar desde una cuenta sin rol profesional | **Rechazado por la clave foránea**, `23503` contra `professionals` |
  | 3a | Filtrar la búsqueda por tipo sin nada declarado | **0 resultados**, contra **1** sin filtro. El profesional existe y el filtro lo excluye |
  | 3b | Declarar el tipo y repetir el filtro | **1 resultado** para el tipo declarado y **0** para otro tipo. El filtro discrimina |
  | 4a | Corregir el precio propio | **Pasa**, la fila queda en 195,50 |
  | 4b | Quitar lo propio | **Pasa**, 0 filas |
  | 4c | Otro usuario intenta cambiar y borrar la fila ajena | **No la toca**: la ve (proyección pública, RF-02.6) y sigue en 180,00 y existiendo |

  **El experimento 4 hubo que reescribirlo.** La primera versión encadenaba el insert, el
  update y el delete como CTE hermanas de una sola sentencia, y daba 0 filas afectadas:
  las CTE que escriben comparten la instantánea y no ven lo que insertó su hermana. No era
  un rechazo de la política. Queda anotado porque el síntoma —0 filas— es idéntico al de
  una política que deniega, y confundirlos habría dado por verificado lo contrario de lo
  que pasaba.

- **El ayudante de disposición gana una variante.**
  `assertFitsTheScreenWithoutScrolling`, en `app/src/test/java/bo/saludencasa/ui/LargeFont.kt`,
  para un control cuyo contenedor no desplaza, como los botones de un `AlertDialog`; y la
  medición del borde de la pantalla toma la primera raíz, porque un diálogo abierto es una
  segunda ventana y `onRoot()` encontraba dos nodos. Razonamiento en `docs/decisions.md`,
  2026-10-09.

**Lo que las pruebas de esta historia sí atrapan, verificado por mutación el 2026-10-09.**

| Mutación | Resultado |
|---|---|
| La lista deja de desplazarse (`userScrollEnabled = false`) | **Fallan 4 de 6** pruebas de disposición |
| La acción «Quitar» deja de componerse | **Falla** `bothActionsOfADeclaredServiceStayOnScreen` |
| El diálogo del precio pierde su desplazamiento | **Falla**, pero por la razón equivocada (ver abajo) |
| **Las dos acciones de la tarjeta en un `Row` en vez de un `FlowRow`** | **PASA. No lo atrapa** |

**La última fila es el punto ciego del 2026-10-08, en vivo.** Se intentó esa mutación
esperando que fallara y pasó: la etiqueta se recorta **dentro de su propio botón**, Compose
la mide al ancho que el contenedor le da, y ni los límites ni la semántica delatan nada. El
`FlowRow` se queda porque es la disposición correcta, pero **ninguna prueba lo sostiene**, y
el comentario del código lo dice con esas palabras en vez de afirmar lo contrario. Esta es
la tercera vez que esa forma de defecto aparece en el proyecto y sigue dependiendo de mirar
las previsualizaciones.

**Y se quitó un `verticalScroll` que se había agregado al contenido de los dos diálogos.**
Sin él, a 320 dp y 200 % de fuente **no se recorta nada**: se comprobó midiendo con la
variante que no desplaza. Dejarlo habría sido programación defensiva sin prueba que la
sostenga, y además le quitaba a la prueba su única señal, porque con el desplazamiento
puesto la pantalla ya no puede recortar. Sin él, `assertIsDisplayed` sobre el motivo del
rechazo es lo que avisaría si el contenido del diálogo creciera.

**Deuda declarada.**

- **`active` queda como columna sin escritor.** Es el interruptor de «ahora mismo no presto
  esto» que ningún requisito pide. Si se pide, el índice único hay que revisarlo primero.
  Registrado en `docs/decisions.md`, 2026-10-09.
- **Un tipo que el administrador desactive hace desaparecer de la pantalla el servicio
  declarado sobre él**, en lugar de dibujar una tarjeta sin nombre. Es observable y no hay
  aviso. Cubrirlo exige decidir qué pasa con un tipo retirado del catálogo, que es trabajo
  de la pantalla de administración del catálogo y no existe hoy. Lo fija
  `CatalogMapperTest.aDeclaredServiceWhoseTypeLeftTheCatalogIsDropped`.
- **El catálogo no es traducible.** Viaja en español desde la base, y la regla de
  internacionalización no lo alcanza porque es dato, no recurso de cadenas. El día que
  exista `values-en/`, los nombres de los tipos seguirán en español.
- **El cambio 5 de la retrospectiva del Sprint 3 se volvió a violar**, por orden de
  trabajo: los comentarios que citan `docs/decisions.md`, 2026-10-09 se escribieron antes
  que las entradas que citan. El estado final es correcto —las siete entradas existen y
  cada cita apunta a la suya—, pero el orden fue el que la retrospectiva pidió cambiar.
  Queda anotado aquí para que la retrospectiva del Sprint 4 lo recoja con el dato y no de
  memoria.
- **`supabase db reset` y `db diff --linked` no se corrieron:** Docker no estaba levantado.
  La migración es de dato y no toca el esquema, así que `db diff --schema public` no tenía
  nada que encontrar, pero la reconstrucción desde cero **sí** la ejerce —la semilla escribe
  los nombres sin acento y esta migración los corrige después— y conviene comprobarla al
  cerrar el sprint.


**Verificaciones del cierre, 2026-10-09.** `./gradlew ktlintCheck staticAnalysis
assembleDebug` concluye sin error y **sin ninguna advertencia nueva del compilador**.
`./gradlew testDebugUnitTest`: **70 suites, 394 pruebas, 0 fallos, 0 omitidas** —eran 63 y
363 antes de la historia, de modo que HU-10 agrega 7 suites y 31 pruebas.

**Archivos creados o modificados el 2026-10-09.**

| Capa | Archivos |
|---|---|
| Migración | `supabase/migrations/20261009130000_accent_service_type_catalog.sql` (nueva) |
| Dominio | `features/catalog/domain/model/ServiceType.kt` · `ProfessionalService.kt` · `DeclaredServices.kt` · `CatalogError.kt` · `CatalogResult.kt` (nuevos) · `domain/repository/ICatalogRepository.kt` (nuevo) · `domain/usecase/GetMyDeclaredServicesUseCase.kt` · `DeclareServiceUseCase.kt` · `UpdateServicePriceUseCase.kt` · `RemoveServiceUseCase.kt` (nuevos) |
| Datos | `features/catalog/data/model/CatalogDto.kt` · `data/datasource/SupabaseCatalogDataSource.kt` · `data/mapper/CatalogMapper.kt` · `data/mapper/CatalogErrorMapper.kt` · `data/repository/CatalogRepository.kt` (nuevos) |
| Presentación | `features/catalog/presentation/MyServicesScreen.kt` · `MyServicesViewModel.kt` · `CatalogErrorMessages.kt` (nuevos) |
| Inyección | `di/CatalogModule.kt` (nuevo) · `SaludEnCasaApplication.kt` |
| Navegación | `navigation/Routes.kt` (`MyServicesRoute`) · `navigation/SaludEnCasaNavHost.kt` |
| Perfil | `features/profile/presentation/ProfessionalProfileSection.kt` · `ProfileScreen.kt` (la lambda `onOpenMyServices` y las previsualizaciones) |
| Recursos | `res/values/strings.xml` · `res/values-es/strings.xml` (21 claves `catalog_`, `error_catalog_`, `profile_open_my_services` y el plural `catalog_estimated_duration`) |
| Pruebas | `test/features/catalog/FakeCatalogRepository.kt` · `domain/model/DeclaredServicesTest.kt` · `domain/usecase/DeclareServiceUseCaseTest.kt` · `UpdateServicePriceUseCaseTest.kt` · `data/mapper/CatalogMapperTest.kt` · `CatalogErrorMapperTest.kt` · `presentation/MyServicesViewModelTest.kt` · `MyServicesScreenLargeFontTest.kt` (nuevos, 7 suites) · `test/ui/LargeFont.kt` (la variante que no desplaza) |
| Documentos | `.claude/rules/glosario.md` (`DeclaredServices`, `undeclaredTypes`) · `docs/decisions.md` (siete entradas del 2026-10-09) · `plan.md` (esta sección) |

### HU-11 · Buscar profesionales cerca de mi domicilio `[x]` — 13 puntos

> Como **paciente**, quiero **ver qué profesionales hay cerca de mi dirección**,
> para **elegir a quién solicitar la atención**.

**Criterios de aceptación**

- [x] Dado que tengo una dirección registrada, cuando abro la búsqueda, entonces
      veo los profesionales dentro del radio sobre un mapa. **La función no devolvía
      ninguna coordenada**, así que el marcador no tenía de dónde salir: la migración
      `20261009140000` agrega el punto de la base profesional, redondeado a la manzana.
      Verificado contra el remoto y con `ProfessionalSearchMap`.
- [x] Dado que cambio a vista de lista, cuando la consulto, entonces veo distancia,
      tarifa base y reputación de cada uno. `ProfessionalCard`, montada por primera vez;
      verificado por `aResultCardKeepsItsNameDistanceRateAndActionOnScreen`.
- [x] Dado que filtro por tipo de servicio, cuando aplico el filtro, entonces solo
      aparecen quienes lo declararon. El filtro viaja como `p_service_type_id`;
      verificado por `choosingATypeAndAvailabilityNarrowsWhatIsAsked` y por el
      experimento 3b de HU-10 contra el remoto.
- [x] Dado que filtro por disponibilidad inmediata, cuando aplico el filtro,
      entonces solo aparecen quienes están disponibles ahora. El parámetro es de tres
      estados y el filtro apagado viaja como `null`;
      `anAvailabilityFilterThatIsOffTravelsAsNothingAndNotAsFalse`.
- [x] Los resultados están ordenados por distancia ascendente. Experimento 2:
      1000, 1200, 2000, 4000 y 4000 metros, con el desempate por `id` idéntico en dos llamadas.
- [x] Solo aparecen profesionales verificados y activos. Experimento 3: un `PENDING`,
      un `REJECTED` y un `active = false` a kilómetro y medio, ninguno aparece.
- [x] Con quinientos profesionales cargados, la búsqueda responde en menos de un segundo.
      **64 ms** medidos con `explain (analyze)` sobre 500 profesionales sintéticos
      (experimento 6). RNF-01 pide menos de 1000.

**Requisitos:** RF-06.1 a RF-06.5, RNF-01, RN-01, RN-02, INV-08.

**Lo que ya estaba hecho desde el Sprint 0, y nunca se había llamado.**
`search_nearby_professionals` ya filtraba por radio, tipo y disponibilidad, ya ordenaba por
distancia, ya exigía `APPROVED` y `profiles.active`, ya excluía al llamante (RN-13) y ya unía
con la base profesional y no con el domicilio (RN-02). Ningún archivo Kotlin la mencionaba.
Igual con media docena de componentes —`ProfessionalCard`, `SegmentedControl`, `RatingBadge`,
`AvailabilityDot`, `RadioOptionGroup`— construidos a partir de `docs/design-system.md` y
jamás montados. HU-11 fue en buena parte el ensamblaje de piezas que llevaban un mes esperando.

#### La decisión que gobernó la historia: la ubicación se publica por manzanas

La función devolvía `distance_m` **exacta** —se midió: `1062.76035493` m— y recibe
`p_latitude`, `p_longitude` y `p_radius_km` como parámetros libres, con
`grant execute to authenticated`. Tres llamadas desde tres orígenes trilateran la base al
metro, y una búsqueda binaria sobre el radio llega sola al mismo sitio. De modo que **la
coordenada exacta del profesional ya era pública antes de que existiera el mapa**, y redondear
solo el marcador habría sido cosmético: la resolución publicada es la del canal más fino que se
expone.

La migración `20261009140000` cuantiza **los tres canales** a la misma rejilla de una manzana:
el punto a tres decimales (~110 m), la distancia a la centena de metros, y el radio pedido a
décimas de kilómetro. El orden se sigue calculando con la distancia exacta, y gana un desempate
por `id` que antes no tenía. Medido después: el punto sale `-17.368, -66.174` donde la base
está en `-17.3676355, -66.1742961`, la distancia sale `1100`, y el borde del radio solo
discrimina de cien en cien metros (1,00 y 1,04 fuera; 1,06 dentro). Razonamiento completo en
`docs/decisions.md`, 2026-10-09.

#### La deuda que el Sprint 3 le asignó, resuelta

`professionals_select_counterpart` concedía lectura de la **fila completa** de `professionals`
—`verification_status` incluido— a quien compartiera un servicio. Se retiró
(`20261009141000`): la contraparte lee `professional_directory`, que por construcción solo
contiene profesionales `APPROVED` y activos. Que la pertenencia a esa vista **sea** la
verificación es la forma estructural de RF-04.6. Verificado con el experimento 8, que
discrimina: con un servicio compartido, la contraparte **no** lee la fila y **sí** lee la
proyección y su `profiles`.

#### Experimentos SQL contra el proyecto remoto, 2026-10-09

Ocho, todos dentro de transacciones que terminan en `rollback`. Los cinco obligatorios de
«Ubicación» necesitaban profesionales sintéticos, y `profiles.id` referencia `auth.users`: el
preparativo **suelta esa clave foránea dentro de la transacción**, porque en PostgreSQL el DDL
es transaccional y el `rollback` la restituye. Comprobado después: la clave está, y no quedó
ni un perfil, ni una dirección, ni una fila de profesional sintética.

| # | Prueba | Resultado |
|---|---|---|
| 1 | `nearbySearchReturnsOnlyProfessionalsWithinRadius` | **Pasa y discrimina.** 5 dentro de 5 km; el de 33 km aparece al pedir 40. Y el de cobertura 1 km a 3 km de distancia **no** aparece: RN-02 son dos radios, no uno |
| 2 | `nearbySearchOrdersResultsByAscendingDistance` | **Pasa.** 1000, 1200, 2000, 4000 y 4000 metros en ese orden, y los dos empatados salen igual en dos llamadas seguidas |
| 3 | `nearbySearchExcludesUnverifiedAndInactiveProfessionals` | **Pasa.** `PENDING`, `REJECTED` y `active = false` a 1,5 km: ninguno |
| 4 | `nearbySearchUsesTheProfessionalBaseAndNotThePrimaryAddress` | **Pasa.** Domicilio a 33 km, base a 1,2 km: aparece a 1200 m. La distancia se mide desde la base |
| 5 | `nearbySearchExcludesTheCallerFromTheirOwnResults` | **Pasa.** El llamante, con los dos roles y una base a 800 m —más cerca que nadie—, no aparece |
| 6 | `nearbySearchUsesSpatialIndexAndNotSequentialScan` + RNF-01 | **Pasa sin forzar nada.** `Index Scan using idx_addresses_location` con `Index Cond: location && _st_expand(...)`, y ni un recorrido secuencial en el plan. **61 ms** el cuerpo, **64 ms** la función entera, con 500 profesionales. No hizo falta `enable_seqscan = off` |
| 7 | Trilateración, antes y después | **La fuga existía y quedó acotada.** Antes: `distance_m = 1062.76035493`, once cifras, desde un origen elegido por el llamante con una cuenta de paciente. Después: `1100`, punto en la manzana, y el borde del radio quantizado a 100 m |
| 8 | La contraparte sin la política | **Discrimina.** `shares_service = true`, lee la fila de `professionals` 0 veces, la proyección pública 1 y su `profiles` 1 |

**Lo que el experimento 6 enseñó de método.** La función es `language sql` pero **no se
inlinea**: `explain` sobre ella muestra un `Function Scan` y esconde el plan interno. El plan
de verdad hay que pedirlo sobre el cuerpo de la consulta. Anotar los 64 ms sin mirar el plan
habría dado por buena la cláusula del índice sin haberla visto nunca.

#### Mutaciones, con su resultado

| Mutación | Resultado |
|---|---|
| La página deja de desplazar (`userScrollEnabled = false`) | **11 de 11** pruebas de disposición fallan |
| La etiqueta de una opción se fija a una línea | **Falla** `theLongestServiceTypeWrapsInsteadOfHidingItsTail`, y lo dice con un número: «Both rows are 48.0.dp tall» |
| La etiqueta de una **ficha** se fija a una línea (diseño anterior) | **PASÓ.** Es la razón por la que el control cambió; ver abajo |

#### Dos correcciones que la medición impuso, y no la planificación

**El mapa dejó de ser el fondo de la pantalla.** Se construyó primero al estilo de Uber, con
el mapa a pantalla completa y los controles flotando encima. La prueba al 200 % de fuente sobre
320 dp lo tumbó: una columna flotante **no desplaza**, y a ese tamaño la cabecera, el origen,
el selector y los filtros suman más que el alto de la pantalla. Quedó como una sola página que
desplaza, con el mapa de `Spacing.mapHeight` entre los filtros y los resultados. El idioma de
Uber se conserva donde de verdad vive, que es el movimiento.

**El filtro por tipo dejó de ser una fila de fichas.** La ficha del nombre más largo del
catálogo medía **470 dp en una pantalla de 320**. Se le puso tope de ancho y la prueba volvió a
pasar —y eso era lo engañoso: con 240 dp la etiqueta siguió en una sola línea de 32,5 dp, es
decir con más de la mitad del nombre oculta **dentro de la propia ficha**. Pasó a ser un
`RadioOptionGroup` plegado, que es lo que el sistema de diseño reserva para etiquetas largas.
Las fichas se quedan donde sus etiquetas son cortas: los tres radios y «Disponibles ahora».

**Y una lección sobre cómo se escribe esta clase de aserción.** La primera versión comparaba la
altura contra los 32 dp de reposo de Material y pasaba, porque al 200 % de fuente una sola
línea ya los supera. Se mutó el código y **la prueba siguió pasando**: afirmaba algo que no
sostenía. La que quedó compara la opción larga contra una opción **corta del mismo árbol**.
Es la comparación relativa, y no la medida absoluta, lo que permite decir algo sobre el recorte
interior.

#### Cinco defectos que la revisión final encontró, y ninguna prueba veía

- **El toque en un pin no llegaba a ninguna parte.** El mapa no avisaba de la selección, de
  modo que `selectedId` no podía cambiar nunca y la tarjeta del profesional elegido era código
  inalcanzable. Corregido: `onClusterItemClick` devuelve `true` —consume el toque, y con ello
  impide que el SDK abra su propia ventanita encima— y un toque en el mapa fuera de un pin
  guarda la tarjeta.
- **La pantalla se quedaba en «Buscando…» para siempre** si alguien tocaba un filtro antes de
  que el origen resolviera: el cambio de criterio cancelaba el trabajo que estaba resolviendo
  la dirección, y la búsqueda que lo reemplazaba no tenía origen alrededor del que buscar, así
  que se iba sin pedir nada. Reproducido con
  `changingTheRadiusBeforeTheOriginResolvedStillSearches` —falla con «List is empty», cero
  peticiones— y corregido resolviendo el origen de nuevo en vez de cancelarlo. La pantalla se
  recuperaba al salir y volver, que es justo lo que hace que un defecto así sobreviva a una
  demostración.

- **La distancia desaparecía de la tarjeta de quien no tiene calificaciones.** La fila que
  lleva el distintivo lleva también la distancia, y sin calificaciones el distintivo lo
  reemplaza una frase: en un `Row` llano el segundo hijo solo recibe el ancho que dejó el
  primero, que al 200 % de fuente era ninguno. `assertIsDisplayed` **falla**: la distancia no se
  dibujaba en absoluto, y los profesionales sin calificar son el caso común en una plataforma
  joven. Corregido con un `FlowRow` en el componente, y la prueba mide contra la distancia de la
  otra tarjeta, que es una cadena **más larga**: una línea más corta no puede ser legítimamente
  más alta que una más larga del mismo estilo.
- **El pie de la lista se quedaba girando para siempre** si llegaba una reanudación —una
  rotación, o volver de una ficha— mientras una página estaba en vuelo. `refresh` cancelaba ese
  trabajo sin bajar su bandera, y la lista solo vuelve a pedir página con la bandera baja.
  Corregido cancelándolo únicamente donde la lista se reemplaza de verdad, y probado con el
  portillo del falso.
- **La cámara encuadraba el círculo contra el ancho de la pantalla**, no contra el recuadro que
  el mapa ocupa: el mapa va con el margen de la página a los lados y mide 280 dp de alto, de
  modo que el borde del radio y los resultados más cercanos a él quedaban fuera de vista por los
  dos ejes. Corregido midiendo el recuadro con `BoxWithConstraints` y tomando su lado más corto;
  `fitZoom` ya no depende del ancho de la pantalla. **Ninguna prueba podía verlo**: la aritmética
  del zoom era correcta y es lo único que `FitZoomTest` mira.

Y una simplificación del mismo repaso: `rowsReturned` viajaba desde el repositorio hasta el
caso de uso para que el final de la lista se decidiera con las filas que el motor envió y no
con las que sobrevivieron a la lectura. **Nunca difieren**: una fila ilegible falla la página
entera, así que nada se descarta nunca. Se quitó, junto con la prueba que lo ejercía, porque
simulaba un estado que el repositorio no puede producir.

#### Lo que ninguna prueba de este proyecto ve

- **El radar y el resorte del círculo del radio.** No tienen semántica que afirmar. Se comprobó
  antes de diseñarlos que una animación infinita **no** cuelga el reloj de Robolectric —Compose
  la cancela en las pruebas—, de modo que no hubo que diseñar alrededor de un problema
  inexistente. Pero que se vean bien lo dicen las previsualizaciones y el emulador.
- **El texto recortado dentro de su contenedor**, salvo cuando hay una forma corta al lado
  contra la que medir. Es lo que acaba de costar dos diseños.
- **El mapa real.** Viaja como slot, de modo que ni la previsualización ni la prueba tocan el
  SDK. Lo que se ve dentro del recuadro solo se comprueba en un dispositivo.

#### Deuda declarada

- **La paginación se pide desde el pie de la lista**, porque un mapa no tiene pie. En modo mapa
  se dibujan los resultados ya cargados: los 20 más cercanos. Con el radio como está, nadie va
  a notarlo; el día que haya cientos de profesionales en una ciudad, el mapa necesitará su
  propia forma de pedir más.
- **El distintivo de «verificado» por profesional sigue sin existir** (RF-04.6). HU-11 definió
  **qué** ve la contraparte —la proyección pública— y lo hizo cumplir retirando la política;
  dibujar el distintivo es criterio de HU-12. Mientras tanto la línea de la cuenta de
  resultados dice «profesionales verificados», que hace visible RF-06.5 sin componente nuevo.
- **`ProfessionalType.labelRes()` se usa desde `features/search/presentation`**, es decir
  presentación de una característica leyendo presentación de otra. Ninguna de las tres reglas
  de arquitectura lo prohíbe y las cuatro cadenas ya existen, pero el sitio natural de ese
  enumerado compartido sería `core/`. Moverlo toca tres características y no es trabajo de esta
  historia.
- **El remoto sigue con 0 filas en `professional_services`.** El filtro por tipo devolviendo
  cero resultados es correcto, no un defecto.
- **La reproducibilidad del esquema quedó verificada el mismo día**, en cuanto hubo Docker, y
  con ello se cierra el pendiente que HU-10 había dejado abierto. Ver abajo.

#### Verificaciones del cierre

`./gradlew build`, `ktlintCheck`, `staticAnalysis` y `assembleDebug`: sin error y **sin ninguna
advertencia nueva del compilador**. `./gradlew testDebugUnitTest`: **78 suites, 451 pruebas, 0
fallos, 0 omitidas** (eran 70 y 394 al cerrar HU-10). Las dos migraciones aplicadas con
`db push` tras `--dry-run`, con los `revoke` y `grant` repetidos a mano porque el `drop` de una
función se los lleva y PostgreSQL vuelve a conceder `execute` a `public` por omisión.

#### Reproducibilidad del esquema, verificada el 2026-10-09

Pendiente desde HU-10 por falta de Docker, y resuelto al levantarlo.

- **`supabase db diff --linked --schema public`: «No schema changes found».** Y lo que hizo
  para llegar ahí vale más que el resultado: aplicó **las 31 migraciones desde cero** sobre una
  base sombra, las dos de esta historia incluidas, sin un error. De modo que la historia de
  migraciones reconstruye el esquema **y** reproduce exactamente el del proyecto remoto, sin
  desvío.
- **`supabase db reset`, sin `--linked`**, sobre la pila local. Hacía falta además del diff
  porque **un diff compara esquema y no datos**, y la migración de los acentos
  (`20261009130000`) es de dato: su efecto no aparece en ninguna comparación de esquema. En la
  base reconstruida los doce tipos salen con sus acentos donde el español los pide —
  «Aplicación de inyectables», «Colocación y control de vía venosa», «Consulta médica general»,
  «Curación de heridas», «Terapia física y rehabilitación»—, que es la secuencia que esa
  migración tenía que demostrar: la semilla los escribe sin acento y la migración los corrige
  después, siempre en ese orden.
- En la misma base reconstruida: `professionals` tiene **4 políticas** y
  `professionals_select_counterpart` **no está**, y `search_nearby_professionals` devuelve sus
  **trece** columnas, con `base_latitude` y `base_longitude` entre ellas. La pila local se
  detuvo al terminar.


#### El cuarto canal, encontrado y cerrado el 2026-10-11

El cierre del 2026-10-09 enumeró tres canales sobre el punto de la base y los
cuantizó a la manzana. Eran cuatro. El que faltaba **no es un número que la
función devuelva: es la respuesta misma.**

`p_latitude` y `p_longitude` son parámetros libres, así que en vez de fijar el
origen y mover el radio —la dirección que el encaje protege— se fija el radio y se
mueve el origen. Con el radio en 5,0 km, que el encaje vuelve exactamente 5000 m y
por tanto conocido, una bisección sobre el origen encuentra un punto a 5000 m
exactos de la base; por tres puntos pasa una sola circunferencia, y su centro es la
base, al metro. Unas veinte llamadas. El segundo `st_dwithin`, el de la cobertura,
era aún más barato: nunca se encajó, y `coverage_radius_km` es público en
`professional_directory`, de modo que basta **un** punto del borde.

**Encajar el radio no cerraba esto y nunca pudo**: deja el radio discreto pero
conocido, y un radio conocido es todo lo que la trilateración necesita cuando el
origen es continuo. El arreglo no es otro redondeo: la función deja de tocar el
punto exacto una vez lo ha leído.

| # | Prueba | Resultado |
|---|---|---|
| 9 | El borde del radio, medido desde el punto exacto | **Desapareció.** 4999 m y 5001 m devuelven lo mismo. Antes del cambio, ahí había un borde nítido al metro |
| 10 | El borde del radio, medido desde el nodo publicado | **Está aquí.** 4999 devuelve la fila y 5001 no: el borde recuperable rodea el punto que el marcador ya daba |
| 11 | El borde de la cobertura, los dos orígenes | **Igual que el anterior.** Sin borde desde el punto exacto, con borde en el nodo |
| 12 | Las cinco obligatorias de «Ubicación», repetidas porque el cuerpo se reescribió | **Pasan.** 200, 1000, 1500, 4000 y 4000 metros en orden; ausentes el de 33 km, el de cobertura corta, el `PENDING`, el `REJECTED`, la cuenta inactiva y el propio llamante |
| 13 | RNF-01 e INV-08 con 500 profesionales sintéticos | **El índice sigue trabajando.** `Index Scan using idx_addresses_location` con `Index Cond: location && _st_expand(..., 5100)`, la condición del punto encajado como filtro encima, y **1,7 ms** |
| 14 | Permisos tras `create or replace` | **Intactos.** `authenticated` ejecuta, `anon` no. La firma no cambió, así que no hubo `drop` que se llevara los `grant` |

Sin residuo, y la clave foránea restituida.

**Lo que costó el índice, y cómo se pagó.** `st_dwithin` sobre una expresión
calculada no puede usar `idx_addresses_location`, porque el operando deja de ser la
columna indexada. La columna intacta conserva un prefiltro con 100 m de holgura
—media diagonal de una celda son 79 m como mucho, en el ecuador— y el punto
encajado decide después.

**La lección, que vale más que el arreglo:** cuantizar lo que una función devuelve
no acota lo que un parámetro libre puede sondear. Mientras un predicado binario
dependa de un punto exacto y el llamante controle una coordenada continua, ese
punto es recuperable por muchos decimales que se recorten a la salida. Registrado
en `docs/decisions.md`, 2026-10-11, junto con la corrección de la entrada del
2026-10-09.

#### Archivos

| Qué | Archivos |
|---|---|
| Migraciones | `20261009140000_nearby_search_on_a_city_block_grid.sql`, `20261009141000_counterpart_reads_the_public_projection.sql`, `20261011114540_nearby_search_answers_about_the_published_point.sql` |
| Dominio de la búsqueda | `SearchRadius`, `SearchCriteria`, `SearchOrigin`, `NearbyProfessional`, `SearchError`, `SearchResult`, `IProfessionalSearchRepository`, `GetSearchOriginUseCase`, `SearchNearbyProfessionalsUseCase` |
| Datos de la búsqueda | `NearbySearchDto`, `SupabaseProfessionalSearchDataSource`, `NearbyProfessionalMapper`, `NearbySearchParamsMapper`, `SearchErrorMapper`, `ProfessionalSearchRepository` |
| Presentación | `ProfessionalSearchScreen` (14 previsualizaciones), `ProfessionalSearchViewModel`, `ProfessionalSearchMap`, `SearchMapModel`, `SearchLabels` |
| Compartido | `ui/animations/RadarPulse`, `ui/animations/RadarMotion`, `core/util/DistanceFormat`, `di/SearchModule` |
| Modificados | `ui/components/ProfessionalCard` (la calificación pasa a ser opcional), `features/catalog/` (el catálogo solo, por caso de uso), `AccountScreen` y sus ocho previsualizaciones, `navigation/`, `SaludEnCasaApplication`, `libs.versions.toml`, los dos `strings.xml` (+34 entradas, 287 en total) |
| Pruebas | `FakeProfessionalSearchRepository` con sus fábricas, `GetSearchOriginUseCaseTest`, `SearchNearbyProfessionalsUseCaseTest`, `NearbyProfessionalMapperTest`, `NearbySearchParamsMapperTest`, `SearchErrorMapperTest`, `FitZoomTest`, `ProfessionalSearchViewModelTest`, `ProfessionalSearchScreenLargeFontTest` |
| Documentos | `docs/decisions.md` (7 entradas), `docs/design-system.md` (4 componentes nuevos), `.claude/rules/glosario.md`, `.claude/rules/testing.md` (cómo se afirma algo del recorte interior), `plan.md` |


### HU-12 · Consultar la ficha de un profesional `[x]` — 5 puntos

> Como **paciente**, quiero **ver el detalle de un profesional antes de
> contactarlo**, para **decidir con información**.

**Criterios de aceptación**

- [x] Dado que toco un resultado, cuando se abre la ficha, entonces veo biografía,
      especialidad, experiencia, servicios y tarifas.
      *Verificado con las nueve previsualizaciones y con
      `PublicProfileScreenLargeFontTest`. Los servicios y sus precios son la parte
      nueva: la ficha ya mostraba todo lo demás desde el Sprint 2.*
- [x] Dado que el profesional tiene calificaciones, cuando abro la ficha, entonces
      veo su promedio, su total de atenciones y los comentarios recibidos.
      *El promedio y el total ya estaban. Los comentarios se verificaron con los
      cuatro experimentos SQL de abajo, porque el remoto tiene cero: no puede
      existir una calificación sin una atención completada, y esa cadena es de los
      Sprints 5 a 9.*
- [x] Dado que el profesional está verificado, cuando abro la ficha, entonces veo
      el distintivo, pero nunca el archivo de su título.
      *El distintivo no tiene campo que lo encienda: estar en
      `professional_directory` **es** estar verificado (INV-07). El documento no
      tiene columna en la vista y no puede llegar a la pantalla (RF-04.6).*
- [x] Los comentarios se cargan paginados.
      *Experimento 2 contra el remoto y cuatro pruebas del modelo de vista.*

**Requisitos:** RF-02.6, RF-04.6, RF-12.4, RNF-08.

**Lo que ya estaba hecho.** `PublicProfileScreen` existe desde el Sprint 2 y ya
mostraba biografía, especialidad, experiencia, tarifa, disponibilidad, total de
atenciones y promedio. HU-12 agregó tres cosas —los servicios declarados, el
distintivo y los comentarios paginados— y, para la tercera, cambió la pantalla de
`Column` con desplazamiento a `LazyColumn`: un pie de paginación no existe sin ella.

#### La decisión que gobernó la historia: los comentarios no se leen de la tabla

La lectura parecía no necesitar migración. `reviews_select_visible` ya dice cuáles
son públicos —visibles, recibidos **del lado profesional** de esa atención, y sobre
alguien del directorio—, y `professional_services_select_public` ya abre los
servicios declarados de cualquier aprobado.

No bastaba, y lo encontró la exploración del código antes de escribir una línea:
`reviews_select_own` es una **segunda política permisiva** sobre la misma tabla.
La seguridad a nivel de fila une sus políticas con un `or`, de modo que el único
lector que es a la vez el sujeto de la ficha y quien la abre —un profesional
mirando su propia vista previa, que la aplicación ofrece desde el Sprint 2— lee
estrictamente más que un paciente.

Medido: con dos calificaciones dirigidas a la misma persona de doble rol, el
tercero vio una y la persona misma vio las dos, **incluida la recibida como
paciente**. Eso es INV-17 roto, en la pantalla que existe para mostrar lo que ve un
paciente. Ningún filtro del cliente lo repara: saber de qué lado de una atención
estuvo alguien exige leer `services`, que un tercero no ve.

La regla se mudó a una proyección, `professional_reviews`, donde
`professional_directory` ya guarda la suya. Ver `docs/decisions.md`, 2026-10-11.

#### Experimentos SQL contra el proyecto remoto, 2026-10-11

Todos con `set local role authenticated`, un `sub` ficticio y `rollback`. La cadena
completa —perfiles, roles, paciente, profesional, solicitud, oferta, atención
completada y calificación— se siembra dentro de la transacción, soltando
`profiles_id_fkey` como en HU-11.

| # | Prueba | Resultado |
|---|---|---|
| 1 | `ratingsReceivedAsAPatientNeverBecomePublic`, leído por un tercero | **Pasa y discrimina.** Dos calificaciones al mismo destinatario, las dos `visible`: el tercero ve solo la recibida como profesional |
| 2 | Paginación y comentarios ocultos | **Pasa.** Seis calificaciones con **el mismo instante**, una oculta: tres páginas de dos devuelven 5 filas y 5 distintas, la oculta nunca aparece, y la última página sale corta |
| 3 | El mismo caso 1, leído por **el dueño de la ficha** | **Falla contra la tabla, y por eso existe la migración.** Ve las dos, la recibida como paciente incluida |
| 4 | El caso 3 contra `professional_reviews` | **Pasa.** El dueño y el tercero ven exactamente lo mismo: una fila |
| 5 | Escritura a través de la vista, antes de la segunda migración | **La vista era escribible.** Un profesional autenticado reescribió con `update` la calificación que había recibido —puntaje y texto— y el disparador recalculó su promedio público a 5,00 |
| 6 | Las seis vistas del esquema, auditadas | **Solo la nueva estaba expuesta.** `my_addresses` es auto-actualizable pero `security_invoker = true`, de modo que una escritura sigue enfrentando las políticas; `professional_directory` corre como su dueño pero une dos tablas y no se puede escribir |
| 7 | La misma escritura, después de la segunda migración | **Rechazada y sigue leyendo.** `ERROR 42501: permission denied for view professional_reviews`, y la lectura del dueño devuelve la misma única fila de antes |

Sin residuo: 0 perfiles sintéticos, 0 calificaciones, 0 atenciones, 0 solicitudes y
la clave foránea restituida.

#### Mutaciones, con su resultado

| Mutación | Resultado |
|---|---|
| El nombre del servicio fijado a una línea | **Falla** `theLongestServiceNameWrapsInsteadOfBeingClipped` |
| El distintivo de verificado quitado de la pantalla | **Falla** `theVerifiedBadgeIsReachableAndFitsTheScreen`, y solo esa |
| La guarda de página en vuelo quitada del modelo de vista | **Falla** `reaching the end twice while the page loads asks for it once` |
| El `distinctBy` quitado del modelo de vista | **Falla** `a comment written while the list is open is not shown twice`. La prueba anterior, sobre páginas estáticas, **pasaba**: ver abajo |
| La guarda del pie invertida, de modo que un aviso no detiene la petición | **Falla** `aFooterCarryingAReasonWaitsForTheTap` |

**Y la misma lección de HU-11, otra vez, en su forma sutil.** La primera versión de
la aserción del nombre largo decía «más alto que el corto». La mutación **pasó**:
recortado a una línea, el nombre largo mide 35 dp contra los 33 del corto, porque
los dos no llevan los mismos glifos. Comparar contra el corto no basta; hay que
comparar contra **el doble** del corto, porque envolver significa al menos dos
líneas. Con el umbral corregido, sin mutar mide 75 dp y la mutación falla.

#### Lo que las previsualizaciones atraparon y ninguna prueba veía

La fecha del comentario se escribió primero con
`DateUtils.getRelativeTimeSpanString`, el formateador de la plataforma. La
previsualización mostró **«Sep 29, 2026»** para un comentario de doce días: pasada
una semana deja de ser relativo. Casi todo comentario real habría salido fechado y
no envejecido. Se reemplazó por un cálculo propio de cinco tramos con sus `plurals`,
con prueba unitaria en cada borde. **Tercera vez que el proyecto comprueba que
compilar no es mirar.**

#### Lo que la revisión de código encontró, y una prueba que no podía fallar

La revisión corrió con el revisor del complemento `feature-dev` sobre el trabajo
completo. Encontró **un defecto crítico que las cuatro pruebas de política no
veían**, porque todas miraban la lectura:

**La proyección era escribible.** `professional_reviews` es la primera vista del
proyecto que es a la vez **de definidor** (`security_invoker = false`) y
**auto-actualizable** —una sola tabla, sin `join`, sin agregados, con la
subconsulta confinada al `where`—. PostgreSQL encamina un `update` o un `delete`
por ella, y como corre con los privilegios de su dueño esa escritura nunca llega a
ver la seguridad de fila de `reviews`. Supabase concede todo a `authenticated`
sobre un objeto nuevo, y la primera migración solo se lo quitó a `anon`.

Medido antes de arreglarlo: un profesional reescribió la calificación que había
recibido, texto y puntaje, y el disparador le subió el promedio público a 5,00. Un
`delete` con el mismo filtro borra un comentario incómodo. `reviews` no tiene
política de `update` ni de `delete` **a propósito** —una calificación es parte del
registro—, y la vista pasaba por encima de las dos.

Se corrigió con una migración nueva, porque una aplicada no se edita, y se auditaron
las otras cinco vistas del esquema: ninguna estaba expuesta.

**Y una prueba que no podía fallar.** La revisión señaló que
`loading the next page of comments appends without repeating one` afirmaba algo que
el falso garantizaba por construcción: servía páginas estáticas con identificadores
únicos, de modo que el `distinctBy` nunca tenía nada que quitar y borrarlo no hacía
fallar nada. Se reemplazó por una que **inserta un comentario arriba entre las dos
páginas**, que es como ocurre de verdad —el desplazamiento apunta una fila más
atrás y devuelve una ya vista—. Ahora quitar el `distinctBy` la hace fallar.

El resto de lo que la revisión levantó, ya corregido: la aserción del comentario en
blanco pasaba si el mapeo entero devolvía nulo; faltaban las pruebas del reintento
de la primera página de comentarios y de la de servicios, el borde del múltiplo
exacto del tamaño de página, y la guarda del pie —que vive en la pantalla y que
ninguna prueba del modelo de vista podía alcanzar—; faltaba la previsualización del
estado fallido de los comentarios; y un comentario del código afirmaba que una llave
repetida «congela» una lista perezosa, cuando lo que hace es lanzar una excepción y
tumbar la pantalla. Esa frase estaba también en la búsqueda, de HU-11, y se corrigió
en los dos lugares.

Tres cosas que la revisión marcó ya estaban resueltas cuando llegó su informe: la
fecha relativa, las dos secciones pidiéndose a la vez en lugar de una tras otra, y
la prueba de los tramos de antigüedad.

#### Deuda declarada

- **Los comentarios no se demostraron en el emulador, y no pueden demostrarse
  todavía.** El remoto tiene 0 calificaciones y 0 atenciones; una calificación
  exige una atención completada, que exige la cadena solicitud → oferta → atención
  → pago de los Sprints 5 a 9. El recorrido del autor verá el estado vacío, que es
  **el resultado correcto**, no un defecto. El camino de datos queda verificado por
  los cuatro experimentos. HU-12 no es historia crítica, de modo que el emulador no
  era exigible.
- **El comentario no dice quién lo escribió**, por decisión del autor del
  2026-10-11. Si alguna vez se quiere el nombre, es una migración —una proyección
  pública de `profiles` para esos autores—, no un cambio de pantalla.
- **Los servicios declarados no paginan.** No hacía falta: el catálogo tiene doce
  tipos y un profesional no puede declarar dos veces el mismo, así que la lista
  está acotada por construcción, y la consulta lleva su `limit` igual (RNF-08).
- **`ScreenCatalog` no captura tres previsualizaciones de otras pantallas**
  —`ReviewRejectDialog` y las dos de precios de `MyServices`—, porque un diálogo
  abre otra raíz y el capturador dibuja la principal. Es anterior a HU-12 y no
  pertenece a esta historia; queda anotado aquí porque se vio al correrlo.

#### Verificaciones del cierre

- `./gradlew build`, `ktlintCheck`, `staticAnalysis` y `assembleDebug`: sin error y
  sin ninguna advertencia nueva del compilador.
- **83 suites, 491 pruebas, 0 fallos.** Eran 78 y 451 al cerrar HU-11.
- Las diez previsualizaciones de la ficha generadas y **abiertas**: clara, oscura,
  al 200 % de fuente, sin servicios ni comentarios, con la página de comentarios en
  error, con los servicios en error, con los comentarios en error, no publicada,
  cargando y fallida.
- Los cuatro estados resueltos en cada sección: cargando, vacío, con contenido y
  error, cada uno con su previsualización o su prueba.
- Migración aplicada al remoto con `--dry-run` previo.

#### Reproducibilidad del esquema, verificada el 2026-10-11

- **`supabase db diff --linked --schema public`: «No schema changes found».** Lo que
  hizo para llegar ahí es lo que vale: aplicó **las 34 migraciones desde cero** sobre
  una base sombra —las tres de hoy incluidas, las dos de HU-12 y la del cuarto canal
  de HU-11— sin un error, y el esquema resultante coincide con el remoto columna por
  columna.
- De modo que el remoto no tiene nada que las migraciones no digan: ningún cambio
  entró por el panel web, y el proyecto se puede reconstruir en otra cuenta corriendo
  el repositorio. Es el atributo de calidad que la defensa exige.
- La base sombra se destruye sola al terminar; no quedó ningún contenedor del
  proyecto levantado.

#### Archivos

| Qué | Archivos |
|---|---|
| Migraciones | `20261011044809_public_reviews_projection.sql`, `20261011111121_revoke_writes_on_the_public_reviews_projection.sql` |
| Dominio de calificaciones | `ProfessionalReview`, `ReviewError`, `ReviewResult`, `IReviewRepository`, `GetProfessionalReviewsUseCase` |
| Datos de calificaciones | `ReviewDto`, `SupabaseReviewDataSource`, `ReviewMapper`, `ReviewErrorMapper`, `ReviewRepository` |
| Presentación de calificaciones | `ReviewErrorMessages`, `ReviewLabels` |
| Catálogo | `ProfessionalServicesResult`, `ICatalogRepository.getServicesOf`, `CatalogRepository`, `SupabaseCatalogDataSource.findServicesOf` (renombrada), `GetProfessionalServicesUseCase` |
| Ficha | `PublicProfileScreen` (10 previsualizaciones, ahora `LazyColumn`), `PublicProfileViewModel` (`ServicesState`, `ReviewsState`, paginación) |
| Compartido | `core/util/RelativeTime`, `di/ReviewModule`, `di/CatalogModule`, `di/ProfileModule`, `SaludEnCasaApplication` |
| Textos | `values-es/strings.xml` y `values/strings.xml`, 18 claves nuevas, idénticos entre sí |
| Pruebas | `FakeReviewRepository`, `GetProfessionalReviewsUseCaseTest`, `ReviewMapperTest`, `RelativeTimeTest`, `PublicProfileViewModelTest` (14 pruebas), `PublicProfileScreenLargeFontTest` (10 pruebas), `PublicProfileReviewsFooterTest`, `FakeCatalogRepository` |
| Documentos | `docs/decisions.md` (5 entradas), `docs/design-system.md` (2 componentes), `.claude/rules/glosario.md` |

## Incremento del sprint

Un paciente abre la aplicación, ve en el mapa a los profesionales verificados que
están a menos de cinco kilómetros de su casa, ordenados por distancia y con su
tarifa, filtra por el tipo de atención que necesita, y abre la ficha de uno para
ver su biografía, sus servicios con sus precios y su distintivo de verificado.
**El diferenciador central del producto queda demostrado.**

> **Dicho con precisión, que es lo que vale en un incremento.** Se recorrió en el
> emulador el 2026-10-10 con las dos cuentas del autor, y el profesional que
> aparece es una enfermera, no un fisioterapeuta: el enunciado de la planificación
> nombraba un ejemplo, no un requisito. Lo que **no** se demostró en pantalla son
> los comentarios de la ficha, porque en el remoto no hay ninguno y no puede
> haberlo: una calificación exige una atención completada, y esa cadena es de los
> Sprints 5 a 9. El camino de datos quedó verificado con cuatro experimentos contra
> el remoto, y lo que el autor ve es el estado vacío, que es el resultado correcto.
> Es la misma trampa que el Sprint 2 documentó —prometer en el incremento algo que
> depende de un sprint posterior—, y esta vez se vio al planificar la historia y no
> al cerrarla.

## Retrospectiva

**El producto ya se puede enseñar.** Tres sprints de identidad, perfiles y
verificación construyeron las condiciones; este entregó la pantalla por la que
alguien diría que la aplicación sirve para algo. El motor de búsqueda llevaba un
mes desplegado sin que ninguna línea de Kotlin lo llamara, y buena parte del sprint
fue montar piezas que ya existían: `search_nearby_professionals`, `ProfessionalCard`,
`RatingBadge`, `SegmentedControl` y `AvailabilityDot` venían del Sprint 0.

### Velocidad medida: 23 puntos, los 23 planificados

HU-10 (5) + HU-11 (13) + HU-12 (5) = 23. Las tres cumplen la Definición de
Terminado y ninguna se trasladó. **Cinco migraciones** aplicadas, **83 suites y 491
pruebas** en verde al cierre —eran 59 suites y 331 al cerrar el Sprint 3—, y trece
experimentos SQL contra el proyecto remoto.

> El Sprint 4 se ejecutó del **09/10/2026** al **11/10/2026**.

**Tres días de calendario contra una ventana de siete, y la quinta medición seguida
que no mide capacidad.** Esto es lo más importante que esta retrospectiva tiene que
decir, porque contradice lo que el propio encabezado de este sprint prometía.

La retrospectiva del Sprint 3 pidió por cuarta vez cerrar por fecha de corte, y
esta vez **se hizo bien**: la fecha se escribió por adelantado, el 2026-10-08, en el
sprint y en `docs/decisions.md`. Y aun así el sprint cerró por alcance, cuatro días
antes del corte, porque **el trabajo se acabó primero**. Una fecha de corte solo
mide capacidad cuando el alcance no cabe; cuando sobra plazo, no ata nada y el
número vuelve a decir cuánto se planificó.

La serie completa: 21 puntos en 4 días (Sprint 1), 21 en 2 (Sprint 2), 23 en 4
(Sprint 2.5), 21 en 2 efectivos (Sprint 3) y 23 en 3 (Sprint 4). **Ninguno de los
cinco agotó su plazo.** La conclusión honesta es que este proyecto todavía no sabe
cuánto cabe en una semana, y que seguir escribiendo «la próxima vez cerramos por
fecha» no lo va a averiguar: hay que planificar un sprint con más alcance del que
se cree que cabe, y dejar que el corte muerda.

### Qué se cumplió de lo propuesto para el Sprint 4

De los siete cambios que pedía la retrospectiva del Sprint 3:

1. **Cerrar por fecha de corte: la regla se cumplió, el efecto no.** La fecha estaba
   escrita antes de empezar, que era lo que faltaba las cuatro veces anteriores. El
   sprint cerró igualmente por alcance. Ver arriba.
2. **`androidTest` resuelto antes de agregar pantallas: sí.** Las tres pantallas del
   sprint llegaron con prueba de disposición a 320 dp y 200 % de fuente, y esas
   pruebas atraparon cosas: el control que se salía, la fila que dejaba de componerse.
3. **Enumerar los escritores de las columnas que un invariante protege: se hizo, y
   faltó un escritor que no es una columna.** Para INV-17 se enumeraron las tres
   políticas de `reviews`, y ahí apareció que `reviews_select_own` es una segunda
   política permisiva. Lo que **no** se enumeró fue quién puede *escribir* a través
   de una vista nueva, y ese fue el agujero del sprint.
4. **HU-11 define qué ve la contraparte: sí.** `professionals_select_counterpart` se
   retiró y la contraparte lee la proyección pública. El distintivo de verificado lo
   trajo HU-12, sin campo propio: estar en el directorio **es** estar verificado.
5. **Escribir la entrada de `decisions.md` antes del comentario que la cita: sí**, y
   se notó. Las nueve entradas del sprint se escribieron antes que su comentario.
6. **HT-17 y la limpieza de la cuenta administradora: no se programó.** Sigue
   pendiente y sigue sin bloquear.
7. **`supabase db reset` y la deriva del esquema: sí.** `db diff --linked` el
   2026-10-11, con las 34 migraciones aplicadas desde cero sobre una base sombra: «No
   schema changes found».

### Qué funcionó

- **Las previsualizaciones atraparon un defecto real, por primera vez de forma
  explícita.** La fecha de los comentarios se escribió con `DateUtils`, el formateador
  de la plataforma, y la imagen mostró «Sep 29, 2026» para un comentario de doce días:
  pasada una semana deja de ser relativo. Ninguna prueba lo habría visto, porque el
  texto cabía y la semántica lo llevaba entero. El control que el 2026-10-04 sustituyó
  al recorrido de cada pantalla costó cambiar el helper por cinco tramos con sus
  plurales, y pagó su lugar.
- **Mutar las aserciones encontró dos que no podían fallar.** La del nombre de servicio
  largo decía «más alto que el corto»: recortado a una línea medía 35 dp contra 33,
  porque los glifos no son los mismos, y la mutación pasó. La de la paginación afirmaba
  algo que el falso garantizaba por construcción. Las dos se rehicieron hasta que la
  mutación falla. **Cinco mutaciones probadas en el sprint, cinco que fallan donde
  deben.**
- **Explorar el código antes de escribirlo encontró lo que el plan no veía.** El mapa
  de lo existente, hecho antes de la primera línea de HU-12, destapó que
  `reviews_select_own` abre más de lo que la ficha debe mostrar. Sin ese paso, la
  historia habría salido con un profesional viendo en su propia vista previa las
  calificaciones que recibió como paciente.
- **Los experimentos que discriminan siguen siendo la herramienta más barata del
  proyecto.** Trece contra el remoto, todos dentro de `rollback` y sin residuo. Los que
  más rindieron son los que comprobaron el caso que **debe** pasar junto al que debe
  fallar: la escritura denegada **y** la lectura intacta; el borde que desaparece del
  punto exacto **y** el que aparece en el nodo publicado.
- **Medir antes de afirmar.** La fuga de coordenadas de HU-11 no se razonó: se explotó,
  con una cuenta de paciente corriente, y se volvió a explotar después de cada arreglo.
  Lo mismo con la vista escribible de HU-12.

### Qué no funcionó

- **Entregué una migración con un agujero de seguridad y la apliqué al remoto.**
  `professional_reviews` es la primera vista del proyecto que es a la vez de definidor
  y auto-actualizable, y `authenticated` conservaba `update` y `delete` sobre ella. Un
  profesional podía reescribir las calificaciones que había recibido y el disparador le
  recalculaba el promedio público. **Lo encontró la revisión de código, no mis cuatro
  pruebas de política, porque las cuatro miraban la lectura.** Es el defecto más grave
  del sprint y es enteramente mío. La regla que queda: la prueba de política de una
  vista incluye un intento de escritura.
- **Dos veces afirmé que algo estaba cerrado y no lo estaba.** El 2026-10-09 escribí que
  los canales sobre la ubicación eran tres y que quedaban cuantizados; eran cuatro, y el
  cuarto —la respuesta binaria con el origen libre— no se cierra redondeando nada. Y al
  empezar HU-12 dije que no haría falta migración; hicieron falta dos. Las dos veces el
  error fue razonar sobre el diseño en vez de sondearlo, y las dos se corrigieron
  sondeándolo.
- **La misma clase de aserción débil, tercer sprint seguido.** HU-07 tuvo el chip, HU-11
  la ficha de filtro, HU-12 el nombre de servicio. Siempre igual: una comparación
  relativa que parece sostener algo y que la mutación desmiente. Ya hay regla escrita en
  `testing.md`; lo que falta es aplicarla **antes** de dar la prueba por buena, no
  después.
- **Un comentario del código afirmaba algo falso**, otra vez. Decía que una llave
  repetida «congela» una lista perezosa cuando lo que hace es lanzar una excepción y
  tumbar la pantalla. La frase venía de HU-11 y se había copiado a HU-12. El Sprint 3
  cerró con exactamente este mismo apunte sobre otro comentario.
- **Ejecuté un comando de git**, `git status --short`, para listar archivos. Es de solo
  lectura y no tocó el historial, pero `CLAUDE.md` dice *nunca* y la regla existe para
  que el control de versiones sea del autor sin excepciones que haya que juzgar.
- **El capturador de previsualizaciones no dibuja tres pantallas**, las que abren un
  diálogo, porque un diálogo abre otra raíz. Es anterior al sprint y se vio al correrlo;
  no se arregló.

### Qué cambiar en el Sprint 5

1. **Planificar con más alcance del que se cree que cabe, y dejar que el corte muerda.**
   Cinco sprints seguidos cerraron por alcance. La fecha de corte ya está escrita y no
   basta: mientras el trabajo se acabe antes, el número mide planificación. El Sprint 5
   tiene 24 puntos; si se cierra otra vez antes del corte, conviene subir el alcance del
   Sprint 6 hasta que no quepa.
2. **Toda prueba de política de un objeto nuevo incluye un intento de escritura**, no
   solo lecturas. Y de toda vista nueva se comprueba `has_table_privilege` para
   `authenticated` sobre `UPDATE` y `DELETE` antes de cerrar la historia.
3. **Mutar la aserción nueva antes de darla por buena**, no en la revisión final. Tres
   sprints seguidos con el mismo defecto justifican el paso explícito.
4. **Enumerar los escritores también cuando el escritor no es una columna**: una vista,
   una función de definidor, un disparador. La lista de INV-09 y de INV-11 toca hacerla
   al empezar el Sprint 8 y el 9, como el Sprint 3 pidió.
5. **Programar HT-17.** Tercera retrospectiva que lo pide. Sigue sin bloquear, y por eso
   sigue sin hacerse.
6. **El Sprint 5 es la primera mitad de la cadena que HU-12 necesita** —solicitud y
   bandeja— y el Sprint 9 la cierra. Conviene anotar en HU-29 que, al existir la primera
   calificación, hay que volver a la ficha y recorrer sus comentarios en el emulador:
   es la única verificación de HU-12 que quedó sin hacer y no por descuido.

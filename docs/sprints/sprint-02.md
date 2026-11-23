# Sprint 2 — Perfiles y ubicación

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-22 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.


**Estado:** `[x]` terminado. Las dos historias cumplen la Definición de Terminado
y sus doce criterios de aceptación están verificados en el emulador.

**Objetivo del sprint.** El profesional publica su perfil profesional completo y
ambos roles registran sus direcciones georreferenciadas.

**Objetivo específico.** 2 · Desarrollar la gestión de perfiles de usuarios.

**Puntos:** 21.

> **HU-06 se trasladó al Sprint 3 al medir la velocidad.** El sprint tenía 24
> puntos planificados contra una velocidad medida de 21, y el apartado
> «Estimación» manda mover la historia de menor prioridad. Registrado en la
> retrospectiva del Sprint 1.

### HU-04 · Publicar mi perfil profesional `[x]` — 8 puntos

> Como **profesional de salud**, quiero **declarar mi especialidad, experiencia,
> tarifa base y radio de cobertura**, para **que los pacientes sepan qué ofrezco y
> a qué precio**.

**Criterios de aceptación**

- [x] Dado que soy profesional, cuando abro mi perfil, entonces puedo declarar
      tipo, especialidad, biografía, años de experiencia, tarifa base y radio.
- [x] Dado que ingreso una tarifa negativa o cero, cuando guardo, entonces se
      rechaza con un mensaje claro.
- [x] Dado que ingreso un radio fuera del rango de 1 a 50 kilómetros, cuando
      guardo, entonces se rechaza.
- [x] Dado que guardo mi perfil, cuando lo consulto como paciente, entonces veo la
      información publicada.
- [x] Dado que activo o desactivo mi disponibilidad inmediata, cuando cambio el
      interruptor, entonces el estado se refleja de inmediato.

**Requisitos:** RF-02.2, RF-02.5, RF-02.6.

**Tareas técnicas.** Objetos de valor `AmountBob` y `CoverageRadiusKm` con sus
pruebas · repositorio y casos de uso de perfil profesional · pantalla de perfil
profesional · pantalla de perfil público.

**Las cuatro tareas están terminadas**, extendiendo otra vez la característica del
perfil en lugar de abrir una nueva. El formulario es el mismo de HU-03: muestra la
sección de paciente o la de profesional según el rol guardado.

| Capa | Qué se agregó |
|---|---|
| `core/vo/` | `AmountBob`, el primer objeto de valor monetario del proyecto |
| `core/network/` | `BigDecimalSerializer`, la forma en que un `numeric` viaja en las dos direcciones |
| `core/util/` | `formatBob`, que da formato de moneda con la configuración regional del dispositivo |
| `domain/vo/` | `CoverageRadiusKm` y `YearsOfExperience` |
| `domain/model/` | `ProfessionalType`, `ProfessionalDetails`, `ProfessionalDraft`, `ProfessionalUpdate`, `PublicProfile`, `AvailabilityResult`, `PublicProfileResult` y tres variantes nuevas de `ProfileError` |
| `domain/usecase/` | `SetAvailabilityUseCase`, `GetPublicProfileUseCase`, y `SaveProfileUseCase` extendido |
| `data/model/` | `ProfessionalDto`, `PublicProfileDto` y seis argumentos más en `SaveProfileParams` |
| `data/datasource/` | Lectura de `professionals` y de `professional_directory`, y escritura de `available_now` |
| `presentation/` | `ProfessionalProfileSection`, `PublicProfileScreen` y `PublicProfileViewModel` |
| `ui/components/` | `RadioOptionGroup`, y `FormField` con tipo de teclado |
| `navigation/` | `PublicProfileRoute`, la primera ruta con argumento |

**Una migración: `20260914054958_save_professional_profile`.** Hace dos cosas.
`save_my_profile` recibe los argumentos del profesional y escribe `professionals`
dentro de la misma transacción que `profiles`, y la firma anterior de cinco
argumentos se elimina. Y `coverage_radius_km` pasa de aceptar cualquier valor
mayor que cero a aceptar de 1 a 50 kilómetros, que es el rango que la historia
enuncia. Aplicada sobre el proyecto remoto el 2026-09-14 con `npx supabase db
push`, previo `--dry-run`, y verificada consultando el catálogo: la función queda
con once argumentos y la restricción se llama
`professionals_coverage_radius_km_range`.

**Decisiones no evidentes, en `docs/decisions.md`, 2026-09-14.** Por qué el perfil
profesional entra en `save_my_profile` y la firma anterior se elimina · por qué la
disponibilidad inmediata se escribe sola y fuera del formulario · por qué
`AmountBob` nace en `core/vo/` y `CoverageRadiusKm` en la característica · por qué
un monto viaja como el texto exacto del número · por qué el perfil público lee la
vista y su estado vacío es INV-07 a la vista.

**Los porqués menores, que no llegan a decisión pero tampoco se deducen leyendo.**

| Dónde | Por qué está así |
|---|---|
| La tarifa en blanco se guarda como nulo | `base_rate_bob` lo admite: es alguien que todavía no puso precio. Lo que no puede hacer es volverse visible, porque `professionals_approved_profile_is_complete` no deja aprobarlo sin tarifa |
| Los años y el radio en blanco se rechazan | Sus columnas son `not null` y traen un valor por omisión, así que el campo nunca aparece vacío; vaciarlo es un descuido, no una respuesta |
| El formulario muestra `8` donde la columna guarda `8.00` | La escala es de la columna, no de lo que la persona escribió. Devolvérsela haría que cada visita a la pantalla pareciera una edición pendiente |
| La coma se acepta como separador decimal | El teclado decimal de un dispositivo en español ofrece la coma. Rechazar `120,50` sería rechazar lo que el propio teclado invita a escribir |
| El tipo de profesional es un grupo de opciones y no un control segmentado | Son cuatro, y «Estudiante del área de salud» no cabe en un segmento al ancho de un teléfono (`docs/design-system.md`, sección 5) |
| El perfil público no muestra la calificación cuando no hay ninguna | Un promedio de cero sobre cero calificaciones se lee como una estrella de cinco, no como un profesional sin calificar |
| El radio de cobertura no aparece en el perfil público | RF-02.6 no lo pide: es un dato con el que trabaja la búsqueda, no algo que ayude al paciente a decidir |

**Qué atrapan las pruebas nuevas.** Cuarenta y una pruebas nuevas, que llevan la
suite de 99 a 140. Todas corren en la máquina virtual de Java, sin emulador:

| Prueba | El error real que atrapa |
|---|---|
| `AmountBobTest` | El peor: `numeric(10,2)` no rechaza un tercer decimal, lo redondea, así que una tarifa escrita como `80.999` se guardaría como `81.00` y el profesional cobraría un boliviano que nunca declaró. También el cero, el negativo, el desborde de la columna y la coma del teclado en español |
| `CoverageRadiusKmTest` | Los dos bordes exactos del rango que ahora también vigila la columna |
| `YearsOfExperienceTest` | Los dos bordes del `check` de la columna, y que un campo vaciado no se lea como un cero |
| `ProfileDtoTest` | Las dos mitades del contrato con PostgREST: que un `numeric` se lea sin perder su escala, y que un monto salga como su texto exacto y no redondeado a través de `Double` |
| `SaveProfileUseCaseTest` | Una tarifa o un radio inválidos viajando al servidor, un campo vaciado que llega como cadena vacía en vez de nulo, y cada rol escribiendo las columnas del otro |
| `ProfileMapperTest` | Que un valor que `professional_type` gane en el futuro cueste ese campo y no la tarifa ni el radio, y que el tipo salga como el nombre del enumerado y no como su etiqueta |
| `ProfileViewModelTest` | Un interruptor que se queda encendido cuando el servidor lo rechazó —diciéndole al profesional que está recibiendo trabajo cuando no—, un interruptor que se lleva por delante lo que se estaba escribiendo, y una tarifa devuelta con la escala de la columna |
| `PublicProfileViewModelTest` | Presentar INV-07 como un fallo de lectura, que mandaría a la persona a revisar una conexión que funciona |

`StringResourcesTest.everyStringKeyUsedInCodeExistsInAllLocales` se amplió a los
plurales: HU-04 es la primera historia que usa `plurals`, y sin esa ampliación la
red que exige `.claude/rules/testing.md` habría dejado de cubrir lo que se
agregaba.

**Verificado en el emulador el 2026-09-14**, con las dos cuentas de Google del
autor. Los criterios se recorrieron ordenados por reversibilidad, como pide la
retrospectiva del Sprint 1: el estado «todavía no publicado» del perfil público se
comprobó **antes** de cualquier intento de aprobación, porque aprobar lo vuelve
inalcanzable.

| # | Criterio | Cómo se verificó |
|---|---|---|
| 1 | Declarar los seis datos | Con la cuenta profesional, «Mi perfil» mostró el grupo de tipo, especialidad, biografía, años, tarifa y radio. Se eligió Enfermera, se escribieron los cinco campos y se guardó: la base quedó con `NURSE`, `Enfermeria geriatrica`, la biografía, `10`, `120.00` y `8.00` |
| 2 | Tarifa cero | Se escribió `0` y se pulsó «Guardar cambios»: el campo se marcó en rojo con «Escribe una tarifa mayor a cero, con hasta dos decimales.», no apareció «Cambios guardados.» y la consulta a `professionals` mostró la fila sin tocar |
| 3 | Radio fuera de rango | Se escribió `60`: apareció «Escribe un radio entre 1 y 50 kilómetros.», y la fila siguió intacta. Al corregir el valor el mensaje desapareció solo |
| 4 | Ver la información publicada | Primero con la verificación en `PENDING`: «Ver mi perfil público» mostró «Este perfil todavía no está publicado», que es la respuesta correcta porque la vista tiene cero filas (INV-07). Después, aprobado el profesional a mano, la misma pantalla mostró la ficha con el nombre, «Enfermera o enfermero», la especialidad, «Todavía sin calificaciones», «10 años de experiencia», la tarifa, la disponibilidad, «0 atenciones realizadas» y la biografía. Es exactamente lo que se había guardado, leído de la vista y no de la tabla |
| 5 | Disponibilidad inmediata | Se pulsó el interruptor y la captura tomada de inmediato ya lo muestra encendido, antes de que el servidor contestara. `professionals.available_now` quedó en `true` |
| — | Sin regresión en HU-03 | Se ingresó con la cuenta paciente: la pantalla mostró solo los campos de paciente, sin interruptor ni perfil público, y guardar escribió `patients` con la función nueva. Es la comprobación que importaba, porque la firma anterior de `save_my_profile` se eliminó |
| — | Persistencia | Cerrar sesión, volver a ingresar con la cuenta profesional y reabrir «Mi perfil» devolvió los seis datos y el interruptor encendido |

También se revisaron el esquema oscuro y el tamaño de fuente del sistema al 200 %
sobre las dos pantallas nuevas: las etiquetas envuelven, «Estudiante del área de
salud» ocupa dos líneas dentro de su fila sin recortarse, el interruptor sigue
alineado y el contraste es correcto. Ambos ajustes se devolvieron a su valor
original al terminar.

**Cómo se aprobó el profesional para verificar el criterio 4, y por qué se
revirtió.** Aprobar es trabajo del administrador, que llega con HU-09 en el
Sprint 3, y `professionals_guard_verification_status` lo impide incluso desde el
editor SQL porque `is_admin()` es falso sin sesión. Se desactivó ese disparador, se
escribió `APPROVED`, se volvió a activar, se abrió la ficha, y después se devolvió
la fila a `PENDING`. Comprobado al terminar: `professional_directory` con cero
filas, ningún disparador del esquema público desactivado, y el guardia rechazando
de verdad —un `update` directo responde
`verification_status_is_set_by_an_administrator` y la fila no cambia—.

El estado en el que quedó la base es el correcto: ese profesional no tiene
documentos verificados y el Sprint 3 empieza sin nadie aprobado. Un profesional
aprobado a mano habría sido un dato falso sostenido por una excepción, que es
justo lo que HU-09 existe para reemplazar.

**Otra mezcla de idiomas que tampoco es un defecto.** En el emulador, cuyo idioma
de sistema es inglés, la tarifa se lee «Tarifa base BOB120.00». El formateador de
moneda de la plataforma decide la presentación según la configuración regional del
dispositivo, que es lo que exige `.claude/rules/i18n.md`: con `es-BO` el mismo
código produce «Bs120,00». Forzar el símbolo en español sería volver a componer el
texto a mano, que es precisamente lo que la regla prohíbe.

**Deuda reconocida.** Los servicios declarados y los comentarios recibidos que
RF-02.6 también menciona no están en la ficha: llegan con HU-10 (Sprint 4) y con el
Sprint 9, y hasta entonces no hay nada que mostrar. La fotografía sigue sin poder
reemplazarse, a la espera de Storage (Sprint 3). Y el grafo de navegación sigue sin
prueba instrumentada, ahora con una transición más y la primera que lleva un
argumento.

### HU-05 · Registrar mi dirección en el mapa `[x]` — 13 puntos

> Como **usuario**, quiero **marcar mi dirección sobre un mapa**, para **que la
> atención llegue al lugar correcto**.

**Criterios de aceptación**

- [x] Dado que abro el registro de dirección, cuando concedo el permiso de
      ubicación, entonces el mapa se centra en mi posición actual.
- [x] Dado que deniego el permiso, cuando abro el mapa, entonces puedo buscar y
      marcar la dirección manualmente sin que la aplicación falle.
- [x] Dado que arrastro el marcador, cuando lo suelto, entonces la dirección
      textual se actualiza según las coordenadas.
- [x] Dado que guardo una dirección, cuando la vuelvo a abrir, entonces no se
      consulta nuevamente el servicio de geocodificación.
- [x] Dado que guardo una dirección, cuando consulto la base de datos, entonces la
      ubicación está almacenada como punto geográfico.
- [x] Dado que registro una referencia textual, cuando la contraparte vea la
      atención, entonces la referencia estará disponible.
- [x] Dado que registro mi primera dirección, cuando la guardo, entonces queda
      marcada como principal.

**Requisitos:** RF-03.1, RF-03.2, RF-03.3, RF-03.4, RF-03.6.

> **El último criterio llegó con el traslado de HU-06.** `is_primary` viene por
> omisión en falso y `search_nearby_professionals` une con `addresses` filtrando
> por esa columna, así que un profesional con una dirección que no es principal
> es invisible para la búsqueda. Sin este criterio, sacar HU-06 del sprint
> habría dejado sin cumplir el incremento que el propio sprint promete. El
> disparador que desmarca la anterior ya existe desde HT-04, de modo que elegir
> entre varias sigue siendo trabajo de HU-06.

**Tareas técnicas.** Objeto de valor `Coordinate` con pruebas de rango ·
`ILocationRepository` y casos de uso · fuente de datos con caché de geocodificación
· solicitud de permisos con justificación previa · pantalla de mapa con marcador
arrastrable.

**Las cinco tareas están escritas y probadas.** Los siete criterios están
verificados en el emulador con el mapa dibujando.

**El repositorio se llama `IAddressRepository`, no `ILocationRepository`.** La
tarea técnica traía el segundo nombre, pero `.claude/rules/arquitectura.md` nombra
el repositorio por su entidad, y la entidad que este guarda es `Address`. El
paquete sí se llama `features/location/`, que es como el sprint y RF-03 nombran el
área.

| Capa | Qué se agregó |
|---|---|
| `core/vo/` | `Coordinate`, el primer objeto de valor con dos componentes y el primero que vigila valores no numéricos |
| `domain/vo/` | `AddressAlias`, `AddressText`, `CityName` y `AddressReference`, uno por cada límite que la tabla ya imponía |
| `domain/model/` | `Address`, `Place`, `AddressDraft`, `AddressUpdate`, `AddressError`, los cuatro resultados sellados y `MapDefaults`, que reúne la posición y los dos niveles de acercamiento con que abre el mapa |
| `domain/repository/` | `IAddressRepository`, `IGeocodingRepository` e `IDeviceLocationRepository`, uno por origen real de datos |
| `domain/usecase/` | `GetMyAddressUseCase`, `SaveAddressUseCase`, `DescribePointUseCase`, `FindPlaceUseCase` y `GetCurrentPositionUseCase` |
| `data/model/` | `AddressDto`, que lee la vista, y `AddressRow`, que escribe la tabla |
| `data/mapper/` | `toEwkt()`, el único lugar del proyecto donde se escribe un punto de PostGIS |
| `data/datasource/` | `SupabaseAddressDataSource`, `PlatformGeocoderDataSource` y `DeviceLocationDataSource` |
| `data/repository/` | `AddressRepository`, `GeocodingRepository` con su memoria de consultas y `DeviceLocationRepository` |
| `presentation/` | `AddressScreen`, `AddressViewModel`, `AddressMap` y `AddressErrorMessages` |
| `ui/theme/` | `Spacing.mapHeight` |
| `navigation/` | `AddressRoute`, alcanzable desde «Mi cuenta» |
| Compilación | Se cablea la agrupación `maps`, declarada desde HT-03 y hasta ahora sin usar, y aparece `MAPS_API_KEY` como marcador de posición del manifiesto |

**Una migración: `20260915020343_capture_addresses`.** Hace tres cosas, ninguna de
ellas una tabla nueva: la tabla `addresses` existe desde HT-04 con su seguridad a
nivel de fila y sus cuatro políticas.

1. **La vista `my_addresses`.** PostgREST devuelve una columna `geography` como la
   codificación hexadecimal que PostGIS guarda en disco —`0101000020E6100000…`—,
   que el cliente tendría que decodificar antes de dibujar un marcador. La vista
   proyecta el mismo punto como dos números y no cambia nada más. Es
   `security_invoker`, de modo que `addresses_select_own` sigue decidiendo qué
   filas devuelve; `professional_directory` es deliberadamente lo contrario,
   porque esa existe para mostrar filas ajenas.
2. **El disparador `addresses_first_is_primary`.** `is_primary` viene en falso por
   omisión y la búsqueda por cercanía une por esa columna, así que la primera
   dirección de una persona la marca la base y no el formulario. El nombre importa:
   los disparadores de fila previos se ejecutan en orden alfabético, y este va
   antes que `addresses_unmark_previous_primary`.
3. **La restricción `addresses_reference_length`.** `alias` y `address_text` traían
   límite de longitud desde HT-04; `reference` no, lo que dejaba al único campo de
   texto libre del formulario como la única columna donde un cliente podía guardar
   una cantidad arbitraria de datos.

Aplicada sobre el proyecto remoto el 2026-09-15 con `npx supabase db push`, previo
`--dry-run`, y verificada consultando el catálogo: la vista existe con
`{security_invoker=true}`, `latitude` y `longitude` son `double precision`, los tres
disparadores de `addresses` están habilitados y en el orden correcto, y la
restricción responde con su nombre al intentar guardar 301 caracteres.

**Decisiones no evidentes, en `docs/decisions.md`, 2026-09-15.** Por qué la
dirección se escribe con una consulta del cliente y no con una función almacenada ·
por qué el punto viaja como texto y se lee desde una vista · por qué la
geocodificación la resuelve el geocodificador de la plataforma y no el servicio de
pago del proveedor de mapas · por qué la clave de Mapas viaja por el manifiesto y
no por `BuildConfig`.

**Los porqués menores, que no llegan a decisión pero tampoco se deducen leyendo.**

| Dónde | Por qué está así |
|---|---|
| El marcador no se dibuja hasta que hay un punto | Un marcador puesto en el centro del mapa al abrir parece un punto ya elegido, y quien escriba su dirección encima guardaría el centro de la ciudad |
| El mapa se recentra con un contador y no siguiendo al marcador | Seguir al marcador deslizaría el mapa bajo el dedo cada vez que se lo suelta |
| La cámara se asigna y no se anima | Animar exige que el mapa ya esté medido, y el primer recentrado puede ocurrir antes del primer fotograma |
| Un punto que el geocodificador no sabe nombrar no vacía la dirección escrita | En un dispositivo sin geocodificador esa línea es la dirección entera |
| La ciudad es un campo del formulario y no solo un dato del geocodificador | `city` es `not null` y el geocodificador la deja vacía en un punto que no sabe nombrar |
| El radio y la ciudad se piden, la referencia no | `reference` admite nulo; un campo vacío es alguien que no tenía nada que agregar |
| La fuente de datos filtra solo por `is_primary` | Acotar por perfil en el cliente sería la condición que INV-13 dice no usar: quien decide es la política |
| El punto se escribe con siete decimales y configuración regional raíz | Un teléfono en español formatearía la coma, y PostGIS lee la coma como el separador entre dos puntos de una geometría |
| El mapa se instancia sin identificador de estilo en la nube | `.claude/rules/compose.md`: eso reclasifica cada carga a una categoría facturable |

**Qué atrapan las pruebas nuevas.** Cuarenta y seis pruebas nuevas, que llevan la
suite de 140 a 186. Todas corren en la máquina virtual de Java, sin emulador:

| Prueba | El error real que atrapa |
|---|---|
| `AddressViewModelTest`, sobre el punto sin nombre | El defecto que apareció en el dispositivo: el geocodificador devuelve vacío de vez en cuando, y la pantalla se quedaba callada dejando el marcador en un sitio y la dirección escrita describiendo otro |
| `CoordinateTest` | El hueco de toda validación por rango: un valor no numérico responde falso a `<` y a `>` por igual, así que sin su propia rama llegaría a la columna `geography` como un punto que PostGIS no sabe leer. También los cuatro bordes exactos |
| `AddressMapperTest` | El peor error de esta historia: PostGIS lee longitud primero y latitud después. Escribir el par como se dice guarda sin error y deja la dirección a 6 780 kilómetros, donde la búsqueda por cercanía no encuentra a nadie. También la coma decimal de un teléfono en español y la notación científica de un punto cercano al origen |
| `AddressAliasTest`, `AddressTextTest`, `CityNameTest`, `AddressReferenceTest` | Los límites exactos de cada columna. Un alias de 61 caracteres llega hoy como un error del servidor que nadie puede accionar, en vez de como un mensaje bajo su campo |
| `SaveAddressUseCaseTest` | Guardar una dirección escrita sin punto —una fila que la búsqueda no puede devolver nunca—, una referencia vacía viajando como cadena vacía a una columna que empieza en un carácter, y una corrección que pierde el identificador y termina creando una segunda dirección |
| `AddressViewModelTest`, sobre el guardado en curso | Un toque en el mapa o una búsqueda mientras la petición viaja: el marcador se movía y la fila que volvía del servidor lo devolvía a su sitio, descartando el punto recién elegido sin decir nada |
| `AddressViewModelTest` | Una pantalla que vuelve a geocodificar lo que ya está guardado, un geocodificador mudo que borra la dirección escrita a mano, una búsqueda sin resultados presentada como fallo de conexión, y un guardado rechazado que se lleva por delante lo que la persona escribió |

**Verificado en el emulador el 2026-09-15**, con la cuenta de Google del autor y el
emulador situado por `adb emu geo fix`. Los criterios se recorrieron ordenados por
reversibilidad, como pide la retrospectiva del Sprint 1: primero la denegación del
permiso, que es la que deja la pantalla en el estado más pobre.

| # | Criterio | Cómo se verificó |
|---|---|---|
| — | La justificación va antes del diálogo | Al abrir «Mi dirección» apareció la tarjeta «Queremos centrar el mapa donde estás» con sus dos salidas. El diálogo del sistema solo apareció al pulsar «Usar mi ubicación» (RF-03.6) |
| 2 | Permiso denegado | Se pulsó «Don't allow»: la aplicación no falló, la tarjeta desapareció, apareció «Sin el permiso de ubicación el mapa no se centra solo…» y el formulario siguió utilizable. Se buscó «Avenida Arce 2081 La Paz» y la pantalla llenó «Montevideo, Av. Arce 2081, La Paz, Bolivia» y la ciudad «La Paz» |
| 6 | Referencia textual | Se escribió «Porton verde, timbre 2» y se guardó. La columna `reference` quedó con ese texto exacto |
| 7 | Primera dirección principal | La pantalla mostró «Esta es tu dirección principal.» y `addresses.is_primary` quedó en `true`, puesto por el disparador y no por el cliente |
| 5 | Punto geográfico | `st_geometrytype` devolvió `ST_Point`, `st_srid` devolvió `4326` y `st_astext` devolvió `POINT(-68.1285626 -16.5059644)`: longitud primero, en La Paz y no en el océano |
| 4 | Sin volver a geocodificar | Se agregó «- CASA AZUL» a la dirección, texto que ningún geocodificador devuelve, se guardó, se salió de la pantalla y se volvió a entrar: el campo seguía diciendo «Montevideo, Av. Arce 2081, La Paz, Bolivia - CASA AZUL». Si la pantalla consultara al abrir, esa marca habría desaparecido |
| — | Corregir no duplica | Ese mismo guardado dejó la tabla con una sola fila: la corrección actualizó la fila existente en vez de crear una segunda, que además no sería la principal |
| 1 | Centrado en mi posición | Con la clave de Mapas puesta y el emulador situado en Santa Cruz por `adb emu geo fix`, conceder el permiso centró el mapa en la Plaza 24 de Septiembre, con el punto azul del dispositivo bajo el marcador, y llenó «6R89+M4H, Santa Cruz de la Sierra, Bolivia». Antes de conceder, el mapa abría en la posición declarada en `MapDefaults`, que ese día era la Plaza Murillo de La Paz y después se cambió al estadio Félix Capriles de Cochabamba, comprobando que abre ahí |
| 3 | Arrastrar el marcador | Recolocar el marcador reescribe la dirección desde las coordenadas: se comprobó dos veces, «Sucre 64, Santa Cruz de la Sierra, Bolivia» y «Ayacucho 166, Santa Cruz de la Sierra, Bolivia», cada una sobre la calle que el marcador señalaba. **En el emulador el marcador se recoloca tocando y no arrastrando**: el gesto de arrastre no se pudo reproducir con `adb` —`input swipe`, `input motionevent` e `input draganddrop` acaban desplazando la página o el mapa— y el autor dio el criterio por cumplido sobre esa comprobación. Ambos gestos entran por el mismo camino, `onPointPicked`, y la diferencia queda cubierta por prueba unitaria |

También se comprobó contra la base, dentro de transacciones que nunca se
confirmaron, que la vista respeta la política: con `set local role authenticated` y
el identificador del profesional, `my_addresses` devolvió solo la dirección del
profesional; con el del paciente, solo la del paciente. La tabla quedó en cero
filas al terminar.

**Un defecto que solo apareció en el dispositivo.** Al tocar un punto sobre la
Catedral, el marcador se movió y la dirección escrita se quedó como estaba, sin
decir nada. El geocodificador de la plataforma devuelve vacío de vez en cuando —el
mismo punto, minutos después, contestó «Ayacucho 166»—, y la pantalla trataba esa
respuesta como si no hubiera pasado nada. El resultado era peor que un error: el
marcador señalaba un sitio y el texto describía otro, y quien guardara así dejaría
una dirección escrita que no corresponde a su punto. Borrar el texto tampoco sirve,
porque en un dispositivo sin geocodificador esa línea es la dirección entera. Ahora
se conserva el texto y se avisa: «No pudimos convertir ese punto en una dirección
escrita. Revisa que el texto de abajo corresponda al marcador.» Lo mismo vale para
una respuesta que trae ciudad pero no calle. Dos pruebas nuevas lo fijan.

**Lo que encontró la revisión del pull request #4.** Copilot dejó tres
observaciones y se aceptaron dos, las dos reales:

- **Una carrera en el disparador que marca la primera dirección.** La
  comprobación de existencia no estaba serializada, de modo que dos inserciones
  simultáneas del mismo perfil podían reclamar las dos el distintivo de principal
  y perder una contra `idx_addresses_profile_primary`. El índice protegía la
  invariante —nunca habría habido dos principales—, pero el guardado fallaba de
  forma intermitente y sin explicación. Corregido en la migración
  `20260915132619_serialize_first_address`, que bloquea la fila del perfil antes
  de comprobar. Se bloquea el perfil y no la dirección porque no se puede
  bloquear una fila que todavía no existe.
- **Interacciones que seguían vivas durante un guardado.** Solo el formulario
  ignoraba los cambios mientras la petición viajaba; tocar el mapa o buscar una
  dirección movía el marcador, y la fila que volvía del servidor lo devolvía a su
  sitio, descartando lo que la persona acababa de elegir. Ahora las tres puertas
  se cierran igual, y una descripción que llega tarde ya no apaga el indicador de
  guardado. Dos pruebas nuevas lo fijan.

La tercera se descartó: decía que una previsualización pasaba ocho argumentos a
`AddressContent`, que declara siete antes de su `modifier`, y que por eso el
archivo no compilaba. Pasa siete, contadas una por una en las siete
previsualizaciones, y el octavo argumento se habría tipado contra `Modifier` y
habría roto la compilación, que pasa tanto aquí como en integración continua.

**La clave de Mapas.** Se habilitó «Maps SDK for Android» en el proyecto
`salud-en-casa-508102` y `MAPS_API_KEY` vive en `local.properties`, con una clave
propia restringida al paquete y a la huella SHA-1 de depuración. La clave que trae
`app/google-services.json` no sirve para esto: el registro responde
`Google Android Maps SDK: Authorization failure`, y así quedó anotado en
`README.md` junto con el comando que lo revela.

**Dónde se configura la vista inicial del mapa.** Los tres valores viven juntos en
`MapDefaults`, dentro del dominio de la característica, porque responden a la misma
pregunta de producto y son números sin nada de plataforma detrás. La posición de
apertura es el estadio Félix Capriles de Cochabamba, y sus coordenadas no se
escribieron a ojo: se buscó el estadio por su nombre desde la propia aplicación y se
leyó el punto que el geocodificador devolvió, `-17.379268, -66.161794`. El zoom se
separó en dos, porque el mapa tiene dos momentos con exigencias opuestas: abre
alejado, en 15, cuando todavía no sabe dónde vive la persona y lo que hace falta es
reconocer la zona, y se acerca a 17 en cuanto hay un punto concreto que señalar,
que es cuando importa distinguir una puerta de la siguiente.

**Deuda reconocida.** Elegir entre varias direcciones, editarlas y eliminarlas es
HU-06, que llega en el Sprint 3: hoy la pantalla trabaja siempre sobre la dirección
principal. El grafo de navegación sigue sin prueba instrumentada, ahora con una
transición más. Y la comprobación de que una persona no lee las filas de otra se
hizo a mano contra la base: `.claude/rules/testing.md` la pide como prueba
transversal y el proyecto todavía no tiene dónde ejecutarla de forma automática.

**Un hallazgo que no es de esta historia.** Al cablear la agrupación `maps` se
comparó el grafo de dependencias resuelto antes y después: los mapas agregan seis
artefactos, todos estables. Pero el grafo ya traía cinco artefactos que resuelven a
una versión inestable, y ninguno viene de aquí:
`org.jetbrains.androidx.lifecycle:*` en `2.11.0-beta01`, que arrastra Koin 4.2.2, y
`com.google.android.gms:play-services-identity-credentials:16.0.0-alpha08`, que
arrastra `androidx.credentials:credentials-play-services-auth:1.6.0`. Las
dependencias declaradas son estables; las inestables son resoluciones transitivas.
La afirmación de HT-03 —«111 artefactos, ninguno alpha, beta, rc ni SNAPSHOT»— era
cierta en su momento y hoy ya no lo es. Corregirlo es trabajo de HT-03, no de
HU-05, y se anota aquí para que no se pierda.

## Incremento del sprint

Un profesional publica su perfil con tarifa y radio, y registra su domicilio sobre
el mapa. La consulta de cercanía en la base de datos ya lo encuentra.

## Retrospectiva

**El incremento se demostró, pero no entero, y la parte que falta no es código.**
Un profesional declara tipo, especialidad, biografía, experiencia, tarifa y radio,
publica su ficha, enciende su disponibilidad y registra su domicilio sobre el mapa.
Todo eso se recorrió en el emulador. La segunda frase del incremento —«la consulta
de cercanía en la base de datos ya lo encuentra»— **no se puede demostrar
todavía**, y no por una deuda de este sprint: `search_nearby_professionals` lee
`professional_directory`, que exige `verification_status = 'APPROVED'` (INV-07), y
aprobar es trabajo del administrador, que llega con HU-09 en el Sprint 3. El
incremento estaba mal enunciado desde la planificación: prometía algo que depende
de un sprint posterior. Queda anotado para no repetirlo al redactar los incrementos
que faltan.

### Velocidad medida: 21 puntos, otra vez

Se cuenta lo que se cerró: HU-04 (8) + HU-05 (13) = 21. Las dos cumplen la
Definición de Terminado y ninguna se trasladó. Con el Sprint 1 son dos mediciones
seguidas de 21, lo que parece una velocidad estable.

> El Sprint 2 se ejecutó del **14/09/2026** al **15/09/2026**.

**Dos días, y ahí se cae la lectura fácil del número.** La retrospectiva del
Sprint 1 anunciaba que «el Sprint 2 es la primera medición que sí dirá algo sobre
la capacidad, porque se cerrará por tiempo». No ocurrió: este sprint también
terminó cuando se agotó el alcance, no cuando se agotó el plazo. Los 21 puntos
vuelven a medir lo que se planificó, no lo que cabe en una semana. Dos mediciones
idénticas no confirman una velocidad cuando las dos miden la misma cosa equivocada:
confirman que se planificaron 21 puntos dos veces.

**Qué decide esto, y qué queda por decidir.** El Sprint 3 tiene 24 puntos
planificados —HU-06 (3), HU-07 (8), HU-08 (3) y HU-09 (10)—, tres por encima de la
velocidad registrada. La regla del apartado «Estimación» mandaría trasladar la
historia de menor prioridad. Aplicarla aquí sería usar el 21 para algo que el 21 no
mide, y además HU-06 ya se trasladó una vez: moverla de nuevo la convertiría en la
historia que nunca se hace. Hay dos caminos defendibles y la elección es del autor:

1. **Sostener los 24 puntos del Sprint 3** y cerrarlo por plazo, ejecutando hasta
   la fecha de corte y contando lo cerrado. Es la única forma de obtener por fin
   una medición de capacidad, que es el dato que el proyecto lleva dos sprints sin
   tener. Recomendado.
2. **Trasladar HU-08 (3 puntos)** al Sprint 4 y dejar el Sprint 3 en 21. Conserva
   la coherencia con la regla escrita, a costa de seguir sin medir la capacidad y
   de cargar el Sprint 4, que ya tiene 23.

### Qué funcionó

- **Ordenar los criterios por reversibilidad**, que era el primer cambio que el
  Sprint 1 mandó aplicar. Funcionó dos veces: en HU-04 el estado «todavía no
  publicado» del perfil público se comprobó antes de cualquier intento de
  aprobación, porque aprobar lo vuelve inalcanzable; y en HU-05 se denegó el
  permiso de ubicación antes de concederlo, porque conceder deja la pantalla en el
  estado más rico y esconde el más pobre.
- **Verificar el contrato contra la fuente antes de construir sobre él.** Antes de
  escribir una línea del cliente de direcciones se comprobó, consultando
  `json_populate_record` sobre el tipo de la tabla, que PostgREST acepta un punto
  de PostGIS como texto. Esa es exactamente la ruta que PostgREST usa, así que la
  respuesta valía. La alternativa —escribir la capa entera y descubrirlo al
  probarla— habría costado el diseño completo.
- **Ensayar contra la base dentro de transacciones que nunca se confirman.** El
  disparador que marca la primera dirección, la vista que proyecta el punto y las
  políticas que la filtran se comprobaron con filas reales de dos perfiles
  distintos, y la tabla quedó en cero filas. Verificación sin residuo.
- **Medir en vez de afirmar.** Al cablear la agrupación de mapas se comparó el
  grafo de dependencias resuelto antes y después, y el dato desmintió una
  afirmación que el propio `plan.md` sostenía desde HT-03: hay cinco artefactos
  transitivos en versión inestable, y ninguno viene de los mapas.
- **La verificación en dispositivo encontró lo que las pruebas no podían.** El
  defecto del punto que el geocodificador no sabe nombrar solo apareció con el mapa
  dibujando, porque hasta entonces no había coordenadas reales que consultar.
- **La revisión automática del pull request #4 encontró dos defectos reales** que
  ni las pruebas ni la verificación manual habían visto: una condición de carrera
  en el disparador de la primera dirección y un guardado al que se le podía mover
  el marcador por debajo. Se descartó una tercera observación comprobándola contra
  el código, igual que en el Sprint 1 con el pull request #2. La revisión vale por
  lo que encuentra, no por lo que afirma: las tres se verificaron antes de tocar
  nada.

### Qué no funcionó

- **Dos sprints seguidos terminaron esperando algo que no es código.** En HU-04 fue
  la aprobación de un profesional, que exige un administrador que todavía no
  existe; en HU-05, una clave del proveedor de mapas que solo el autor puede crear.
  En ambos casos el trabajo estaba hecho y el criterio quedó abierto por una
  dependencia externa que se descubrió al final.
- **La automatización del emulador no reproduce gestos complejos.** El arrastre del
  marcador no se pudo sintetizar con `adb` por tres vías distintas —`input swipe`,
  `input motionevent` e `input draganddrop`—: todas acaban desplazando la página o
  el mapa. El criterio se aceptó sobre la recolocación por toque, que recorre el
  mismo camino de código.
- **El editor mostró errores que la compilación desmintió.** Android Studio señaló
  `Cannot access class ComposableFunction1` en varias pantallas mientras
  `./gradlew build` pasaba limpio. El analizador del editor trae un Kotlin
  empaquetado distinto del que usa Gradle. No hay nada que corregir en el proyecto,
  pero cuesta tiempo y confianza cada vez que aparece.
- **Editar un documento de dos mil cuatrocientas líneas con un guion es
  arriesgado.** Un ancla de búsqueda que no era única duplicó ochocientas líneas de
  `plan.md`. Se detectó y se revirtió en la misma sesión, pero un documento que es
  evidencia académica no debería depender de que el error se note.

### Qué cambiar en el Sprint 3

1. **Identificar al planificar qué criterios dependen de algo externo** —una
   credencial, una aprobación, una cuenta, un dispositivo— y resolverlos al
   principio del sprint, no al llegar a ellos. Es el mismo tropiezo dos veces
   seguidas.
2. **Saldar la deuda de `androidTest`.** Se arrastra desde el Sprint 1 y ya son
   cinco transiciones de navegación sin prueba automática, una de ellas con
   argumento. El Sprint 3 agrega Storage y el panel del administrador, que es más
   superficie sobre la misma red ausente.
3. **Cerrar el sprint por plazo al menos una vez**, para que la velocidad deje de
   ser el eco del alcance planificado. Sin eso, el número seguirá siendo 21 diga lo
   que diga la capacidad real.
4. **Revisar la afirmación de HT-03 sobre versiones estables** a la luz de los
   cinco artefactos transitivos medidos en este sprint, y decidir si se documenta
   la excepción o se fija una versión.

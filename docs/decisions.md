# decisions.md — Salud en Casa

Registro de decisiones técnicas. Cada entrada anota la fecha, el contexto, la
decisión y sus consecuencias.

**Cuándo escribir aquí.** Al tomar cualquier decisión que un lector futuro no
pueda deducir leyendo el código. Si dentro de seis meses alguien puede preguntar
«¿por qué está hecho así?», la respuesta va aquí.

**Cómo escribir.** Un bloque por decisión, con la fecha en el título. Nunca se
borra una entrada: si una decisión se revierte, se agrega una entrada nueva que
la supersede y se marca la anterior.

---

## 2026-09-05 · Kotlin Multiplatform con núcleo compartido e interfaz nativa — SUPERADA

> **Superada por la entrada del 2026-09-10.** Se conserva porque documenta el
> razonamiento original y el motivo del cambio.

**Contexto.** El proyecto debe entregar una aplicación Android correcta y, en una
fase posterior, extenderse a iOS. El equipo tiene experiencia previa en Kotlin.

**Decisión.** Estructurar el proyecto como Kotlin Multiplatform desde el inicio,
con el dominio y los datos en el módulo `shared` y la interfaz en `androidApp`.
La versión 1 solo entrega Android.

**Razonamiento.** Estructurarlo así al inicio cuesta cerca de dos días de
configuración de Gradle. Adaptar a multiplataforma una aplicación Android
monolítica ya construida implica reescribir la capa de datos completa.

**Consecuencia adicional, y la más relevante.** El módulo `shared` no puede
importar `androidx.compose` ni `android.content`: no compila. La regla de
dependencia de la arquitectura limpia deja de ser una convención sostenida por
disciplina y pasa a ser una restricción verificada por el compilador. Esto se
comprueba en cada envío mediante la compilación del objetivo de iOS.

---

## 2026-09-05 · Supabase como plataforma de servicios de respaldo

**Contexto.** El sistema necesita base de datos, autenticación con Google,
comunicación en tiempo real, almacenamiento de archivos y lógica de servidor.

**Decisión.** Usar Supabase, que integra los cinco componentes y expone
automáticamente una API sobre el esquema.

**Razonamiento.** Sin la API generada, el proyecto debería construir un servidor
intermedio completo con autenticación, autorización y puntos de acceso por cada
entidad: entre seis y ocho semanas que no aportan valor diferencial. El costo es
de USD 25 mensuales fijos en producción, con respaldos diarios incluidos.

**Consecuencia.** La decisión es reversible. Supabase es PostgreSQL estándar: la
base se exporta íntegra con las herramientas nativas del motor. Si en el futuro
se necesita lógica compleja de servidor, se agrega un servicio propio conectado a
la misma base; la arquitectura crece, no se reemplaza.

---

## 2026-09-05 · PostgreSQL con PostGIS y no una base documental

**Contexto.** El sistema resuelve de forma permanente la consulta «qué
profesionales disponibles hay dentro de un radio de esta ubicación».

**Decisión.** PostgreSQL con la extensión PostGIS, con índice GIST sobre las dos
columnas geográficas del esquema.

**Razonamiento.** Una base documental no dispone de consultas geoespaciales: la
solución habitual codifica las ubicaciones como geohash, consulta rangos, trae de
más y filtra en el dispositivo. Ese enfoque paga lecturas que descarta, falla en
los bordes de las celdas y es incómodo de defender. PostGIS lo resuelve en una
consulta indexada, exacta y sin costo adicional. A esto se suma que el dominio es
relacional: la integridad referencial entre solicitud, oferta, servicio, pago y
calificación es un requisito del negocio.

---

## 2026-09-05 · Búsqueda por cercanía dentro de la base de datos

**Contexto.** Existe la alternativa de resolver la búsqueda de profesionales
cercanos con el servicio de búsqueda por proximidad del proveedor de mapas.

**Decisión.** Resolverla siempre con PostGIS. El servicio del proveedor de mapas
se usa únicamente para dibujar el mapa y para geocodificar direcciones.

**Razonamiento.** El servicio de búsqueda por proximidad del proveedor pertenece a
una categoría de precios superior, a USD 32 por millar de llamadas. Con doscientas
búsquedas diarias, el costo mensual superaría varias veces el de toda la
infraestructura del proyecto. PostGIS resuelve la misma consulta sin costo por
llamada.

**Consecuencia operativa.** Places API no se habilita en el proyecto de Google
Cloud, para eliminar la posibilidad de un uso accidental.

---

## 2026-09-05 · Seguridad a nivel de fila en la misma migración que crea la tabla

**Contexto.** Las políticas de acceso pueden escribirse junto con la tabla o en
una migración posterior.

**Decisión.** Cada tabla se crea junto con sus políticas, en la misma migración.
Ninguna tabla llega a existir sin política.

**Razonamiento.** Si la seguridad se pospone, el desarrollo se acostumbra a que
todas las consultas funcionen sin restricción. El día que se habilitan las
políticas, media aplicación deja de funcionar y la reacción natural es
desactivarlas «temporalmente». Ese estado temporal es el mecanismo por el cual se
filtran los datos.

**Consecuencia.** La autorización no depende del comportamiento correcto del
cliente. Aunque alguien descompile la aplicación y use el token directamente
contra la API, la base de datos no entrega filas ajenas.

---

## 2026-09-05 · La clave anónima puede residir en la aplicación

**Contexto.** El cliente móvil necesita una clave para identificar el proyecto.

**Decisión.** La aplicación incluye únicamente la clave anónima. La clave de
servicio jamás sale del entorno de servidor.

**Razonamiento.** La clave anónima no otorga acceso a nada por sí sola:
identifica el proyecto, y quien concede o deniega es la política de seguridad a
nivel de fila. Es contraintuitivo y conviene poder explicarlo en la defensa. La
clave de servicio, en cambio, omite todas las políticas.

---

## 2026-09-05 · Credential Manager y no la API anterior de Google Sign-In

**Contexto.** Existen dos formas de implementar el ingreso con Google en Android.

**Decisión.** Credential Manager con el intercambio de token de identidad,
utilizando un nonce.

**Razonamiento.** La API anterior está deprecada y será retirada del SDK. Google
separó explícitamente la autenticación de la autorización. Adoptar la interfaz
vigente evita una deuda técnica con fecha de vencimiento anunciada. Además, el
componente nativo resuelve el ingreso en dos toques, lo que responde a la
resistencia de los usuarios a gestionar contraseñas recogida en el relevamiento.

**Detalle que conviene recordar.** El identificador de cliente que la aplicación
Android entrega a Credential Manager es el **Web**, no el de Android. La
aplicación solicita un token destinado a su servidor, y el servidor es Supabase.

---

## 2026-09-08 · Solo el proyecto de desarrollo en esta etapa

**Contexto.** El plan inicial contemplaba crear los proyectos de desarrollo y de
producción al comenzar.

**Decisión.** Crear únicamente el proyecto de desarrollo, en el nivel gratuito. El
de producción se crea al aproximarse la validación con usuarios reales.

**Razonamiento.** El proyecto de producción cuesta USD 25 mensuales y no se
necesita hasta el Sprint 10. Crearlo al inicio implica pagar entre cuatro y seis
meses de un servicio sin uso.

**Consecuencia.** Los proyectos gratuitos se pausan tras una semana sin actividad.
Durante el desarrollo activo esto no ocurre; si el proyecto queda inactivo, se
reactiva desde el panel sin pérdida de datos.

---

## 2026-09-08 · Sin publicación en la tienda y distribución por App Distribution

**Contexto.** Google Play exige cuenta de desarrollador de tipo organización para
las aplicaciones de salud, lo que requiere número D-U-N-S con un trámite que puede
extenderse hasta treinta días.

**Decisión.** La publicación queda fuera de alcance en esta fase. La distribución
a los participantes de la validación se realiza mediante App Distribution.

**Razonamiento.** Permite ejecutar la validación con usuarios reales sin depender
del trámite, conservando la instalación controlada y la recolección de reportes de
fallos. El trámite del D-U-N-S se inicia con anticipación respecto de la fecha
prevista de publicación, no ahora.

---

## 2026-09-08 · El detalle de la atención vive en la conversación

**Contexto.** Cada atención necesita dejar registro de lo realizado. Un registro
clínico estructurado con diagnóstico implicaría una tabla adicional con semántica
de solo agregar, autoría y fecha, por tratarse de un registro con valor legal.

**Decisión.** En esta fase, el detalle de lo realizado se registra en la
conversación de la solicitud. El registro clínico estructurado queda fuera de
alcance.

**Razonamiento.** Un campo de texto editable no sirve para un diagnóstico: un
`UPDATE` sobrescribe sin dejar rastro, no guarda autoría ni momento, y admite un
solo registro por atención. Hacerlo bien exige una tabla propia, que excede el
alcance de esta fase.

**Condición que debe respetarse.** No dispersar el detalle de la atención en
campos improvisados de otras tablas. Mientras viva solo en la conversación,
incorporar después una tabla vinculada al servicio es puramente aditivo. Si
apareciera un campo suelto «para salir del paso», la migración posterior
arrastraría registros sin autoría, sin fecha y sin garantía de no haber sido
alterados.

**Restricción asociada.** Como la conversación contiene el detalle de la atención,
es información sensible. El rol administrador queda excluido de la tabla de
mensajes mediante política.

---

## 2026-09-08 · Sin pasarela de pagos, con abstracción preparada

**Contexto.** Integrar una pasarela real en Bolivia exige empresa constituida con
NIT y afiliación bancaria, con plazos de meses. No es viable para el alcance
actual.

**Decisión.** El pago ocurre fuera de la aplicación, en efectivo o por
transferencia directa. La aplicación lo registra mediante confirmación de ambas
partes y acumula la comisión adeudada por el profesional.

**Razonamiento.** Permite sostener el modelo de comisiones sin la pasarela, y
otorga propósito real y demostrable al rol de administrador financiero definido
en los requisitos. Es además honesto: no simula una pasarela inexistente.

**Preparación para la fase siguiente.** La tabla `payments` contempla desde ahora
los campos `method`, `provider` y `external_reference`, además de una máquina de
estados. Incorporar una pasarela consistirá en agregar una función de servidor que
reciba su notificación. **No requiere cambio de esquema.**

---

## 2026-09-09 · Identificadores en inglés, documentos en español

**Contexto.** El modelo de datos presentaba una inconsistencia: nombres de tabla
en inglés con nombres de columna en español.

**Decisión.** Todo identificador de código y de esquema se escribe en inglés.
Los documentos del proyecto —`plan.md`, `requirements.md`, `decisions.md`,
`README.md`— se escriben en español.

**Razonamiento.** Una convención mixta es difícil de defender y genera fricción
permanente al escribir consultas. El inglés para identificadores coincide con la
práctica del ecosistema y con la del proyecto previo del autor. El español para
los documentos responde a que son evidencia académica que revisa el tribunal.

**Consecuencia.** Las columnas del modelo se renombraron antes de escribir la
primera migración: `nombre_completo` pasa a `full_name`, `estado_verificacion` a
`verification_status`, `monto_bob` a `amount_bob`, y así con el resto. El mapeo
completo está en `.claude/rules/glosario.md`. Hacerlo ahora no tiene costo; después
de la primera migración lo habría tenido.

---

## 2026-09-09 · Valores de enumerado en mayúsculas también en la base de datos

**Contexto.** PostgreSQL admite cualquier convención para los valores de un tipo
enumerado.

**Decisión.** Los valores se escriben en `SCREAMING_SNAKE_CASE`, igual que las
constantes de enumerado de Kotlin.

**Razonamiento.** La correspondencia con las constantes del lenguaje es directa y
elimina una capa de conversión entre el modelo de dominio y el de transporte. Se
aparta de la convención habitual en PostgreSQL, que suele usar minúsculas, y esa
desviación se acepta deliberadamente a cambio de la consistencia con el código.

---

## 2026-09-09 · Sprints de dos semanas con velocidad medida, no supuesta — SUPERADA EN PARTE

> **La duración quedó superada por la entrada del 2026-09-14**, que la corrige a
> una semana con el tramo real medido. Lo demás de esta entrada sigue vigente: la
> capacidad estimada, la velocidad medida en vez de supuesta y el traslado de
> historias como mecanismo de ajuste. Se conserva porque documenta el
> razonamiento original.

**Contexto.** El proyecto sigue SCRUM y requiere planificación por sprints.

**Decisión.** Sprints de dos semanas. La capacidad inicial se estima entre veinte
y veinticinco puntos, pero la velocidad real se **mide** al cerrar el Sprint 1 y
la planificación de los siguientes se ajusta con ese valor.

**Razonamiento.** Una velocidad supuesta que no se contrasta convierte la
planificación en una ficción. El traslado de una historia al sprint siguiente no
es un fracaso: es el mecanismo de ajuste de SCRUM, y su registro en las
retrospectivas es evidencia directa del carácter iterativo del desarrollo.

---

## 2026-09-09 · El agente no ejecuta operaciones de git

**Contexto.** El desarrollo se apoya en un agente que crea y modifica archivos.

**Decisión.** El agente nunca ejecuta un comando de git. El autor gestiona el
control de versiones de forma manual. Al terminar una tarea, el agente informa qué
archivos creó, modificó o eliminó.

**Razonamiento.** El historial del repositorio es evidencia académica del
desarrollo incremental. El autor debe conservar control total sobre qué se
versiona, cuándo y con qué mensaje, para que ese historial refleje su propio
proceso y no una secuencia generada.

**Alcance.** Editar `.gitignore` sí está permitido: es configuración del proyecto,
no una operación sobre el historial.

---

## 2026-09-09 · Internacionalización desde el primer día

**Contexto.** El producto se lanza únicamente en español. Traducirlo no está
previsto en esta fase.

**Decisión.** Ningún texto visible se escribe en el código. Todo proviene de
recursos de cadenas con claves en inglés, con `values/` como reserva y `values-es/`
como idioma inicial, desde el Sprint 0.

**Razonamiento.** El costo de hacerlo desde el inicio es prácticamente nulo: la
misma cantidad de texto, en otro archivo. El costo de introducirlo después es
recorrer cada pantalla del proyecto extrayendo literales, con el riesgo de omitir
alguno. Agregar un idioma pasa a ser traducir un archivo.

**Consecuencia sobre el dominio.** Los tipos de resultado de la capa de dominio
transportan **tipos de error**, no frases. Un caso de uso devuelve
`RequestError.ProfessionalNotVerified`, no un texto. La capa de presentación
traduce ese tipo a una clave de recurso.

Esto se aparta del patrón `Error(message: String)` del proyecto previo del autor.
El motivo es que una frase en la capa de dominio la ata a un idioma y a una
presentación: el dominio no debe conocer los recursos de Android, y la misma
condición de error puede necesitar presentarse distinto en cada pantalla.

---

## 2026-09-09 · La suite de pruebas pasa completa en cada iteración

**Contexto.** Es habitual acumular pruebas deshabilitadas o marcadas como
ignoradas, con la intención de corregirlas más adelante.

**Decisión.** Ninguna historia se cierra con una prueba en rojo. No existe la
prueba deshabilitada temporalmente, la ignorada por anotación ni la comentada.

**Razonamiento.** Una suite con pruebas apagadas deja de ser una red de seguridad:
nadie distingue entre lo que está roto y lo que está pendiente, y el equipo se
acostumbra a ver rojo. Si una prueba falla, o el código está mal y se corrige, o la
prueba expresaba mal la regla y se corrige la prueba explicando por qué.

**Criterio complementario sobre qué se prueba.** Una prueba vale por el error que
puede atrapar, no por la línea que ejecuta. Se escriben pruebas para reglas de
negocio, casos límite exactos, condiciones de carrera, políticas de seguridad,
atomicidad de operaciones y datos congelados. No se escriben para elevar un
porcentaje de cobertura ni para verificar que un simulador fue invocado.

---

## 2026-09-09 · Dirección visual y correcciones de contraste

**Contexto.** Se adoptó como referencia visual un kit de interfaz para
aplicaciones de salud, con una paleta basada en un teal profundo.

**Decisión.** Se conserva la dirección visual de la referencia —teal primario,
tarjetas de radio amplio, encabezado con degradado, navegación flotante,
tipografía Inter—. Las pantallas se diseñan a partir de los requisitos; la
referencia no dicta los flujos.

**Correcciones necesarias.** La verificación de contraste de la paleta detectó dos
combinaciones que no cumplen los criterios de accesibilidad:

- **`#FFA600` sobre blanco: 1,96:1.** El ámbar no puede usarse como color de texto
  ni de icono pequeño sobre fondo claro. Se usa como relleno, con texto oscuro
  encima. En particular, el número de la calificación va en tinta oscura, no en
  ámbar.
- **Blanco sobre `#1A8F33`: 4,23:1.** Insuficiente para texto de tamaño normal. El
  verde de estado se oscurece a `#15782B`, que alcanza 5,59:1.

El teal primario `#1A6F8F` con texto blanco alcanza 5,73:1 y se conserva sin cambio.

**Por qué importa aquí más que en otro producto.** Una parte relevante de los
usuarios serán adultos mayores o sus familiares. El contraste y el tamaño mínimo
de texto no son un requisito formal a cumplir: son condición de uso.

**Ubicación del material.** Las imágenes de referencia viven en
`docs/design/references/`, no en los recursos de la aplicación, porque son
documentación y no material del producto. Las fotografías de personas del kit no
se usan en la aplicación.

---

## 2026-09-10 · Abandono de Kotlin Multiplatform: Android exclusivamente

**Supersede la decisión del 2026-09-05 sobre Kotlin Multiplatform.**

**Contexto.** La estructura multiplataforma se adoptó para preservar la
portabilidad a iOS a costo marginal. Durante el Sprint 0 aparecieron dos datos que
no estaban disponibles al decidir.

El primero es práctico: la configuración de Gradle y de los conjuntos de fuentes
resultó considerablemente más costosa de lo estimado. La cifra de «dos días» del
razonamiento original corresponde a alguien con experiencia previa en la
tecnología, no a la primera vez.

El segundo es de fondo, y pesa más: **el alcance declarado del proyecto es
Android**. Sostener ante un tribunal una arquitectura multiplataforma cuando el
alcance no contempla una segunda plataforma exige justificar complejidad que el
problema no pide. El motivo real —ahorrar trabajo en una fase futura que no forma
parte del proyecto— es una razón de conveniencia, no de ingeniería.

**Decisión.** El proyecto es exclusivamente Android, en un único módulo Gradle
`:app`, con paquetes por característica. Se elimina el módulo compartido, el
objetivo de compilación de iOS y toda dependencia elegida por ser portable.

**Momento del cambio.** Se tomó antes de escribir la capa de datos, el catálogo de
versiones y las migraciones. El costo de revertir fue prácticamente nulo: recrear
el proyecto desde la plantilla estándar de Android conservando el `applicationId` y
las huellas de firma, que es lo único que ata la aplicación a los servicios
externos ya configurados. Ninguna configuración de Supabase, Google Cloud o
Firebase debió rehacerse.

**Consecuencia que hay que asumir.** La regla de dependencia de la arquitectura
limpia deja de estar garantizada por el compilador. En un solo módulo, nada impide
técnicamente que la capa de presentación importe la de datos. La garantía se
sustituye por dos mecanismos explícitos: reglas de análisis estático que verifican
las importaciones de `domain/` y de `presentation/`, y la prueba transversal
`domainLayerHasNoPlatformImports`. Es una garantía más débil, y por eso se
documenta como tal en lugar de fingir que el resultado es equivalente.

**Nota para la defensa.** Revisar una decisión cuando aparecen datos que no se
tenían al tomarla es criterio de ingeniería, no inconsistencia. La alternativa
—sostener una decisión que ya se sabe desalineada con el alcance— habría sido peor.

---

## 2026-09-10 · Sin base de datos local en esta fase

**Contexto.** El stack inicial incluía Room para caché local. Al revisar la
trazabilidad se detectó que **ningún requisito de `docs/requirements.md` exige
operación sin conexión**, y que un criterio de aceptación de la historia HU-03 la
daba por supuesta sin respaldo.

**Decisión.** Se elimina Room del alcance. `DataStore` cubre la sesión y las
preferencias del usuario. El criterio de aceptación sin respaldo se retiró de HU-03.

**Razonamiento.** Es el mismo error que la adopción de multiplataforma: agregar
una pieza porque podría ser útil, sin un requisito que la exija. Configurar una
base de datos local, sus entidades, sus objetos de acceso y su estrategia de
sincronización es trabajo real que no sirve a ningún requisito declarado.

**Vía de incorporación posterior.** Registrado como FA-08 en
`docs/requirements.md`. Si aparece la necesidad, primero se agrega el requisito y
después la implementación. No al revés.

---

## 2026-09-10 · Solo dependencias en versión estable

**Contexto.** Las plantillas de Android Studio proponen por omisión versiones de
vista previa de Compose y de Material 3.

**Decisión.** Ninguna dependencia en versión `alpha`, `beta`, `rc` ni `SNAPSHOT`.
El catálogo de versiones se revisa en HT-03 y toda versión de vista previa se
reemplaza por la estable vigente.

**Razonamiento.** El proyecto se defiende dentro de varios meses. Una API en
desarrollo puede cambiar entre sprints, y una migración forzada a mitad del
desarrollo consume tiempo que no aporta al producto. La estabilidad de las
dependencias es una condición de reproducibilidad, que es un atributo de calidad
exigible en la defensa.

---

## 2026-09-10 · Prototipado por sprint, no por adelantado

**Contexto.** La historia técnica del sistema de diseño incluía prototipar en
Figma los seis flujos del producto dentro del Sprint 0. Era el criterio más caro
del sprint, no era trabajo de código, y podía bloquear el cierre de la fundación
por un motivo ajeno a la arquitectura.

**Decisión.** El Sprint 0 implementa el tema y los componentes base. **Las
pantallas se prototipan al planificar el sprint que las construye**, no antes.

**Razonamiento.** Prototipar seis flujos por adelantado produce trabajo que se
rehace: el diseño de un flujo se beneficia de lo aprendido construyendo el
anterior. Distribuir el prototipado por sprint es además más consistente con un
desarrollo que se declara incremental e iterativo.

---

## 2026-09-10 · Diagramas versionados en Mermaid, no imágenes

**Contexto.** La documentación de arquitectura exigía diagramas sin fijar dónde
viven ni en qué notación.

**Decisión.** Los diagramas viven en `docs/architecture/`, escritos en **Mermaid
dentro de archivos Markdown**. Un archivo por diagrama: componentes, paquetes,
secuencia de autenticación y despliegue.

**Razonamiento.** Un diagrama en texto se versiona, se compara entre revisiones y
se renderiza en el repositorio. Una imagen exportada queda desactualizada en
silencio y no permite ver qué cambió. Como los diagramas son evidencia de tesis,
que su evolución sea trazable tiene valor adicional.

---

## 2026-09-11 · `namespace` y `applicationId` difieren de forma deliberada

**Contexto.** Al recrear el proyecto desde la plantilla de Android, Android Studio
fijó `namespace = bo.saludencasa.app` y ubicó las fuentes bajo
`app/src/main/java/bo/saludencasa/app/`, mientras la documentación acordaba
`bo.saludencasa` como paquete base.

**Decisión.** Se corrige el proyecto, no los documentos.

| Propiedad | Valor | Motivo |
|---|---|---|
| `applicationId` | `bo.saludencasa.app` | Inmutable. Las huellas SHA-1, los identificadores OAuth y `google-services.json` están atados a él |
| `namespace` | `bo.saludencasa` | Se corrige. Ningún servicio externo lo utiliza |

**Razonamiento.** Al momento de detectarlo existían cuatro archivos fuente. Cuando
existan `core/`, `di/`, `navigation/`, `features/` y sus pruebas, mover el paquete
sería un refactor de cientos de archivos. Corregir la documentación habría sido más
barato hoy y más caro después.

**Sobre la divergencia.** Que ambos valores difieran es válido y habitual en el
complemento de compilación de Android: el `namespace` define la raíz de los
paquetes y de la clase de recursos generada; el `applicationId` identifica la
aplicación ante la tienda y los servicios. **El `applicationId` no se cambia
nunca**, porque hacerlo invalidaría toda la configuración externa ya cerrada en
HT-02.

---

## 2026-09-11 · `google-services.json` se excluye del control de versiones

**Contexto.** El archivo contiene la clave de API de Firebase y la configuración
del proyecto. RNF-06 establece que ninguna credencial figura en el control de
versiones, y su criterio de verificación es la revisión del historial del
repositorio.

**Decisión.** El archivo se **excluye**. El `README.md` documenta cómo obtenerlo
desde la consola de Firebase.

**Razonamiento.** La clave que contiene está restringida y es extraíble de
cualquier paquete de aplicación, de modo que el riesgo es bajo. Pero mantener
RNF-06 sin excepciones es más simple de sostener que introducir una categoría de
«credencial que sí se versiona», que obligaría a justificar el límite cada vez.

El argumento en contra sería la reproducibilidad: sin el archivo el proyecto no
compila desde una copia limpia. Ese argumento no aplica aquí, porque el proyecto
tampoco compila sin `local.properties`, que contiene las claves de Supabase y de
Mapas. Excluirlo no introduce una barrera nueva.

**Consecuencia operativa.** El archivo se ubica en `app/`, que es donde lo busca
el complemento de Google Services. Estaba en la raíz del proyecto, donde no lo
lee ningún complemento.

---

## 2026-09-11 · Color dinámico desactivado

**Contexto.** La plantilla de Android habilita el color dinámico de Material You,
que desde Android 12 deriva la paleta del fondo de pantalla del dispositivo.

**Decisión.** Se desactiva. El parámetro se fija en `false` o se elimina.

**Razonamiento.** El color dinámico reemplaza la paleta completa en tiempo de
ejecución. Con `docs/design-system.md` fijando el teal `#1A6F8F` y dos
correcciones de contraste verificadas —el ámbar que no puede llevar texto sobre
fondo claro y el verde que debe oscurecerse—, dejarlo activo anularía HT-06
completo y haría inverificable el punto 9 de la Definición de Terminado.

En una aplicación de salud con usuarios adultos mayores, el contraste no es una
preferencia estética: es condición de uso. No puede quedar a merced del fondo de
pantalla que cada persona tenga configurado.

---

## 2026-09-11 · Un solo número de versión de Java en todo el proyecto: 17

**Contexto.** Al ejecutar HT-01 aparecieron tres números distintos de máquina
virtual de Java conviviendo en el mismo proyecto. El archivo
`gradle/gradle-daemon-jvm.properties`, que Android Studio genera de forma
automática, exigía la versión 25 para el proceso que ejecuta Gradle. El `README.md`
y la etapa de preparación de HT-08 declaraban 17. La plantilla dejaba
`compileOptions` en 11.

Las dos primeras cifras designan la máquina virtual sobre la que corre la
herramienta de compilación; la tercera designa el código intermedio que se
produce. Son cosas distintas y pueden diferir sin que nada falle, que es
precisamente lo que las hacía difíciles de detectar.

**Decisión.** Las tres se unifican en **17**: el proceso de Gradle, el objetivo de
`compileOptions` y de `jvmTarget`, y el JDK que declara el `README.md` y que usará
la integración continua.

**Razonamiento.** El problema no era de funcionamiento sino de reproducibilidad,
que es un atributo de calidad exigible en la defensa y el mismo criterio que ya
motivó exigir dependencias en versión estable. Con dos números conviviendo, la
integración continua de HT-08 se habría configurado con JDK 17 mientras el archivo
del daemon pedía 25: el resultado es una descarga silenciosa de otra máquina
virtual en cada ejecución, o un fallo, según cómo quedara configurado el flujo. Un
proyecto que se levanta desde cero siguiendo su propio `README.md` no debería
depender de esa resolución implícita.

Se eligió 17 y no 25 porque es la versión que ya declaraban los documentos, porque
es de soporte prolongado, y porque el plugin de compilación de Android en su
versión 9.3.2 la acepta sin restricción. Se verificó ejecutando `./gradlew build`
completo con el daemon fijado en 17.

**Consecuencia.** `gradle/gradle-daemon-jvm.properties` queda versionado con
`toolchainVersion=17` y las direcciones de descarga correspondientes para cada
sistema operativo. Quien clone el proyecto obtiene la misma máquina virtual sin
instalarla a mano: si no la tiene, Gradle la descarga. El archivo se regenera con
`./gradlew updateDaemonJvm --jvm-version=<version>` y **nunca se edita a mano**,
porque las direcciones de descarga van atadas a la versión.

**Qué vigilar.** Android Studio puede volver a generar este archivo con la versión
de su máquina virtual incorporada al actualizarse. Si reaparece un número distinto
de 17, es eso y no un cambio deliberado.

---

## 2026-09-11 · La version de Kotlin la fija la dependencia mas nueva, no la plantilla

**Contexto.** La plantilla de Android Studio dejo Kotlin en 2.2.10. Al declarar el
catalogo de HT-03 con las versiones estables vigentes, la compilacion fallo:

```
Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin.
The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read
versions up to 2.3.0.
```

El origen resulto ser `com.google.maps.android:maps-compose:8.6.0`, que arrastra
`kotlin-stdlib:2.4.10`. Una biblioteca compilada con Kotlin 2.4 no puede
consumirse desde un compilador 2.2: cada compilador lee metadatos hasta una
version menor por encima de la suya.

**Decision.** Kotlin pasa a **2.4.20**, la estable vigente. La version deja de ser
un valor heredado de la plantilla y pasa a ser una **cota inferior** impuesta por
la dependencia mas moderna del catalogo.

**Razonamiento.** La alternativa era congelar `maps-compose` en una version
anterior para no mover Kotlin. Eso habria cambiado un problema visible por uno
latente: la siguiente dependencia que se actualice vuelve a romper la compilacion,
y el proyecto acumula versiones antiguas por una razon que nadie recuerda. Subir
el compilador es la correccion en la causa.

**Como se verifico.** Se cablearon temporalmente las once dependencias del
catalogo —Supabase, Koin, Mapas, Credential Manager, DataStore, Coil y el
complemento de serializacion— y se compilo el proyecto completo. Con Kotlin 2.2.10
fallaba; con 2.4.20 compila. El cableado temporal se revirtio despues: cada
historia declara lo que necesita.

**Consecuencia y regla que queda.** Antes de agregar una dependencia al catalogo,
comprobar con que version de Kotlin fue compilada. Si exige una superior, se sube
Kotlin en el mismo cambio, nunca se fija la dependencia en una version vieja para
evitarlo. El sintoma es siempre el mismo mensaje de metadatos incompatibles.

---

## 2026-09-11 · La ficha publica del profesional es una vista, no una politica

**Contexto.** Tres requisitos tiran en direcciones opuestas sobre la misma fila.
RF-02.6 y RF-06.3 exigen que cualquier usuario vea el nombre, la foto, la tarifa
y la reputacion de un profesional. RN-06 y RF-08.6 exigen que el telefono
permanezca oculto hasta que se acepte una oferta. INV-13 exige que nadie lea
filas ajenas.

La seguridad a nivel de fila resuelve filas, no columnas: una politica que
permita leer la fila de un profesional permite leer **todas** sus columnas,
telefono incluido. Y los privilegios por columna de PostgreSQL son por rol, no
por fila, de modo que quitar `phone` al rol autenticado se lo quitaria tambien a
cada usuario sobre su propia fila.

**Decision.** La fila de `profiles` es privada: se lee solo por su dueno, por el
administrador, y por la contraparte una vez que existe un servicio entre ambos.
La ficha publica se sirve por la vista `professional_directory`, que **no
contiene las columnas de contacto**.

**Razonamiento.** El telefono no queda oculto por una condicion que alguien pueda
relajar mas adelante, sino porque no esta en la proyeccion. Es una garantia
estructural: para filtrarlo habria que anadir deliberadamente la columna a la
vista, lo que se ve en la revision de una migracion.

**Detalle que hay que entender antes de tocarla.** La vista se declara
`security_invoker = false`, es decir, se ejecuta con los privilegios de su dueno
y no aplica las politicas de las tablas base. Es intencional: debe leer filas que
el llamante no puede leer por si mismo. Lo que hace de filtro es su propia
clausula `where`, que exige `verification_status = 'APPROVED'` y `profiles.active`
y con ello materializa INV-07. **Anadir una columna de contacto a esta vista es
una fuga de datos**, no un cambio cosmetico.

**Consecuencia.** La busqueda por cercania devuelve el mismo conjunto de columnas
publicas. La revelacion del contacto es una politica aparte sobre `profiles`,
condicionada a `shares_service_with()`, que existe desde la migracion que crea
`services` porque es la condicion de la que depende.

---

## 2026-09-11 · La reputacion vive en `profiles`, no en `professionals` — SUPERADA

> **Superada por la entrada del 2026-10-01 sobre la reputación por rol.** Esta
> entrada describió con precisión el costo de la alternativa —«el paciente
> necesitaría una columna paralela en `patients` y el disparador de recálculo
> tendría que decidir a cuál escribir según el rol del destinatario»— y ese costo
> es exactamente el que se paga ahora. Lo que cambió no es el razonamiento sino su
> premisa: era correcto mientras una persona tuviera un solo rol. Con rol múltiple,
> una sola columna mezcla la reputación como paciente con la reputación como
> profesional, y `reviews_select_visible` haría pública la primera. Se conserva
> porque documenta por qué la forma simple era la correcta en su momento.

**Contexto.** El glosario nombra `averageRating` sin fijar en que tabla reside, y
la lectura natural es ponerla en `professionals`, que es donde la muestra la
ficha publica.

**Decision.** `average_rating` y `total_reviews` son columnas de `profiles`.

**Razonamiento.** La calificacion es de ida y vuelta: RF-12.2 hace que el
profesional califique al paciente, y HU-30 pide que otro profesional vea la
reputacion de ese paciente antes de acudir a un domicilio. Si la reputacion
viviera en `professionals`, el paciente necesitaria una columna paralela en
`patients` y el disparador de recalculo tendria que decidir a cual escribir segun
el rol del destinatario. En `profiles` hay una sola columna, un solo disparador y
ninguna rama.

Es ademas coherente con INV-01: `profiles` es la identidad de toda persona, y la
reputacion es un atributo de la persona, no del rol que desempena en una
atencion.

**Que se queda en `professionals`.** `total_services`, el numero de atenciones
prestadas, porque solo tiene sentido para quien las presta. Lo incrementa un
disparador cuando un servicio pasa a `COMPLETED`, no el cliente.

---

## 2026-09-11 · La CLI de Supabase es dependencia del proyecto, y se trabaja contra el proyecto remoto

**Contexto.** Las migraciones de HT-04 necesitaban aplicarse y verificarse. La
via que documenta Supabase para desarrollo es levantar la pila completa en local
con Docker y reconstruirla con `supabase db reset`. La maquina de desarrollo
tiene Docker instalado pero el demonio detenido, y la pila local descarga varios
gigabytes de imagenes.

**Decision.** Dos partes.

Primero, la CLI se instala como **dependencia de desarrollo del proyecto**
(`npm install supabase --save-dev`) con su version exacta fijada en
`package.json`, y se invoca con `npx supabase`. No se instala de forma global.

Segundo, en esta etapa el esquema se aplica y se verifica **contra el proyecto
remoto de desarrollo**, con `npx supabase db push`. El entorno local con Docker
queda fuera de alcance.

**Razonamiento.** Una CLI global es una version distinta en cada maquina y en
integracion continua, que es justo el problema que el catalogo de versiones
resuelve para las dependencias de Android. Fijarla en `package.json` la somete a
la misma regla de reproducibilidad.

Sobre el entorno local: el proyecto remoto de desarrollo ya existe desde HT-02,
es gratuito y es donde la aplicacion se conecta de todos modos. Levantar ademas
una pila local no aporta nada que el proyecto necesite hoy, y si aporta varios
gigabytes y una fuente mas de divergencia entre entornos.

**Consecuencia que hay que asumir.** El criterio de HT-04 que pedia que
`supabase db reset` reconstruyera la base completa queda **aplazado**, no
cumplido. Es el criterio que demuestra que las migraciones reconstruyen el
esquema desde cero, y esa es una propiedad que conviene poder ensenar en la
defensa. Se recupera cuando exista el entorno local o el proyecto de produccion,
y hasta entonces figura como pendiente en `plan.md` en lugar de darse por bueno.

**Consecuencia operativa.** `npx supabase db push` se ejecuta siempre primero con
`--dry-run`. Contra un proyecto remoto no hay deshacer: una migracion aplicada no
se edita, se corrige con otra.

---

## 2026-09-12 · Guardias de escritura contra el propio dueño de la fila, distinguidas por profundidad de disparador

**Contexto.** Una revisión de seguridad detectó que `profiles_update_own` y
`professionals_update_own` solo verifican de quién es la fila, nunca qué
columnas cambian. Nada impedía que un paciente o un profesional escribiera a
mano `average_rating`, `total_reviews`, `active` o `total_services`: un
profesional podía inflar su propia reputación, restaurar su `active` después de
que un administrador lo desactivara, o inflar el número de atenciones que
exhibe en su ficha pública. Encontró también que `messages_update_patient` y
`messages_update_professional` permiten al destinatario reescribir el
`content` de un mensaje, no solo su `read_at`: la conversación es donde vive el
detalle de la atención (RF-09.3, FA-01), así que una integridad débil ahí
compromete el registro completo.

**Decisión.** Dos mecanismos distintos, elegidos por si hace falta o no un
paso alrededor de la comprobación.

Para `profiles` y `professionals`, un disparador `before update` que rechaza el
cambio de esas columnas, salvo cuando el llamante es administrador o la
escritura llega anidada dentro de otro disparador propio.

Para `messages`, un privilegio por columna: se revoca `update` por completo al
rol `authenticated` y se concede solo sobre `read_at`. No hace falta disparador
porque INV-12 ya excluye a cualquier administrador de esta tabla, y ninguna
función interna escribe en ella: no hay cascada que distinguir.

**Razonamiento.** `recalculate_reputation()` e `increment_total_services()` son
disparadores `security definer` que escriben exactamente esas columnas de
`profiles` y `professionals`, y lo hacen desde dentro de una solicitud de un
paciente o profesional común: calificar o marcar un servicio completado.
`auth.uid()` no cambia dentro de una función `security definer`, solo cambia el
rol para efectos de privilegio. Una guardia que solo comprobara
`not is_admin()` habría bloqueado también esa escritura legítima, porque
`is_admin()` es falso en ambos casos: en el intento de fraude y en el
recálculo real.

`pg_trigger_depth()` los distingue sin ambigüedad. Una escritura directa del
cliente alcanza el disparador de guardia en profundidad 1. La escritura de
`recalculate_reputation()` la alcanza en profundidad 2, porque ya viene
ejecutándose desde dentro del disparador `after insert` que la tabla
`reviews` disparó primero. Ningún rol que el cliente controla puede insertarse
entre esos dos niveles: los disparadores son objetos del esquema, no algo que
`authenticated` pueda adjuntar por su cuenta.

**Por qué `messages` no necesita lo mismo.** No hay ninguna función que escriba
en `messages` desde otro disparador, y no hay ningún administrador al que haya
que dejar pasar. El privilegio por columna resuelve el caso completo sin la
complejidad de una guardia con profundidad.

**Consecuencia.** Ninguna migración aplicada se editó. La corrección es una
migración nueva, `harden_self_service_columns`, sobre columnas que ya existían
desde `identity` y `requests`.

---

## 2026-09-12 · El nonce viaja en dos formas, y la sesión se guarda en DataStore

**Contexto.** HT-05 conecta Credential Manager con Supabase. Dos detalles de esa
cadena no se deducen leyendo el código y cuestan horas de diagnóstico cuando se
equivocan, porque el error que devuelven no dice qué pasó.

**El nonce tiene dos valores, no uno.** Google incrusta en el token de identidad
el resumen SHA-256 del nonce, mientras que Supabase lo verifica contra el valor
crudo. Entregar la misma cadena a los dos lados produce un token que Supabase
rechaza sin explicar el motivo.

**Decisión.** El nonce se modela como un tipo con dos propiedades, `raw` y
`hashed`, cada una nombrada por el destino al que va. No existe un constructor
público que permita armar uno inconsistente, y una prueba unitaria fija el
resumen contra un vector conocido. Intercambiarlos deja de ser posible por
descuido: hay que escribir el nombre equivocado a propósito.

**La sesión se guarda en DataStore, no en el almacenamiento por omisión de la
biblioteca.** El cliente de Supabase trae su propio gestor de sesión, que en
Android se apoya en otra dependencia de preferencias.

**Decisión.** Se implementa `SessionManager` sobre DataStore.

**Razonamiento.** `docs/decisions.md` ya fijó que DataStore es el único
almacenamiento local de esta fase, y que no hay base de datos local porque ningún
requisito exige operar sin conexión. Aceptar el gestor por omisión habría metido
una segunda biblioteca de almacenamiento por la puerta de atrás, para guardar el
mismo dato, sin ningún requisito que la pidiera. Es el mismo criterio que descartó
Room y la estructura multiplataforma.

**Consecuencia.** RF-01.6 —la sesión sobrevive a un reinicio y el token se renueva
solo— depende de tres opciones del cliente que están escritas de forma explícita
en `CoreModule` aunque sean las de por omisión: `sessionManager`,
`autoLoadFromStorage` y `alwaysAutoRefresh`. Se escriben porque un requisito
depende de que sigan así, no porque haga falta activarlas.

---

## 2026-09-12 · La profundidad de disparador dentro de una cláusula `WHEN` no es la misma que dentro del cuerpo de la función

**Contexto.** La entrada anterior de este mismo día afirma que una escritura
directa del cliente alcanza el disparador de guardia en profundidad 1, y la
escritura anidada de `recalculate_reputation()` o `increment_total_services()`
lo alcanza en profundidad 2. Esa cifra era correcta para código que llama
`pg_trigger_depth()` dentro del **cuerpo** de la función del disparador, que es
donde se probó en su momento. Los disparadores de `harden_self_service_columns`
la llaman en la cláusula `WHEN`, no en el cuerpo, y ahí Postgres reporta un
número menos: la escritura directa llega en profundidad 0, la anidada en
profundidad 1. La condición `pg_trigger_depth() <= 1`, escrita para el número de
profundidad equivocado, seguía siendo verdadera para la escritura anidada
legítima y la bloqueaba: calificar a alguien o completar un servicio fallaba con
`reputation_and_active_status_are_managed_by_the_server` o
`total_services_is_maintained_by_the_server`, en un sistema que ya estaba
aplicado al proyecto remoto.

Lo encontró el revisor automático de GitHub en el pull request #1. No se aceptó
por su palabra: se verificó con un experimento propio contra la base de datos
remota, con tablas temporales y disparadores anidados de prueba, antes de tocar
nada. `docs/decisions.md` documenta el fundamento del diseño, y una entrada con
un número equivocado en su razonamiento es peor que no tener la entrada.

**Decisión.** Se deja esta entrada nueva en vez de editar la anterior. La
anterior registra fielmente qué se pensaba y por qué en el momento de escribir
la migración original; esta registra qué resultó cierto al verificarlo y qué
corrigió. Borrar o reescribir la primera perdería esa secuencia, que es
justamente el ciclo de inspección y adaptación que SCRUM pide documentar.

La corrección en sí —cambiar `<= 1` por `<= 0` en las dos cláusulas `WHEN`— vive
en una migración nueva, `close_lifecycle_and_visibility_gaps`, junto con el
resto de la respuesta a esa revisión.

**Consecuencia.** Cualquier disparador futuro que necesite distinguir una
escritura directa de una anidada debe decidir, antes de escribir la condición,
si `pg_trigger_depth()` se lee dentro de la cláusula `WHEN` o dentro del cuerpo
de la función, porque el número correcto no es el mismo en los dos lugares.

---

## 2026-09-12 · Respuesta a la revisión del pull request #1: qué se corrigió y qué queda como deuda

**Contexto.** El revisor automático de GitHub señaló 24 observaciones sobre la
rama `ht-05-supabase-client`. Veintiuna se corrigieron —listadas en la entrada de
HT-05 en `plan.md`—, casi todas en la migración `close_lifecycle_and_visibility_gaps`
más dos correcciones en `GoogleAuthClient`. Tres quedan deliberadamente sin
corregir.

**Decisión y razonamiento, por cada deuda:**

- **La franja de disponibilidad no se valida al crear una solicitud agendada**
  (RF-07.3). Validar contra `availability_slots` en el momento de la inserción
  exige decidir cómo se comparan un horario semanal declarado y una fecha
  concreta, con zona horaria y semántica de excepciones que hoy no existen en
  ningún lado del esquema. Es trabajo de la historia que construya la
  programación de citas, no una corrección de revisión.
- **No existe flujo de integración continua** que mapee los secretos del
  repositorio a variables de entorno para `local.properties`. El criterio de
  aceptación de HT-05 lo da por hecho porque copia la redacción de RNF-06, pero
  este proyecto no tiene todavía ningún flujo de trabajo en `.github/workflows/`:
  se decidió no improvisar uno solo para cerrar esta observación, porque
  configurar integración continua es una decisión de alcance propia, no un
  efecto colateral de una revisión de código.
- **La operación atómica de RF-08.5** —aceptar una oferta crea el servicio y el
  pago pendiente en una sola operación— todavía no existe. La migración de esta
  revisión cierra el camino que la sustituía sin querer: ya no es posible poner
  una oferta o una solicitud en `ACCEPTED` con una escritura directa del
  cliente. No abre el camino correcto, porque construir esa función junto con el
  caso de uso y la pantalla que la use es alcance de la historia de negociación
  (HU-14 a HU-19), no de esta corrección. Mientras esa historia no exista, la
  aceptación de una oferta simplemente no es posible desde el cliente, lo cual es
  correcto: tampoco hay pantalla que la ofrezca todavía.

**Consecuencia.** Las tres quedan pendientes de una historia futura que las
declare como su propio criterio de aceptación. Ninguna se marca como resuelta en
`plan.md`.

---

## 2026-09-12 · Sistema de diseño: roles sin hueco en Material 3, e íconos y carga de imagen aplazados

**Contexto.** HT-06 implementa el tema a partir de `docs/design-system.md`. Dos
tipos de brecha aparecieron entre lo que el documento fija y lo que Material 3 o
las bibliotecas del proyecto ya cubren.

**Radios sin hueco en `Shapes`.** Material 3 expone cinco roles
(`extraSmall`/`small`/`medium`/`large`/`extraLarge`); el documento fija ocho
radios distintos. Los cinco que coinciden en cantidad se mapean directamente
(campo de formulario, botón, tarjeta de contenido, hoja inferior). La tarjeta
destacada de 24 dp, la píldora y el avatar circular no tienen rol propio.

**Decisión.** Un objeto adicional, `ExtraShapes`, junto a `Shapes` en
`ui/theme/Shape.kt`, con esos tres radios. Se expone desde `SaludEnCasaTheme`
igual que la tipografía y los colores de estado, no como una constante suelta en
cada pantalla que la use.

**`SaludEnCasaTheme` pasó de función a `object`.** Necesitaba exponer `spacing`,
`statusColors` y `extraShapes` junto a la función que envuelve el contenido.
`operator fun invoke` permite las dos cosas con el mismo nombre —
`SaludEnCasaTheme { contenido }` sigue envolviendo la pantalla,
`SaludEnCasaTheme.spacing.screenMargin` lee el token— exactamente como
`MaterialTheme` ya lo hace en la propia biblioteca de Compose. No es un patrón
inventado para este proyecto.

**Deuda reconocida — iconografía.** El apartado 6 de `docs/design-system.md` pide
Material Symbols, redondeado, grosor 400. Los ocho componentes usan
`material-icons-core` (el conjunto núcleo de Material Icons, no `-extended`, por
RNF-03) porque ningún ícono en ellos es contenido real todavía: son la prueba de
que el tema funciona, no una pantalla que un usuario vaya a ver. El cambio al
conjunto correcto queda para cuando una pantalla real los consuma.

**Deuda reconocida — carga de imagen.** `ProfessionalCard` incorpora
`coil-compose`, ya exigido por `.claude/rules/compose.md` para toda fotografía de
perfil, pero sin motor de red (`coil-network-ktor3`, el que combina con el motor
de Ktor que ya usa Supabase). Ningún código del proyecto hace todavía una petición
HTTP de imagen real: sin motor, Coil cae a su estado de marcador de posición en
vez de fallar, así que la ausencia no bloquea esta historia. El motor se agrega
cuando una historia real cargue una fotografía desde una URL.

**Consecuencia.** Ambas deudas quedan registradas aquí y en la nota de HT-06 en
`plan.md`, para que la historia que primero necesite un ícono de Material Symbols
o una fotografía real no las descubra de nuevo desde cero.

---

## 2026-09-12 · La regla de texto escrito en el código se verifica con pruebas JVM, no con un módulo de reglas de ktlint

**Contexto.** HT-07 exige una regla de análisis estático que señale texto visible
escrito directamente en el código, más la prueba `everyStringKeyUsedInCodeExistsInAllLocales`.
ktlint no ofrece una regla de este tipo, y escribir una regla propia de ktlint o de
Detekt exige un artefacto Kotlin separado del que depende `:app`, porque el
mecanismo de extensión de ambas herramientas se distribuye como una biblioteca de
reglas independiente, no como código dentro del módulo que se analiza.

**Decisión.** Tres pruebas JUnit en `app/src/test/java/bo/saludencasa/i18n/`, que
leen el árbol de código fuente y los `strings.xml` directamente del disco con
expresiones regulares, sin depender de Robolectric ni del sistema de recursos de
Android:

- `StringResourcesTest.everyStringKeyUsedInCodeExistsInAllLocales` — toda clave
  `R.string.*` referenciada desde Kotlin existe en cada `values*/strings.xml` de
  idioma (se excluyen calificadores que no son de idioma, como `values-night`).
- `StringResourcesTest.noVisibleTextIsHardcodedInScreens` — ninguna llamada a
  `Text(...)`, `contentDescription = "..."`, `Toast.makeText(...)` o
  `showSnackbar(...)` recibe una cadena literal. Los componibles anotados con
  `@Preview` quedan excluidos del barrido: solo se ejecutan en la herramienta de
  vista previa, nunca en la aplicación instalada, así que sus datos de ejemplo no
  son texto visible al usuario real.
- `DomainErrorModelingTest.domainErrorsCarryTypesNotMessages` — ningún tipo cuyo
  nombre termina en `Error` declara un campo `message: String`, y no aparece la
  construcción `Error("...")` con una frase literal, el patrón que
  `.claude/rules/i18n.md` prohíbe de forma explícita.

**Razonamiento.** El proyecto es de un solo módulo Gradle por decisión registrada
(2026-09-10, reversión de Kotlin Multiplatform) y no tiene overhead de Detekt ni
de un `ruleset` propio de ktlint todavía. Agregar ese andamiaje solo para esta
regla habría sido exactamente la «biblioteca sin requisito que la exija» que la
disciplina de alcance prohíbe: una prueba de JUnit, que el proyecto ya ejecuta con
`./gradlew test` en cada iteración, cumple el mismo propósito sin una dependencia
nueva ni un módulo adicional.

**Consecuencia.** El barrido de texto hardcodeado es una lista fija de patrones
(`Text(`, `contentDescription`, `Toast.makeText`, `showSnackbar`), no un análisis
semántico completo del árbol de sintaxis: no detecta, por ejemplo, texto
concatenado a mano o pasado a un componible propio con un nombre distinto a esos
cuatro. Si HT-08 incorpora Detekt u otra herramienta de análisis estático más
completa, esta prueba puede retirarse a favor de la regla equivalente; hasta
entonces, es la red de seguridad vigente y cualquier patrón nuevo de texto visible
que se detecte en revisión se agrega a `hardcodedVisibleTextPatterns` en
`StringResourcesTest.kt`.

---

## 2026-09-12 · Firebase Crashlytics como panel de monitoreo, y lo que eso obliga a cambiar en la compilación

**Contexto.** HT-08 exige que «un error provocado deliberadamente aparezca en el
panel de monitoreo», pero ningún documento del proyecto nombraba una herramienta:
ni `docs/requirements.md`, que no tiene un requisito de monitoreo, ni `plan.md`,
que solo describe el criterio. Las dos candidatas reales eran Firebase
Crashlytics y Sentry.

**Decisión.** Firebase Crashlytics, con `firebase-bom` y `firebase-crashlytics`,
sin `firebase-analytics`.

**Razonamiento.** Firebase ya es parte del proyecto desde HT-02: la consola está
creada, `app/google-services.json` existe, y el Sprint 10 distribuye la
aplicación por App Distribution, del mismo proveedor, para recoger reportes de
fallos de los participantes de la validación. Crashlytics vive en esa misma
consola y no cuesta nada sin límite de eventos. Sentry se configuraba con un solo
DSN —más cómodo, porque habría entrado por el mismo mecanismo de
`local.properties` y `BuildConfig` que HT-05 ya montó, sin tocar
`google-services.json`—, pero suma un tercer proveedor externo junto a Supabase y
Google, con cuenta propia y un plan gratuito acotado a cinco mil eventos
mensuales. La disciplina de alcance del proyecto empuja a no sumar proveedores
cuando uno ya presente resuelve lo mismo.

`firebase-analytics` queda fuera a propósito: Crashlytics no lo exige, solo
enriquece los rastros con eventos previos al fallo, y RNF-03 limita el paquete a
25 MB. Verificado tras integrarlo: 19 MB en depuración y 14 MB en publicación sin
ofuscación, de modo que el margen sigue siendo cómodo.

**Consecuencia, y es la que conviene recordar.** Los complementos
`com.google.gms.google-services` y `com.google.firebase.crashlytics` leen
`app/google-services.json` **en tiempo de compilación**. Desde esta historia, un
clon sin ese archivo **ya no compila**, cuando antes sí lo hacía. Es un cambio
real en la puesta en marcha y contrasta de forma deliberada con `local.properties`,
cuya ausencia el proyecto tolera a propósito para que el fallo aparezca en
ejecución, donde es legible. Aquí se aceptó lo contrario porque el error de
Gradle nombra el archivo que falta, así que sigue siendo accionable. El
`README.md` lo advierte en la sección de puesta en marcha.

De ahí se sigue el cuarto secreto del repositorio, `GOOGLE_SERVICES_JSON`: el
flujo de integración continua reconstruye ese archivo decodificándolo desde
base64, junto con los tres valores que ya escribía en `local.properties`. El paso
falla de forma explícita si el secreto está ausente, porque sin esa comprobación
el error que aparece más adelante es el del complemento de Google Services, que
no dice nada útil sobre su causa.

**Dónde queda la verificación.** Que las cinco etapas del flujo concluyan y que
el fallo llegue al panel son los dos criterios que el agente no puede cerrar:
exigen cargar los secretos, hacer un envío y mirar la consola. Quedan marcados
como pendientes del autor en `plan.md`, con el procedimiento en el `README.md`.

---

## 2026-09-12 · Los objetos de valor compartidos viven en `core/vo/`

**Contexto.** HU-01 introduce `Email`, `PhoneNumber` y `PersonName`. Ninguno
pertenece a una sola característica: `Email` y `PersonName` llegan con la
identidad que entrega Google y los volverá a usar el perfil, y `PhoneNumber` lo
pide RF-02.1, que es del perfil, no de la autenticación. La estructura de
`.claude/rules/arquitectura.md` solo preveía `features/<feature>/domain/vo/`.

**Decisión.** Los objetos de valor que comparten varias características viven en
`core/vo/`. Los que pertenecen a una sola siguen en su `domain/vo/`.

**Razonamiento.** La alternativa era dejarlos en `features/auth/domain/vo/` y que
el perfil importara el dominio de la autenticación para obtener un número de
teléfono. Eso convierte a la autenticación en dueña de un concepto que no es
suyo, y la dependencia que crea no expresa ninguna relación real entre las dos
características: la próxima que necesite un teléfono heredaría la misma
importación arbitraria. `core/` ya es el lugar de lo transversal.

**Consecuencia.** `core/vo/` es código de dominio aunque no esté bajo un
`domain/`, así que la regla que mantiene el dominio libre de plataforma tuvo que
ampliarse para alcanzarlo: `ArchitectureRulesTest.domainLayerHasNoPlatformImports`
ahora recorre los archivos con un segmento `domain` en su ruta **y** los de
`core/vo/`. Se comprobó que la ampliación sirve de algo poniendo un archivo sonda
en `core/vo/` que importaba `android.util.Log`, viendo fallar la regla, y
borrándolo. Sin esa ampliación, `core/vo/` habría sido el único paquete puro del
proyecto sin nadie que lo vigilara.

---

## 2026-09-12 · Credential Manager vive en la presentación, y el modelo de vista recibe la petición como función

**Contexto.** Obtener el token de identidad de Google exige un `Context` de
actividad: Credential Manager levanta el selector de cuentas sobre ella. El
dominio no puede conocer Android, y un modelo de vista que guarde una actividad
la sobrevive y la filtra.

**Decisión.** `GoogleCredentialClient` vive en
`features/auth/presentation/` y devuelve el token con su nonce en crudo.
`WelcomeViewModel` no lo inyecta: recibe la petición como
`suspend () -> GoogleCredentialResult`, y la pantalla —que sí tiene
`LocalContext`— es quien la construye.

**Razonamiento.** Es lo que pide la tarea técnica de HU-01, y además resuelve dos
cosas de una vez. El modelo de vista queda sin una sola importación de Android,
de modo que sus pruebas corren en la máquina virtual de Java sin emulador: la
prueba de que cancelar el selector no deja error en pantalla —criterio de
aceptación de HU-01— es una prueba unitaria corriente, no una prueba
instrumentada. Y el `Context` nunca se guarda, solo se usa dentro de la llamada.

**Consecuencia.** El intercambio del token con Supabase sí atraviesa el caso de
uso y el repositorio, como corresponde. Queda saldada la deuda que HT-05 había
reconocido: ya no hay ningún modelo de vista consumiendo un cliente de
infraestructura. `GoogleAuthClient`, que hacía las dos mitades a la vez, se
eliminó.

---

## 2026-09-12 · El cierre de sesión sin conexión limpia la sesión guardada

**Contexto.** RF-01.7 exige que, tras cerrar sesión, la aplicación vuelva a pedir
el ingreso. El cliente de Supabase cierra sesión con alcance local por omisión,
pero envía igual la petición al servidor, y si esa petición no llega —sin red—
la excepción sube y la sesión guardada **queda intacta**. Al reabrir, la persona
sigue dentro sin haberlo pedido.

**Decisión.** Cuando el cierre de sesión falla por transporte, `AuthRepository`
llama a `clearSession()` y lo informa como éxito.

**Razonamiento.** El alcance del cierre ya era local: lo único que la petición al
servidor añade es revocar el token de refresco de este dispositivo, y ese token
se descarta igual al limpiar el almacenamiento. Devolver un error y dejar la
sesión abierta habría sido fiel a la biblioteca y falso frente al usuario, que
pulsó «Cerrar sesión» y vería su cuenta al volver.

**Consecuencia.** Un fallo de transporte al cerrar sesión no se le muestra a
nadie. Cualquier otra excepción sí llega como `AuthError.Unexpected` y la
pantalla la muestra con su reintento.

---

## 2026-09-12 · El número de teléfono se guarda con su código de país

**Contexto.** RF-02.1 pide registrar un teléfono. En Bolivia se escribe de ocho
dígitos y nadie antepone el código de país al dictarlo.

**Decisión.** `PhoneNumber` acepta las dos formas al escribir y guarda siempre la
forma cualificada, `+591` seguido de los ocho dígitos.

**Razonamiento.** Un número de ocho dígitos sin código de país es ambiguo en
cuanto exista un segundo país, y reescribir filas guardadas entonces es peor que
cualificarlas ahora. El costo hoy es una constante en un objeto de valor.

**Consecuencia.** El código del país solo se retira cuando lo que queda es un
número nacional completo, para que `59112345` —ocho dígitos, número válido— no
se mutile hasta quedar inválido. Ese caso tiene su prueba. Cuando el producto
alcance otro país, el cambio es sustituir la constante por el país de la
dirección, no migrar los datos.

---

## 2026-09-13 · El tiempo de espera agotado se reconoce aparte de las demás fallas de transporte

**Contexto.** `AuthRepository` traducía las excepciones de supabase-kt a tipos de
`AuthError` atrapando `HttpRequestException`, que es la excepción con que la
biblioteca envuelve las fallas de red. La revisión del pull request #2 llevó a
leer su fuente: `KtorSupabaseHttpClient` **relanza `HttpRequestTimeoutException`
sin envolverla** y solo envuelve las demás. Las dos son hermanas —ambas heredan
de `IOException`—, así que una jamás alcanza al `catch` de la otra.

**Decisión.** El mapeo de excepción a `AuthError` vive en
`data/mapper/AuthErrorMapper.kt` y reconoce las dos formas por separado.

**Razonamiento.** Con el `catch` anterior, un tiempo de espera agotado caía en la
rama genérica y salía como `AuthError.Unexpected`. Eso tenía dos costos, y el
segundo es el grave. El visible: con una conexión lenta —el caso corriente en el
terreno— la persona leía «Ocurrió un error inesperado» en lugar de «No hay
conexión», justo la vaguedad que `.claude/rules/compose.md` prohíbe. El
invisible: el respaldo del cierre de sesión sin conexión, decidido el
2026-09-12, vive en la rama de red, de modo que la sesión guardada **no se
limpiaba** cuando la falta de conexión se manifestaba como tiempo agotado en vez
de como rechazo inmediato. La conducta que este mismo registro prometía no se
cumplía en la mitad de los casos.

**Consecuencia.** El mapeo se extrajo a una función `internal` en lugar de vivir
dentro de los `catch` del repositorio, precisamente para que tenga prueba propia:
`AuthErrorMapperTest`. Se comprobó que la prueba tiene dientes revirtiendo el
mapeo al anterior y viéndola fallar. La rama de `RestException` queda sin prueba
porque construir una exige un `HttpResponse` de Ktor, y fabricarlo pediría una
dependencia de prueba nueva para un solo caso; se anota como deuda menor.

---

## 2026-09-13 · Cada transición del grafo elimina el destino que deja, no el de arranque

**Contexto.** El grafo de navegación de HU-01 reemplazaba la pila con
`popUpTo(graph.startDestinationId) { inclusive = true }`. Copilot lo señaló en el
pull request #2 y el fuente de Navigation 2.10.1 lo confirma: cuando `popUpTo`
apunta a un destino que ya no está en la pila, `NavControllerImpl` **ignora el
pop entero** —«Better to ignore the popBackStack than accidentally popping the
entire stack»—.

**Decisión.** `replaceCurrentWith` elimina el destino que la transición abandona,
leído de `currentDestination`, en vez del destino de arranque.

**Razonamiento.** La primera transición, de arranque a bienvenida, eliminaba el
destino de arranque; a partir de ahí ninguna otra encontraba su objetivo y la
pila crecía. Ingresar dejaba la bienvenida debajo de la cuenta, y cerrar sesión
dejaba la cuenta debajo de la bienvenida, una entrada más por cada ciclo. El
retroceso desde «Mi cuenta» llevaba a la bienvenida en lugar de salir de la
aplicación.

**Razonamiento sobre lo que el revisor exageró.** Copilot afirmó que el
retroceso podía «revelar datos de la cuenta tras cerrar sesión». No podía:
`AccountViewModel` conserva `SignedOut` como último valor emitido y `stateIn` lo
reproduce al volver, así que la pantalla mostraba el indicador de carga y
rebotaba sola. El defecto era real; el daño que describía, no. Se corrigió por
el defecto, no por el daño.

**Consecuencia.** La corrección no tiene prueba automática: fijarla exige una
prueba instrumentada con `TestNavHostController`, y el proyecto todavía no tiene
nada bajo `androidTest`. Queda anotada como deuda en `plan.md`, HU-01.

---

## 2026-09-13 · El estado de fallo del cierre de sesión ofrece dos salidas, no una

**Contexto.** El botón del estado de fallo de «Mi cuenta» decía «Reintentar» y
solo descartaba el error, sin volver a intentar nada. Lo señaló Copilot en el
pull request #2 y contradice `.claude/rules/compose.md`: «Los controles dicen
exactamente qué ocurre al pulsarlos».

**Decisión.** «Reintentar» vuelve a ejecutar el cierre de sesión, y se agrega un
control secundario, «Seguir con la sesión abierta», que descarta el error.

**Razonamiento.** Cablear «Reintentar» al cierre de sesión y nada más habría
dejado a la persona encerrada en la pantalla de error mientras el fallo
persistiera, sin forma de volver a su cuenta. Dos acciones distintas necesitan
dos controles, y cada etiqueta dice exactamente lo que hace.

**Consecuencia.** `AccountViewModel` y `SignOutUseCase` pasaron de no tener
ninguna prueba a tener `AccountViewModelTest`, que fija el contrato en que se
apoya el botón: pedir otra vez alcanza al repositorio una segunda vez, y el
fallo solo se borra cuando la operación tiene éxito. Esa prueba **no** atrapa el
cableado del botón en sí, que es lo que estaba roto; para eso hace falta una
prueba de Compose sobre `AccountContent`. Queda anotado como deuda.

---

## 2026-09-13 · La elección del rol se escribe con una función almacenada — SUPERADA EN PARTE

> **Superada en parte por la entrada del 2026-10-01 sobre el rol múltiple.** Sigue
> vigente lo esencial: la escritura del rol es una transacción atómica en una
> función `security invoker`, por el motivo que esta entrada explica. Lo que deja de
> valer es «el rol se elige una vez»: `assign_my_role` pasa a ser `add_my_role`, el
> disparador `profiles_guard_role` desaparece, y la regla triplicada que el párrafo
> de consecuencia describe pasa a ser «un rol no se agrega dos veces» en lugar de
> «el rol se elige una vez».

**Contexto.** HU-02 tiene que escribir dos cosas: el rol en `profiles` y la fila
de `patients` o de `professionals`. Desde el cliente son dos peticiones, y
`patients_insert_own` exige que el rol ya esté escrito, así que además tienen un
orden obligatorio.

**Decisión.** Una función almacenada, `assign_my_role(p_role user_role)`, hace
las dos escrituras en una sola transacción. Es `security invoker`.

**Razonamiento.** Si la segunda escritura fallara, la persona quedaría con un
rol sin la fila que lo sostiene, la aplicación la enviaría a la pantalla de un
rol cuyo registro no existe, y nada volvería a intentarlo: la pregunta no se
repite porque el rol ya está puesto. Es el primero de los tres casos que
`.claude/rules/supabase.md` reserva para una función de servidor, la transacción
atómica que el cliente no puede garantizar, y el mismo motivo por el que
`.claude/rules/testing.md` exige `failedAcceptanceLeavesNoPartialRecord` para la
aceptación de una oferta.

La función es `security invoker` y no `security definer` porque no necesita
saltarse ninguna política: cada una de sus escrituras ya la permite la política
del propio usuario. Aporta atomicidad y nada más. Una función definidora que no
la necesita es un agujero sin razón de existir.

**Consecuencia.** `ProfileRepository` hace una sola llamada y tiene una sola
superficie de error. La regla que RF-01.4 y RF-01.5 expresan —el rol se elige
una vez y nunca es `ADMIN`— queda escrita tres veces, y a propósito: en el caso
de uso, en la función, y en el disparador `profiles_guard_role` que ya existía.
La del caso de uso evita la petición inútil; las otras dos son la garantía.

---

## 2026-09-13 · El tipo y la tarifa del profesional pasan a ser nulos

**Contexto.** `professionals.professional_type` y `professionals.base_rate_bob`
se crearon `not null`, y `base_rate_bob` además con `check (> 0)`, de modo que
no admiten un valor de relleno. El esquema daba por supuesto que la fila del
profesional nace completa. El plan del proyecto dice otra cosa: RF-01.4 pide el
rol antes que cualquier otra funcionalidad, en HU-02, y RF-02.2 pone el tipo y
la tarifa en el perfil profesional, que HU-04 recoge en el sprint siguiente.

**Decisión.** Ambas columnas admiten nulo, y la restricción
`professionals_approved_profile_is_complete` impide que un profesional sin tipo
o sin tarifa alcance `verification_status = 'APPROVED'`.

**Razonamiento.** La alternativa era inventar un tipo y una tarifa en nombre de
la persona al elegir el rol, o pedirle en HU-02 datos que la historia no pide y
que HU-04 va a volver a pedirle. Ninguna de las dos es aceptable: la primera
escribe un dato que nadie declaró, y la segunda adelanta alcance de otro sprint.

Aflojar una columna abre un hueco, así que se cierra en la misma migración. La
restricción no es una comodidad: `professional_directory` y
`search_nearby_professionals` filtran por `APPROVED`, de modo que amarrar la
completitud a ese estado deja INV-07 sostenido por el motor y no por la
disciplina de quien programe la pantalla de verificación.

**Consecuencia.** La historia de verificación del Sprint 3 no necesita
comprobar en el cliente que el perfil esté completo antes de aprobar: si lo
intenta con datos faltantes, la base de datos lo rechaza. Un profesional
incompleto existe, pero es invisible para todo el producto.

---

## 2026-09-13 · El rol que se elige es un tipo distinto del rol que se tiene

> **Vigente, con la premisa de su contexto cambiada el 2026-10-01.** `profiles.role`
> ya no existe: los roles de una persona viven en `profile_roles` y el activo en
> `profiles.active_role`. La decisión se sostiene entera de todos modos, y el rol
> múltiple la refuerza: `AssignableRole` es ahora lo que recibe `AddRoleUseCase`, y
> es el único lugar donde habría que agregar `ADMIN` para que llegue a ofrecerse.
> `adminRoleIsNeverSelfAssignable` sigue vigilando ese archivo sin un cambio.

**Contexto.** `profiles.role` admite tres valores —`PATIENT`, `PROFESSIONAL` y
`ADMIN`—, pero RF-01.5 dice que el administrador nunca se autoasigna, y el
criterio de aceptación de HU-02 lo dice como interfaz: «el rol de administrador
nunca aparece como opción».

**Decisión.** `UserRole` tiene los tres valores, porque un perfil puede tener
cualquiera de ellos. `AssignableRole` tiene dos, y es el tipo que recibe
`ChooseRoleUseCase` y con el que la pantalla construye sus opciones.

**Razonamiento.** Con un solo enumerado, no ofrecer `ADMIN` es una condición que
alguien escribe en la pantalla y que otro puede borrar sin que nada se queje.
Con dos tipos, ofrecerlo exige agregarlo a `AssignableRole`, que es un cambio
deliberado y visible en la revisión. La prueba `adminRoleIsNeverSelfAssignable`
vigila exactamente ese archivo.

**Consecuencia.** La pantalla recorre `AssignableRole.entries` en vez de filtrar
una lista, así que agregar un rol elegible en el futuro es agregar una constante
y sus dos cadenas, sin tocar la pantalla.

---

## 2026-09-13 · El arranque resuelve sesión y rol juntos, y el ingreso vuelve al arranque — SUPERADA EN PARTE

> **Superada en parte por la entrada del 2026-10-01 sobre el rol múltiple.** Sigue
> vigente todo el razonamiento: un solo lugar decide el destino, el ingreso vuelve
> al arranque, y un rol que no se puede leer no es un rol que falta. Lo que cambia
> es el dato que el arranque lee: en vez de un rol único lee el conjunto de roles y
> el rol activo, y manda a elegir cuando el conjunto está vacío. Los cinco caminos
> de `StartupViewModelTest` se conservan.

**Contexto.** RF-01.4 exige que quien no tiene rol lo elija antes de acceder a
cualquier otra funcionalidad. Hasta HU-01 el arranque solo miraba la sesión y
enviaba a la bienvenida o a la pantalla principal.

**Decisión.** `StartupViewModel` resuelve sesión y rol y devuelve un único
destino: bienvenida, elección de rol, pantalla principal, o un error con
«Reintentar». `WelcomeScreen`, al terminar el ingreso, navega de vuelta al
arranque en lugar de a la pantalla principal.

**Razonamiento.** Quien acaba de ingresar puede ser alguien que entra por
primera vez o alguien que ya eligió su rol hace meses, y la bienvenida no tiene
forma de distinguirlos sin repetir la consulta que el arranque ya sabe hacer.
Repartir la decisión en dos pantallas es como se desincronizan: basta con que
una de las dos deje de mirar el rol para que RF-01.4 se rompa en ese camino y no
en el otro. Volver al arranque cuesta un indicador de carga y deja un solo lugar
que decide.

**Razonamiento sobre el error.** Un rol que no se puede leer no es un rol que
falta. Si la consulta falla por red, suponer «no hay rol» pondría a alguien que
ya eligió frente a la pregunta otra vez, y la base de datos rechazaría su
respuesta con `role_already_assigned`. El arranque se detiene y ofrece
reintentar.

**Consecuencia.** La pantalla de arranque deja de ser solo un indicador de carga
y pasa a tener estado de error, que es también lo que la Definición de Terminado
pide para cualquier pantalla que cargue datos. `StartupViewModelTest` fija los
cinco caminos.

---

## 2026-09-13 · La fotografía se muestra; reemplazarla espera a que exista Storage

**Contexto.** RF-02.1 incluye la fotografía entre lo que el paciente registra y
edita, y el primer criterio de HU-03 pide ver «la fotografía que trajo Google».
El disparador `handle_new_user()` ya guarda esa dirección en
`profiles.photo_url` desde el primer ingreso, de modo que el dato existe. Lo que
no existe es Storage: ninguna migración ha creado un contenedor, y
`docs/decisions.md` del 2026-09-12 dejó Coil sin motor de red porque nada hacía
todavía una petición de imagen real.

**Decisión.** HU-03 dibuja la fotografía que trajo Google y no permite
reemplazarla. Se agrega `coil-network-ktor3`, que reutiliza el cliente Ktor que
supabase-kt ya trae. Reemplazarla entra con RF-04.1, en el Sprint 3, que es la
historia que obliga a que Storage exista de verdad.

**Razonamiento.** Permitir reemplazarla convertiría a HU-03 en la primera
historia con Storage, y eso no es un campo más: es un contenedor con sus
políticas sobre `storage.objects`, un selector de imágenes, compresión en el
cliente —que `.claude/rules/compose.md` exige— y una decisión sobre URL firmada
frente a URL pública que la regla de Supabase solo contesta para los documentos
de verificación. Esa decisión condiciona también la ficha pública del
profesional del Sprint 4, donde una lista de resultados necesitaría una URL
firmada por cada tarjeta. Tomarla de paso, dentro de una historia de formulario,
es como se elige mal.

**Consecuencia.** El motor de red de Coil deja de estar pendiente: la aplicación
hace ahora una petición real a `googleusercontent.com`, que figura en
`docs/architecture/components.md`. Queda anotado en `plan.md`, HU-03, que el
criterio de reemplazar la fotografía se cierra con la historia de verificación.

---

## 2026-09-13 · El perfil se lee tal como está guardado; el objeto de valor cuida la escritura

**Contexto.** `PersonName` exige entre 2 y 80 caracteres y rechaza dígitos, pero
`handle_new_user()` copia en `full_name` lo que entrega Google sin pasar por el
objeto de valor. Un perfil puede sostener legítimamente un nombre que el objeto
de valor rechazaría.

**Decisión.** `UserProfile.fullName` es la cadena guardada, sin validar. La
validación ocurre al escribir: `SaveProfileUseCase` construye `PersonName`,
`PhoneNumber` y `BirthDate`, y el repositorio recibe un `ProfileUpdate` que solo
contiene objetos de valor.

**Razonamiento.** Leer el nombre a través del objeto de valor lo habría
convertido en nulo, la pantalla habría mostrado un campo vacío y la persona
habría sobrescrito su nombre real con lo que recordara en ese momento. Perder el
dato al leer es distinto de rechazarlo al escribir: HU-01 aceptó perderlo en
`AuthSession` porque ahí el dato era decorativo y la sesión era lo que
importaba; aquí el dato **es** la pantalla.

La regla no se relaja: ningún valor entra al sistema sin pasar por su objeto de
valor. Lo que cambia es que la puerta está en la escritura, que es la única por
la que el cliente puede escribir, y no en la lectura, que refleja filas que el
disparador escribió antes de que la regla existiera.

**Consecuencia.** `ProfileMapperTest` fija que un nombre con un dígito llega
entero, y que un rol desconocido o una fecha ilegible cuestan ese campo y nunca
el perfil completo: la persona tiene que poder abrir la pantalla y corregirlo.

---

## 2026-09-13 · El nombre sale de «Mi cuenta» y vive en «Mi perfil»

**Contexto.** «Mi cuenta» mostraba el nombre y el correo que venían de la sesión
de Supabase, es decir, de los metadatos de Google. HU-03 hace editable el nombre
en `profiles.full_name`. Desde el momento en que alguien lo edita, las dos
pantallas mostrarían nombres distintos.

**Decisión.** «Mi cuenta» deja de mostrar el nombre y conserva el correo. El
nombre pasa a «Mi perfil», que es donde se edita y de donde sale la fila de
`profiles`.

**Razonamiento.** El correo identifica la cuenta de Google con la que se
ingresó, no se edita en ninguna parte y por lo tanto no puede divergir. El
nombre sí. Mantenerlo en dos pantallas con dos orígenes distintos habría hecho
que alguien editara su nombre, volviera a «Mi cuenta», viera el anterior y
concluyera que no se guardó. La alternativa —que «Mi cuenta» también leyera
`profiles`— obligaba a recargarla al volver de «Mi perfil», que es mecanismo
para sostener una duplicación que no hace falta.

**Consecuencia.** Nada mutable queda duplicado entre las dos pantallas. «Mi
cuenta» es la sesión: correo, rol, cerrar sesión. «Mi perfil» es lo que la
aplicación guarda de la persona. La clave `auth_account_name_unavailable` se
eliminó de ambos idiomas al quedar sin uso.

---

## 2026-09-13 · La fecha de nacimiento se elige con el selector de Material 3

**Contexto.** `patients.birth_date` es una fecha con la restricción
`birth_date < current_date`. Había dos formas de pedirla: un campo de texto con
un formato fijo que el objeto de valor analiza, o el selector de fecha de
Material 3.

**Decisión.** El selector de Material 3, con `selectableDates` limitado a fechas
anteriores a hoy. Obliga a `@OptIn(ExperimentalMaterial3Api::class)`.

**Razonamiento.** Un campo de texto fija un formato en el código —`dd/MM/aaaa`—
que ningún traductor puede cambiar, y contradice la regla de
`.claude/rules/i18n.md` de que las fechas se formatean con las utilidades de la
plataforma según la configuración regional. El selector es regional por
construcción, permite escribir la fecha además de navegarla, y expresa la
restricción de la columna en el propio control en vez de dejarla para el mensaje
de error.

**Sobre la anotación.** La regla del proyecto prohíbe dependencias en versión
`alpha`, `beta`, `rc` o `SNAPSHOT`; Material 3 está en versión estable y lo
experimental es la marca de una API dentro de ella. Se acepta de forma
deliberada y acotada a este control. Si esa API cambiara en una versión
posterior, el cambio queda contenido en `BirthDateField`.

**Consecuencia.** La restricción de la columna queda expresada dos veces, y en
capas distintas: el selector impide elegir una fecha futura y
`BirthDate.create` la rechaza igual. `BirthDateTest` fija los límites exactos,
incluido hoy mismo, que es el borde que la columna rechaza.

---

## 2026-09-14 · Guardar el perfil también es una sola transacción — SUPERADA EN PARTE

> **Superada en parte por la entrada del 2026-10-01 sobre el rol múltiple.** Sigue
> vigente que `save_my_profile` es una transacción única y que el servidor —no el
> cliente— decide qué columnas se escriben, que es lo que hace que un argumento
> enviado por error se ignore en vez de escribirse. Lo que cambia es el dato con el
> que decide: en vez de `profiles.role` lee `profiles.active_role`, de modo que
> alguien con los dos roles edita el rol en el que está.

**Contexto.** `saveProfile` enviaba dos peticiones: una a `profiles` y otra a
`patients`. Lo señaló el revisor automático en el pull request #3. Una conexión
que se cortara entre las dos dejaba el nombre y el teléfono guardados, las
columnas del paciente sin guardar, y la pantalla informando que el guardado había
fallado.

**Decisión.** La migración `20260914043306_save_profile_atomically` agrega
`save_my_profile`, que hace las dos escrituras en una transacción. El cliente
hace una sola llamada. La función lee el rol del servidor en vez de recibirlo del
cliente.

**Razonamiento.** Es el mismo remedio y el mismo motivo que la entrada del
2026-09-13 sobre `assign_my_role`, y no haberlo visto aquí una historia después
fue una incoherencia, no un matiz: el proyecto había construido una función
almacenada por este problema exacto y luego escribió el caso siguiente con dos
peticiones.

El daño era menor que en la elección del rol, y conviene decirlo con precisión
porque el revisor lo describió peor de lo que era: este guardado es una
sobrescritura completa de las dos filas, así que **reintentar lo repara**,
mientras que un rol a medio asignar no se reparaba nunca. El problema real es
quien no reintenta, porque la pantalla le dijo que había fallado y en el servidor
quedó la mitad escrita.

Que el rol lo lea el servidor no es un detalle de implementación: un profesional
no tiene fila en `patients` y esas columnas no son suyas. Decidir por el rol
guardado significa que un argumento enviado por error se ignora en vez de
escribirse, que es más fuerte que confiar en que el cliente mande nulos.

**Consecuencia.** El guardado pasó de tres peticiones a dos —la función y la
relectura que redibuja la pantalla—. Queda una ventana menor: si la relectura
falla después de una escritura correcta, la pantalla informa un fallo que no
ocurrió. Se repara sola al reintentar, porque la escritura es idempotente, y se
deja así a propósito antes que devolver la fila desde la función y duplicar el
transformador.

---

## 2026-09-14 · La ruta no lleva tipos de dominio, y el rol se muestra donde ya se carga

**Contexto.** `AccountRoute` llevaba `val role: UserRole` para que «Mi cuenta»
pudiera mostrar el rol sin una consulta más. El revisor del pull request #3
señaló que `.claude/rules/compose.md` exige que los argumentos de ruta sean
identificadores y nunca objetos serializados, y que así el formato de la pila de
retroceso quedaba atado al enumerado del dominio.

**Decisión.** `AccountRoute` vuelve a ser un `data object`, igual que
`StartupDestination.Home`, y la etiqueta del rol se muestra en «Mi perfil».

**Razonamiento.** La corrección obvia era pasar `role.name` como cadena y
convertirla en el límite de la navegación, pero eso deja una conversión que puede
fallar dentro de la capa de presentación y un rol anulable que la pantalla tiene
que contemplar. Mover la etiqueta sale más barato y además corrige algo que
estaba mal colocado: «Mi perfil» ya carga el perfil entero, de modo que ya conoce
el rol sin pedir nada, y el rol vive en `profiles`, que es lo que esa pantalla
muestra.

Es la continuación de la entrada del 2026-09-13 sobre el nombre. La línea es la
misma: «Mi cuenta» es la sesión de Google —el correo, cerrar sesión— y «Mi
perfil» es lo que la aplicación guarda de la persona. El rol estaba del lado
equivocado de esa línea y el revisor lo encontró por otro camino.

**Consecuencia.** `navigation/` deja de importar `features/profile/domain/`.
Cuando el Sprint 4 introduzca pantallas principales distintas por rol,
`StartupDestination.Home` volverá a necesitar el dato; entonces se agrega, con
una pantalla que lo consuma de verdad.

---

## 2026-09-14 · La duración del sprint se corrige a una semana con el dato medido

> Supera en parte la entrada del 2026-09-09, que declaraba dos semanas.

**Contexto.** El marco declaraba sprints de dos semanas. Al registrar en la
retrospectiva las fechas reales del Sprint 1 —del 11 al 14 de septiembre de
2026— resultó que había durado cuatro días. La declaración y la práctica no
coincidían.

**Decisión.** La duración declarada pasa a **una semana**. El rango de capacidad
de veinte a veinticinco puntos se conserva, porque los 21 puntos medidos caen
dentro de él; lo que cambia es la unidad a la que se refiere.

**Razonamiento.** SCRUM pide que la duración del sprint sea fija y conocida, y un
marco declarado que nadie sigue es más difícil de defender que una duración corta
bien registrada. Corregir la declaración para que coincida con lo que se hace es
además el propio mecanismo de inspección y adaptación: el dato apareció al medir,
y la planificación se ajustó con él.

**Razonamiento sobre lo que el 21 no dice.** El Sprint 1 terminó cuando se agotó
el alcance, no cuando se agotó el plazo, de modo que su velocidad no mide la
capacidad: mide lo que se planificó. Es un piso, no un techo. Por eso el traslado
de HU-06 al Sprint 3 se apoya sobre todo en la otra razón registrada en la
retrospectiva —HU-05 vale 13 puntos e introduce mapa, permisos de ubicación y
geocodificación, ninguno tocado antes— y no sobre la comparación de 21 contra 24.

**Consecuencia.** El Sprint 2 será la primera medición que diga algo sobre la
capacidad, porque se cerrará por tiempo y no por alcance. Hasta entonces el rango
de veinte a veinticinco sigue siendo una estimación, no un valor medido.

## 2026-09-14 · El perfil profesional entra en `save_my_profile`, y la firma anterior se elimina — SUPERADA EN PARTE

> **Superada en parte por la entrada del 2026-10-01 sobre el rol múltiple.** La
> firma de once argumentos y el motivo de no partirla en dos funciones siguen
> vigentes. Lo que cambia es la rama: el `if v_role = 'PATIENT' ... elsif` pasa a
> leer `active_role`. La advertencia sobre «function is not unique» sigue siendo la
> razón por la que la firma se reemplaza en el sitio y no se agrega otra al lado.

**Contexto.** HU-04 agrega seis columnas de `professionals` al formulario de «Mi
perfil». Escribirlas con una segunda petición reproduce exactamente el defecto
que encontró la revisión del pull request 3: una conexión que se corta entre las
dos peticiones deja media persona guardada mientras la pantalla informa que el
guardado falló.

**Decisión.** `save_my_profile` recibe los argumentos del profesional y, según el
rol que lee del servidor, escribe `patients` o `professionals` dentro de la misma
transacción. La firma anterior de cinco argumentos se elimina en la misma
migración.

**Razonamiento.** Una función aparte para el profesional volvería a ser dos
peticiones. Dejar además la firma vieja al lado de la nueva no produce una
sobrecarga sino una ambigüedad: PostgREST resuelve una función almacenada por los
argumentos que recibe, y una llamada con los cinco originales encajaría en las
dos. PostgreSQL responde a eso con «function is not unique», no eligiendo una.

`available_now` queda deliberadamente fuera de la función. Es la decisión de la
entrada siguiente.

**Consecuencia.** La lista de argumentos crece a once y seguirá creciendo si el
perfil gana campos. Es el precio de que guardar el perfil sea una sola operación
atómica, y se paga una vez por historia. Si algún día el perfil se parte en
varias pantallas, cada una tendrá su propia función, no su propia petición suelta.

---

## 2026-09-14 · La disponibilidad inmediata se escribe sola, fuera del formulario

**Contexto.** RF-02.5 pide que el profesional indique si está disponible para
atención inmediata **en este momento**, y el criterio de HU-04 dice que al cambiar
el interruptor el estado se refleje de inmediato. El resto de la pantalla es un
formulario que se confirma con «Guardar cambios».

**Decisión.** El interruptor escribe `professionals.available_now` por su cuenta,
con una actualización directa que permite `professionals_update_own`, y no viaja
dentro de `save_my_profile`. La pantalla muestra el valor nuevo antes de que el
servidor conteste y lo devuelve a su lugar, con un mensaje, si la escritura falla.

**Razonamiento.** Son dos cosas distintas. El formulario declara lo que el
profesional ofrece; el interruptor declara si está disponible ahora, y obligarlo a
pulsar «Guardar cambios» para dejar de recibir trabajo es exactamente el momento en
que no va a hacerlo. Llevarlo dentro del guardado tendría además un efecto peor:
un formulario enviado desde una pantalla que lleva minutos abierta reescribiría la
disponibilidad con el valor que tenía cuando se abrió, deshaciendo un cambio hecho
segundos antes.

Mostrarlo antes de la confirmación del servidor no es una comodidad: un
interruptor que tarda en moverse se lee como un interruptor que no funciona, y la
persona lo vuelve a pulsar. Lo que no es aceptable es dejarlo encendido si la
escritura falló, porque le diría que está recibiendo trabajo cuando el servidor
sostiene lo contrario; por eso el fallo lo devuelve a su valor anterior y explica
qué pasó.

**Consecuencia.** `.claude/rules/supabase.md` manda resolver con una consulta del
cliente lo que las políticas ya permiten, y esta lo es: una columna, una fila
propia, sin nada que coordinar. No hace falta una función almacenada.

---

## 2026-09-14 · `AmountBob` nace en `core/vo/` y `CoverageRadiusKm` en la característica

**Contexto.** HU-04 introduce los dos objetos de valor a la vez, y hoy los usa
una sola característica: el perfil. La regla del 2026-09-12 dice que lo
compartido vive en `core/vo/` y lo propio en el `domain/vo/` de su característica.

**Decisión.** `AmountBob` va a `core/vo/`. `CoverageRadiusKm` va a
`features/profile/domain/vo/`, junto a `BirthDate`.

**Razonamiento.** El dinero no es del perfil. `request_offers.amount_bob`,
`services.final_amount_bob` y las tres columnas de `payments` son el mismo
concepto, y cuando lleguen sus características —Sprint 6 y Sprint 8— ninguna
debería importar el dominio del perfil para obtener un monto. Es el caso que la
entrada del 2026-09-12 describe: colocarlo en una característica la convierte en
dueña de algo que no le pertenece.

El radio de cobertura sí es del profesional. RF-06.1 habla de un radio de
búsqueda configurable, pero ese es el radio que elige el paciente al buscar, no el
que declara el profesional: coinciden en la unidad y no en el concepto. Adelantar
una abstracción común a los dos sería suponer que van a compartir reglas, y todavía
no hay una segunda regla que mirar.

**Consecuencia.** Si HU-11 descubre que el radio de búsqueda tiene exactamente los
mismos límites y el mismo comportamiento, moverlo a `core/vo/` será un cambio de
paquete. Inventar hoy la abstracción y descubrir que no sirven igual costaría más.

---

## 2026-09-14 · Un monto viaja como el texto exacto del número, no como número de JSON

**Contexto.** `base_rate_bob` es `numeric(10,2)` y llega desde PostgREST como un
número de JSON. Al escribirlo, la forma evidente era construir el elemento JSON a
partir del `BigDecimal` y dejar que kotlinx lo serializara.

**Decisión.** `BigDecimalSerializer` lee el literal textual del número que llega y
escribe `toPlainString()` como cadena de JSON. El servidor convierte esa cadena al
tipo del argumento, que es el mismo camino que ya recorre la fecha de nacimiento
para llegar a un argumento `date`.

**Razonamiento.** Se comprobó con una prueba antes de confiar en la forma
evidente, y falló: kotlinx serializa un `JsonPrimitive` numérico intentando primero
`Long` y después `Double`, de modo que `120.00` sale como `120.0`. Con dos
decimales y diez dígitos el valor sobrevive, así que el defecto es inofensivo hoy
y es el hábito equivocado para el camino que van a reutilizar las ofertas y los
pagos. El literal sin comillas que lo evitaría es una API experimental, y
`CLAUDE.md` no admite depender de una.

Al leer ocurre lo simétrico: decodificar como `Double` y construir el `BigDecimal`
desde ahí pierde la escala guardada, y la pantalla mostraría `120.0` para una
columna que contiene `120.00`.

**Consecuencia.** Toda columna monetaria del proyecto usa este serializador en
ambas direcciones. `ProfileDtoTest` fija las dos mitades del contrato: qué forma
tiene lo que entra y qué forma tiene lo que sale.

---

## 2026-09-14 · El perfil público lee la vista, y su estado vacío es INV-07 a la vista

**Contexto.** RF-02.6 pide que el paciente consulte el perfil público del
profesional, y el criterio 4 de HU-04 pide ver publicado lo que se guardó. La
proyección pública ya existe desde HT-04: la vista `professional_directory`, que
filtra `verification_status = 'APPROVED'` y `profiles.active` y no contiene
ninguna columna de contacto.

**Decisión.** La pantalla lee únicamente esa vista. Cuando no devuelve fila, no
muestra un error: muestra un estado que dice que el perfil todavía no está
publicado y por qué.

**Razonamiento.** Leer la tabla `professionals` habría mostrado el perfil siempre,
y habría enseñado al profesional una ficha que ningún paciente puede ver. La vista
es lo que el paciente lee, así que es lo que la pantalla tiene que leer para que
la vista previa signifique algo.

Que la vista no devuelva nada no es una falla de lectura. Es INV-07 funcionando:
un profesional sin verificación aprobada no existe para nadie más. Presentarlo
como error mandaría a la persona a revisar su conexión sobre una conexión que
funciona. La vista se otorga a `authenticated` como un todo y no filtra por quien
consulta, de modo que lo que ve el profesional en su vista previa es fila por fila
lo que verá un paciente.

**Consecuencia.** La misma pantalla sirve a HU-12 en el Sprint 4, cuando la
búsqueda le dé al paciente un identificador con el que llegar. Mientras tanto, el
estado con contenido solo se puede ver en un dispositivo si existe un profesional
aprobado, y aprobar es trabajo del administrador, que llega en el Sprint 3 (HU-09).
Los servicios declarados y los comentarios que RF-02.6 también menciona esperan a
HU-10 y al Sprint 9.

---

## 2026-09-15 · La dirección se escribe con una consulta del cliente, no con una función almacenada

**Contexto.** HU-03 y HU-04 guardan el perfil llamando a `save_my_profile`, y lo
natural era continuar el patrón con un `save_my_address`. La dirección, además,
tiene una trampa conocida: PostGIS construye el punto con longitud primero y
latitud después, y `.claude/rules/supabase.md` advierte que invertirlas no produce
ningún error.

**Decisión.** La dirección se inserta y se corrige con las consultas del cliente
que PostgREST ya expone sobre `addresses`, protegidas por las políticas que HT-04
creó. No se agrega función almacenada.

**Razonamiento.** `.claude/rules/supabase.md` reserva las funciones de servidor
para tres casos: transacciones atómicas que el cliente no puede garantizar,
operaciones privilegiadas y recepción de notificaciones externas. Guardar una
dirección es una sola fila: no hay nada que coordinar. Y la regla cierra con «si
algo puede resolverse con una consulta del cliente respetando las políticas, se
resuelve así; no se crea una función de servidor por comodidad».

Quedaban dos argumentos a favor de la función y ninguno resistió. El primero era
la regla de que la primera dirección sea la principal, que parecía exigir leer
antes de escribir dentro de la misma transacción; se resolvió mejor con un
disparador, porque así vale para toda fila que llegue a la tabla y no solo para
las que pasan por este camino. El segundo era contener el orden de las
coordenadas; se resolvió concentrando la construcción del punto en una única
función de transformación, `Coordinate.toEwkt()`, con una prueba que falla si
alguien invierte el par. La prueba es mejor defensa que la función: una función
también puede recibir los argumentos cambiados.

**Consecuencia.** HU-06, que agrega elegir entre varias direcciones y eliminarlas,
sigue el mismo camino y no hereda una función que mantener. La comprobación de que
PostgREST acepta el punto se hizo antes de escribir el cliente, consultando
`json_populate_record` sobre el tipo de la tabla, que es exactamente la ruta que
PostgREST usa para convertir el JSON en fila.

---

## 2026-09-15 · El punto viaja como texto y se lee desde una vista

**Contexto.** `addresses.location` es `geography(Point, 4326)`. No hay forma de
JSON para un valor de PostGIS: al escribir hay que darle algo que su función de
entrada sepa leer, y al leer PostgREST devuelve la codificación hexadecimal que
PostGIS guarda en disco, del estilo `0101000020E6100000…`.

**Decisión.** Al escribir, el punto sale como el texto `SRID=4326;POINT(lon lat)`.
Al leer, la aplicación no consulta la tabla sino la vista `my_addresses`, que
proyecta el mismo punto como `latitude` y `longitude`.

**Razonamiento.** La alternativa al leer era decodificar el hexadecimal en el
cliente, que es escribir un lector de un formato binario ajeno para obtener dos
números que la base ya sabe calcular. La vista cuesta diez líneas de SQL y deja al
cliente sin ninguna noción de PostGIS.

La vista se declara `security_invoker = true`, al revés que
`professional_directory`. Las dos decisiones son coherentes con lo que cada vista
existe para hacer: el directorio muestra filas ajenas y por eso ignora las
políticas de quien consulta; `my_addresses` muestra las filas propias, así que
`addresses_select_own` tiene que seguir decidiendo (INV-13). Se comprobó con `set
local role authenticated` y el identificador de cada perfil: cada uno ve solo su
dirección.

El texto del punto se escribe con siete decimales y configuración regional raíz.
Sin lo primero, una coordenada cercana al origen saldría en notación científica;
sin lo segundo, un teléfono configurado en español escribiría la coma decimal, y
PostGIS lee la coma como el separador entre dos puntos de una geometría. Ninguna
de las dos cosas produce un error legible.

**Consecuencia.** Toda lectura de direcciones pasa por la vista, incluida la de
HU-06. Si alguna vez hace falta otro dato geográfico en el cliente, se agrega a la
vista y no se decodifica nada.

---

## 2026-09-15 · La geocodificación la resuelve el geocodificador de la plataforma

**Contexto.** RF-03.3 pide convertir entre dirección escrita y coordenadas. El
proveedor de mapas vende ese servicio por llamada, y la entrada del 2026-09-05
sobre la búsqueda por cercanía ya dejó dicho que su servicio de proximidad se paga
a un precio que el proyecto no puede sostener.

**Decisión.** Se usa `android.location.Geocoder`, el geocodificador que trae
Android, en las dos direcciones. El proveedor de mapas se usa solo para dibujar.

**Razonamiento.** El geocodificador de la plataforma no cobra al proyecto por
llamada y es el que la propia regla de internacionalización implica al pedir que
los formatos se resuelvan con las utilidades de la plataforma. Su costo es otro: no
existe en un dispositivo sin los servicios de Google, y `Geocoder.isPresent()` lo
dice. Por eso la pantalla trata «este dispositivo no sabe nombrar puntos» como un
estado en el que se sigue trabajando escribiendo la dirección a mano, y no como un
fallo.

La interfaz cambió en API 33, que introdujo la variante con escucha y marcó como
obsoleta la que bloquea. Con `minSdk 26` hay que sostener las dos: la nueva se
anota con `@RequiresApi` para que el análisis estático vea que las llamadas están
protegidas, y la vieja corre fuera del hilo principal.

**Consecuencia.** RF-03.3 pide además no repetir la consulta. La copia duradera es
la columna `address_text`, que es lo que hace que reabrir una dirección guardada no
consulte nada; el repositorio guarda además en memoria lo ya preguntado, con la
coordenada redondeada a unos cinco decimales, porque dos puntos separados por un
metro son la misma puerta.

**Corrección del mismo día, encontrada en el emulador.** El geocodificador devuelve
vacío de vez en cuando sin error ninguno: el mismo punto que no supo nombrar
contestó «Ayacucho 166» minutos después. La pantalla trataba esa respuesta como si
no hubiera pasado nada, y el resultado era peor que un fallo declarado: el marcador
señalaba un sitio y la dirección escrita seguía describiendo el anterior, sin que
nadie lo advirtiera. Guardar así deja una fila cuya ubicación y cuyo texto no
coinciden, y es el texto lo que lee la contraparte para llegar.

Borrar la línea tampoco es la respuesta, porque en un dispositivo sin
geocodificador esa línea es la dirección entera y la escribió la persona. Así que
se conserva y se avisa. Vale igual para una respuesta que trae ciudad pero no
calle, que produce exactamente el mismo desajuste con otra forma.

---

## 2026-09-15 · La clave de Mapas viaja por el manifiesto

**Contexto.** RNF-06 manda que ningún secreto viva en el código fuente, y el
proyecto ya lee tres claves desde `local.properties` y las publica como campos de
`BuildConfig`. El SDK de Mapas no lee `BuildConfig`: busca un `meta-data` llamado
`com.google.android.geo.API_KEY` dentro del manifiesto.

**Decisión.** `MAPS_API_KEY` se lee de `local.properties` con la misma función
`secret()` que las demás, pero se entrega como marcador de posición del manifiesto
en vez de como campo de `BuildConfig`.

**Razonamiento.** Es la única forma de dársela al SDK sin escribirla en un archivo
versionado. La consecuencia de no ponerla es visible y no rompe la compilación: el
marcador queda vacío, la aplicación arranca, y el mapa dibuja una cuadrícula en
blanco mientras el registro dice `Authorization failure`. Eso es preferible a un
error de Gradle que impida compilar una copia limpia del repositorio, que es el
mismo criterio que ya se aplicó a las claves de Supabase.

**Consecuencia.** `README.md` suma la variable a la lista de claves que hay que
poner antes de compilar, y habilitar «Maps SDK for Android» en el proyecto de
Google Cloud pasa a ser un paso de puesta en marcha. Se comprobó que la clave que
trae `app/google-services.json` no sirve para esto: el SDK responde
`Authorization failure` porque esa clave no tiene habilitado ese servicio.

---

## 2026-09-15 · La primera dirección se decide bloqueando la fila del perfil

**Contexto.** El disparador `addresses_first_is_primary` comprueba si la persona
ya tiene alguna dirección y, si no, marca la que entra como principal. La
revisión del pull request #4 señaló que esa comprobación no está serializada:
dos inserciones simultáneas del mismo perfil ven las dos que no hay ninguna,
reclaman las dos el distintivo, y una pierde contra el índice único parcial
`idx_addresses_profile_primary`.

**Decisión.** El disparador bloquea la fila del perfil —`select 1 from
public.profiles where id = new.profile_id for update`— antes de comprobar.

**Razonamiento.** No se puede bloquear la fila que se quiere contar, porque
todavía no existe; hay que bloquear algo que sí existe y que identifique a la
misma persona, y el perfil es esa fila. Dos inserciones de la misma persona se
ponen en fila, la segunda ve la primera y no reclama nada. El bloqueo dura lo que
la transacción y no alcanza a los perfiles de nadie más, de modo que dos personas
registrando su dirección a la vez no se estorban.

Se descartaron dos alternativas. Un bloqueo consultivo, `pg_advisory_xact_lock`,
no depende de permisos sobre otra tabla, pero introduce un espacio de nombres de
identificadores numéricos que hay que documentar y respetar en cada uso futuro, y
el proyecto no tiene ninguno todavía. Capturar la violación de unicidad y
reintentar convierte un caso raro en código que se ejecuta siempre y que hay que
probar. El bloqueo de fila es una línea y se lee.

Se comprobó antes de escribirlo que el bloqueo funciona con las políticas
puestas: con `set local role authenticated` y las credenciales del profesional,
la consulta devuelve su fila y la bloquea.

**Consecuencia.** Lo que fallaba no era la invariante sino el guardado: el índice
único siempre impidió que hubiera dos direcciones principales. Corregir la
carrera cambia un fallo intermitente sin explicación por una espera de
milisegundos. HU-06, que agrega elegir entre varias direcciones, hereda el mismo
disparador ya serializado.

---

## 2026-09-15 · Rediseño de la pantalla de bienvenida sin cifras reales ni fotografía del kit

**Contexto.** El autor pidió, fuera del flujo normal de una historia de usuario,
rediseñar `WelcomeScreen` a partir de una referencia visual (`startup.svg`) que
él mismo aportó, reconociendo de entrada que el pedido «tal vez se sale de las
normas»: una cabecera con degradado, una insignia de calificación («4.9
Valoración»), una insignia de disponibilidad («40+ profesionales») con un punto
verde parpadeante, una fotografía circular de una paciente y una profesional, y
enlaces a «Términos y condiciones» / «Política de privacidad» que la aplicación
todavía no tiene. Ninguna historia de `plan.md` pide estas cifras ni esos
enlaces, así que la Disciplina de alcance de `CLAUDE.md` los habría rechazado
por defecto; el autor autorizó explícitamente la excepción al elegir, entre las
opciones planteadas, incluir el diseño completo como contenido de mercadeo.

**Decisión.** Se reconstruyó `WelcomeScreen` siguiendo la referencia, con tres
salvedades que si se sostienen sin autorización expresa:

1. **La fotografía de la referencia no se usa.** El archivo SVG traía incrustada
   una fotografía de una paciente y una profesional que resultó ser del mismo
   tipo que ya prohíbe `docs/design-system.md`, sección «Fotografías de
   personas»: material de un kit de terceros, de licencia no verificada. Se
   sustituyó por un círculo decorativo con un corazón de Material Symbols, sin
   representar a ninguna persona real. El autor pidió después una animación de
   latido en vez de un icono fijo; el resultado es `PulsingHeartIcon`
   (`ui/components/`), con un pulso de dos tiempos («lub-dub») en vez de una
   respiración genérica, que también respeta «Quitar animaciones».
2. **«4.9 Valoración» y «40+ profesionales» son texto de mercadeo fijo**, en
   `values-es/strings.xml` y `values/strings.xml`, no una cifra calculada desde
   `reviews` ni desde `professional_directory`. No se presentan como si
   estuvieran leyendo la base, y ninguna prueba las trata como tales.
3. **Los enlaces legales son texto, no controles.** No hay pantalla de
   términos ni de política de privacidad todavía, así que no reaccionan al
   toque: `.claude/rules/compose.md` exige que un control diga lo que hace al
   pulsarlo, y un enlace sin destino incumpliría esa regla si se viera como uno.
   El día que esa pantalla exista, dejan de ser `Text` y pasan a serlo.

De la referencia también salieron tres componentes nuevos, pensados para
reutilizarse: `AvailabilityDot` y `PulsingHeartIcon` (`ui/components/`), que
comparten la comprobación de «Quitar animaciones» del sistema
(`core/util/MotionPreference.kt`), y el parámetro `leadingIcon` agregado a
`PrimaryButton` para el logotipo de Google, con valor por omisión nulo para no
afectar los ocho usos existentes. Ese logotipo se dibuja sobre un círculo blanco
fijo —nunca `MaterialTheme.colorScheme.surface`— porque la marca de Google se
diseña para fondo claro sin importar el tema de la aplicación que lo aloja; sin
el círculo, la «G» se pierde contra el teal del botón. El patrón completo de la
cabecera queda documentado en `docs/design-system.md`, sección 5, «Cabecera de
bienvenida con degradado», para las próximas pantallas que el autor mencionó
que vendrán.

**Razonamiento.** Verificar en el emulador con el tamaño de fuente del sistema
al 200 % reveló un defecto real, no solo cosmético: la primera versión fijaba la
altura de la cabecera con un cálculo en `dp` a partir del alto de pantalla, y con
texto más grande —o con una pantalla angosta, probado con `wm size` en 360 dp de
ancho— la insignia de calificación crecía lo suficiente para encajarse contra el
círculo decorativo, dejando asomar solo la punta del icono. La cabecera se
reescribió para que `Column` mida su propio contenido con espaciado en vez de
posiciones absolutas calculadas para el tamaño de reposo: crece con el texto en
lugar de solaparlo. `.claude/rules/compose.md` exige verificar toda pantalla al
200 %; este hallazgo es la razón concreta de por qué esa regla existe.

**Consecuencia.** El copy de bienvenida cambió (`auth_welcome_title` pasa de
«Atención de salud en tu casa» a «Salud en Casa», y el subtítulo se reescribió
para coincidir con la referencia), así que cualquier historia futura que cite
ese texto literal debe releerlo. Los enlaces legales quedan como deuda visible:
en cuanto exista una historia de términos y política de privacidad, hay que
volver a `WelcomeLegalFooter` en `WelcomeScreen.kt` y convertirlos en controles
reales.

---

## 2026-09-15 · HU-06: marcar como principal vuelve a leer la lista, y el cuarto criterio no agrega código

**Contexto.** HU-06 extiende la característica de direcciones de HU-05 —hasta
entonces limitada a una sola, siempre la principal— para listar varias,
marcar cualquiera como principal y eliminarlas. Dos preguntas de diseño no
tenían una respuesta evidente, y una tercera resultó no ser una pregunta de
diseño en absoluto.

**Decisión 1. Marcar una dirección como principal se implementa como un
`PATCH` de un solo campo (`PrimaryAddressRow`, solo `is_primary`), y el
resultado que ve la pantalla se obtiene volviendo a pedir la lista completa,
no calculando en el cliente cuál otra fila dejó de ser principal.**

**Razonamiento.** Quién deja de ser principal ya lo decide
`addresses_unmark_previous_primary`, el disparador que existe desde HT-04.
Repetir esa regla en Kotlin —"busca la fila que hoy es principal y ponle
`false`"— crearía una segunda fuente de verdad que puede desincronizarse de
la primera, y es exactamente el tipo de optimismo que ya causó una carrera en
HU-05 con la primera dirección. Releer la lista tras un `PATCH` exitoso es
más lento en un salto de red, pero el mismo principio ya rige
`AddressRepository.saveAddress`, que redibuja desde la fila que devolvió el
servidor y no desde el texto que se escribió: es la forma establecida de este
proyecto de tratar al servidor como la verdad.

**Consecuencia.** `AddressListViewModel.onSetPrimaryClick` no tiene ninguna
lógica de reordenamiento; su única responsabilidad tras un éxito es llamar a
`load()` de nuevo. Cualquier característica futura que también dependa de
qué dirección es la principal debe seguir el mismo patrón: pedir el dato, no
inferirlo de una respuesta parcial.

**Decisión 2. Eliminar una dirección exige un paso de confirmación explícito
en el modelo de vista (`confirmingDeleteId`), separado del identificador que
efectivamente se está borrando (`pendingId`).**

**Razonamiento.** El criterio de aceptación dice «cuando confirmo, entonces
desaparece», lo que implica que existe un paso previo que no borra nada. Sin
un estado propio para «esperando confirmación», la única forma de exigirlo
sería un diálogo controlado enteramente por la capa de presentación sin que
el modelo de vista supiera de él, lo que habría dejado sin probar con JUnit
la regla de negocio más importante del criterio: que un toque en «Eliminar»
por sí solo nunca llega al repositorio.

**Decisión 3. El cuarto criterio —«un profesional sin dirección principal no
aparece en búsquedas»— no se implementa: ya lo garantiza el esquema desde que
HT-04 se aplicó, y esta historia no le agrega código.**

**Razonamiento.** `search_nearby_professionals` une `professionals` con
`addresses` mediante `join ... on a.profile_id = pro.id and a.is_primary`. Al
ser una unión interna, un profesional sin ninguna fila con `is_primary =
true` no genera fila de resultado, sin que haga falta ningún `where` que lo
excluya a propósito. RN-02 —«la búsqueda considera únicamente la dirección
principal»— ya estaba resuelta por el mismo motivo. El único trabajo real de
esta historia sobre este criterio fue confirmarlo leyendo la función en
`20260911120600_functions.sql`, no escribir nada nuevo.

**Consecuencia.** Nadie debe reabrir este criterio al construir HU-11 (la
búsqueda por cercanía, Sprint 4) esperando encontrar una condición de
visibilidad pendiente: la condición ya está en la función que HU-11 va a
consumir. Si HU-11 alguna vez necesita relajar esta regla —por ejemplo, para
degradarse a la ciudad declarada cuando no hay dirección principal—, es un
cambio de `search_nearby_professionals`, no de `addresses`.

**Nota sobre la verificación de este criterio.** No se comprobó con un
experimento nuevo contra la base remota, como sí hicieron HU-04 y HU-05 para
invariantes de esta clase: las consultas de solo lectura contra el proyecto
quedaron bloqueadas por el clasificador de modo automático de la sesión que
implementó esta historia, a mitad de una comprobación de la fila de un
profesional (motivo indicado por la herramienta: «Production Reads»). La
verificación queda como procedimiento para el autor en `plan.md`, historia
HU-06, «Verificación pendiente del autor».

---

## 2026-09-16 · Marcar como principal no escribía nada

**Contexto.** El autor probó HU-06 apenas se entregó y reportó que «marcar
como principal» no funcionaba, ni desde la aplicación ni comprobándolo por
SQL, y pidió reproducir el error hasta la raíz antes de corregirlo.

**Decisión.** `SupabaseAddressDataSource.setPrimary` deja de mandar un objeto
`@Serializable` (`PrimaryAddressRow`, ya eliminado) y construye el cuerpo del
`PATCH` con el operador `update({ set("is_primary", true) })` del SDK, igual
que `SupabaseProfileDataSource.setAvailableNow` desde HU-04.

**Razonamiento.** La reproducción, hecha con `npx supabase db query --linked`
dentro de transacciones con `rollback` sobre las direcciones de prueba que el
autor ya había creado, descartó primero la base: los tres disparadores de
`addresses` estaban bien definidos y en el orden correcto, y un
`update ... set is_primary = true` directo funcionaba tanto con privilegios
completos como simulando al usuario autenticado con `set local role
authenticated` y `set local request.jwt.claims`. El problema estaba en el
cliente. `install(Postgrest)`, en `CoreModule`, no recibe el
`Json { encodeDefaults = true }` que ese mismo archivo construye para
`DataStoreSessionManager` — no hay ninguna línea que lo conecte—, así que cae
al serializador propio de supabase-kt
(`KotlinXSerializer(Json { ignoreUnknownKeys = true })`), con
`encodeDefaults` en `false`, el valor de la biblioteca. `PrimaryAddressRow`
declaraba `isPrimary: Boolean = true`; como el valor codificado coincidía con
el valor por omisión declarado, kotlinx.serialization lo omitía del cuerpo.
El `PATCH` viajaba como `{}`: ninguna columna cambiaba, y
`addresses_unmark_previous_primary` —que solo se dispara en
`update of is_primary`— nunca se evaluaba. Se confirmó con una prueba JVM
desechable antes de tocar el código: codificar `PrimaryAddressRow(isPrimary =
true)` con el `Json` real de producción daba `{}`. La prueba se retiró junto
con la clase, una vez aplicada la corrección.

Se descartó simplemente quitarle el valor por omisión a `isPrimary` — habría
bastado para arreglar este caso puntual— porque el operador `update({ set(...)
})` no depende en absoluto de `encodeDefaults`: escribe directo a un mapa de
`JsonElement`, así que ninguna futura propiedad con un valor por omisión que
coincida con el valor real puede volver a desaparecer del cuerpo. Es además el
patrón que ya existía en el proyecto para una actualización parcial de una
sola columna.

**Consecuencia.** Ninguna otra escritura del proyecto depende hoy de un valor
por omisión que kotlinx.serialization pueda omitir: `AddressRow.reference` no
declara uno, y los campos de `SaveProfileParams` viajan como argumentos
nombrados de `save_my_profile`, cuyo valor por omisión vive en la firma SQL de
la función, no en la fila. Pero cualquier característica futura que escriba
una fila parcial desde un objeto serializable —no una llamada a función—
tiene que evitar declarar un valor por omisión en la propiedad que va a
cambiar, o preferir directamente `update({ set(...) })`. Vale la pena, en
algún momento sin apuro, decidir si `install(Postgrest)` debería recibir de
forma explícita el `Json` de `CoreModule` para que ambos coincidan de raíz;
no se hizo aquí porque cambiar la configuración global del cliente para
corregir un solo campo era más alcance del que este defecto pedía.

---

## 2026-09-16 · Guardar una dirección vuelve a la lista

**Contexto.** El autor pidió que, tras guardar una dirección, la aplicación
fuera directamente a «Mis direcciones» en vez de quedarse en el formulario
mostrando «Dirección guardada.».

**Decisión.** `AddressScreen` recibe `onSaved: () -> Unit` y lo dispara con
`LaunchedEffect(uiState)` en cuanto el estado llega a `SaveStatus.Saved`; el
grafo de navegación lo conecta con `navController.popBackStack()`.

**Razonamiento.** Es el mismo patrón que `WelcomeScreen.onSignedIn` y
`RoleSelectionScreen.onRoleAssigned` ya usan para navegar tras un resultado
asíncrono, así que no hacía falta inventar uno nuevo. `popBackStack()` y no
`navigate(AddressListRoute)`: `AddressRoute` solo se alcanza hoy desde
`AddressListRoute`, que ya está debajo en la pila, y navegar hacia adelante
apilaría una segunda copia de la lista en vez de volver a la que ya estaba
abierta.

**Consecuencia.** Si alguna vez `AddressRoute` se vuelve alcanzable desde otra
pantalla que no sea la lista, esta decisión habría que revisarla.

> **Corregida el mismo día.** La primera versión de esta decisión conectaba
> `onSaved` con `popBackStack()`. El autor probó guardar una tercera
> dirección con dos ya registradas y la lista, al volver, seguía mostrando
> solo las dos anteriores. Ver la entrada siguiente.

---

## 2026-09-16 · La lista de direcciones no se refrescaba al volver de guardar

**Contexto.** Con el ajuste anterior ya aplicado —`onSaved` navegando de
vuelta a la lista—, el autor probó guardar una tercera dirección teniendo ya
dos registradas y la lista, al volver, seguía mostrando solo las dos
anteriores. Pidió que lo mismo se revisara para editar.

**Decisión.** `onSaved` deja de usar `navController.popBackStack()` y pasa a
`navController.navigate(AddressListRoute) { popUpTo(AddressListRoute) {
inclusive = true } }`.

**Razonamiento.** `popBackStack()` vuelve a la entrada de `AddressListRoute`
que ya estaba en la pila desde antes de abrir el formulario, y Navigation
Compose conserva el `AddressListViewModel` de esa entrada mientras no se
destruye. Ese modelo de vista carga la lista una sola vez, en su `init`, así
que volver a él no la actualiza: sigue mostrando la foto de cuando se abrió
la pantalla, sin la dirección que se acaba de guardar ni el cambio que se
acaba de editar. Sacar la entrada vieja de la pila con `popUpTo(...) {
inclusive = true }` y volver a navegar a la misma ruta crea una entrada
nueva, con un `AddressListViewModel` recién creado que carga la lista
completa por primera vez —ya con el cambio adentro—, en vez de reutilizar uno
que nunca se enteró de que algo cambió.

**Consecuencia.** El mismo `onSaved` cubre agregar y editar, así que ambos
flujos quedan corregidos con un solo cambio. Cualquier pantalla futura de
este proyecto que vuelva a una lista después de crear o editar uno de sus
elementos debe recordar este mismo problema: un modelo de vista de Compose
Navigation no se entera solo de que la pantalla a la que pertenece volvió a
primer plano.

---

## 2026-10-01 · El rol deja de ser único: `profile_roles` y `active_role`

**Contexto.** Una reunión con la contraparte del negocio pidió que una misma
persona sea paciente y profesional y alterne entre ambos, como un conductor de
inDrive o Uber alterna entre conducir y viajar. El proyecto llevaba desde el
Sprint 1 con lo contrario escrito en cinco lugares: la columna escalar
`profiles.role`, el disparador `profiles_guard_role`, la función `assign_my_role`,
las dos políticas de inserción de `patients` y `professionals`, y
`save_my_profile`.

**Decisión.** Los roles de una persona viven en `profile_roles(profile_id, role)`,
de solo agregar. El rol en el que está vive en `profiles.active_role`.
`profiles.role` se elimina.

**Razonamiento.** Se evaluaron tres formas. Un arreglo `roles user_role[]` en
`profiles` es la más rápida de escribir y la peor de consultar: no hay clave
foránea que valide sus elementos, y la restricción «el rol activo es uno de los
que tengo» pasaría a ser un disparador. Tres columnas booleanas —`is_patient`,
`is_professional`— obligan a una migración cada vez que aparece un rol. La tabla de
pertenencia da una clave primaria sobre la que apoyar la integridad del rol activo
y una política de seguridad propia, que es donde este proyecto prefiere que vivan
las garantías.

`profiles.role` **se elimina en lugar de conservarse** como «rol principal»
denormalizado. Dos fuentes de verdad para el mismo hecho divergen, y la copia que
nadie lee se queda obsoleta sin que nada falle. Es el «campo suelto para salir del
paso» que la entrada del 2026-09-08 prohíbe.

**Qué resultó más barato de lo estimado.** La estimación inicial fue que habría
que reescribir las políticas de las quince tablas. Al leer el esquema efectivo
resultaron ser **dos**: `patients_insert_own` y `professionals_insert_own`, más el
ayudante `is_admin()`. Todo lo demás resuelve por pertenencia —`patient_id =
auth.uid()`, `professional_id = auth.uid()`, o la existencia de una fila en
`professionals`—, de modo que `services`, `payments`, `messages`, `reviews` y
`request_offers` sobrevivieron sin tocarse. El dato cambió la decisión de cuándo
hacerlo: con ese costo, hacerlo ahora es más barato que convivir con el modelo
viejo un sprint más.

**Consecuencia.** `add_my_role` reemplaza a `assign_my_role` y deja el rol nuevo
como activo. El disparador `profiles_guard_role` desaparece, porque la regla que
hacía cumplir —un rol no se cambia— dejó de ser la regla; la que queda —un rol no
se agrega dos veces— la hace cumplir la clave primaria de `profile_roles`. La
autoasignación de administrador pasa a morir en el `with check` de la política de
inserción, que es un lugar más fuerte que un disparador porque no admite excepción
por `is_admin()`.

---

## 2026-10-01 · La seguridad se decide por posesión del rol, nunca por rol activo

**Contexto.** Con dos roles por persona aparece la pregunta de si las políticas de
seguridad deben mirar `active_role`. La lectura intuitiva dice que sí: si estoy en
modo paciente, no debería poder escribir como profesional.

**Decisión.** Ninguna política de seguridad lee `active_role`. Las políticas
deciden por posesión del rol y por pertenencia de la fila. `active_role` es estado
de presentación.

**Razonamiento.** Una política que mirara el rol activo no impediría nada. Quien
tiene ambos roles cambia de rol cuando quiere —es justamente la funcionalidad que
se está construyendo—, así que la restricción se saltaría cambiando de rol y
repitiendo la petición. Sería teatro de seguridad: coste de implementación y
mantenimiento a cambio de una garantía que no existe. Lo que sí es una frontera
real es la posesión, y esa no se puede eludir desde el cliente.

**Por qué entonces vive en el servidor y no en `DataStore`.** Porque es
continuidad, no seguridad: el rol activo debe sobrevivir a una reinstalación y ser
el mismo en otro dispositivo. `DataStore` cubre sesión y preferencias de este
dispositivo, y el rol en el que alguien trabaja no es una preferencia de
dispositivo.

**Consecuencia.** La separación queda explícita y conviene que siga así: cuando el
Sprint 5 escriba la bandeja del profesional, el filtro por rol activo es un `where`
de la consulta o una condición de la pantalla, nunca una política. Y la matriz de
permisos de `docs/requirements.md` sección 7 se lee por rol activo para lo que la
aplicación muestra, y por posesión para lo que la base de datos permite.

---

## 2026-10-01 · El rol activo lo garantiza una clave foránea compuesta

**Contexto.** `profiles.active_role` tiene que ser uno de los roles que la persona
tiene en `profile_roles`. Un `check` no sirve: no puede consultar otra tabla.

**Decisión.** `profiles` declara
`foreign key (id, active_role) references profile_roles(profile_id, role)`, que
apunta a la clave primaria de esa tabla. La columna admite nulo hasta que se elige
el primer rol.

**Razonamiento.** Las alternativas eran un disparador `before update` que
consultara `profile_roles`, o una validación en el caso de uso. El disparador es
código que hay que mantener y probar para expresar algo que el motor ya sabe hacer;
la validación en el caso de uso no es una garantía, porque la API REST permite el
`PATCH` directo sin pasar por la aplicación.

Que la columna admita nulo no abre un hueco: con `match simple`, que es el
comportamiento por omisión, la restricción se satisface cuando alguna columna de la
clave es nula, y eso es exactamente lo que se quiere mientras la persona no tenga
ningún rol.

Es el mismo criterio que INV-10 aplica a los pagos, donde el total iguala la suma
de la comisión y el monto del profesional por una restricción del motor y no por
una validación de la aplicación.

**Consecuencia.** El cambio de rol no necesita función almacenada ni validación
previa: una actualización directa de la columna es imposible de corromper. Y la
prueba `activeRoleMustBeOneOfTheHeldRoles` vigila el lado del dominio, donde
`ProfileRoles` modela la misma regla para que un estado imposible no sea
representable en memoria.

---

## 2026-10-01 · El cambio de rol se escribe con una consulta del cliente

**Contexto.** Las dos escrituras de rol anteriores del proyecto —elegir el rol y
guardar el perfil— son funciones almacenadas, por atomicidad. Cambiar de rol
parece el caso siguiente de la misma serie.

**Decisión.** Cambiar de rol es una actualización directa de `profiles.active_role`
a través de `profiles_update_own`. No hay función almacenada.

**Razonamiento.** Es una sola columna de una sola tabla: no hay nada que pueda
quedar a medias, así que la atomicidad que justificaba las otras dos funciones no
aplica. Y la única regla que había que garantizar —que el rol sea uno de los que la
persona tiene— ya la impone la clave foránea compuesta. Una función almacenada aquí
sería ceremonia: `.claude/rules/supabase.md` reserva las funciones de servidor para
tres casos y este no es ninguno. Es el mismo razonamiento de la entrada del
2026-09-15 sobre guardar una dirección con una consulta del cliente.

**La escritura usa `update { set(...) }` y no un objeto serializable.** El proyecto
ya pagó este defecto en HU-06, el 2026-09-16: `install(Postgrest)` no recibe
`encodeDefaults = true`, de modo que un campo cuyo valor coincide con su valor por
omisión desaparece del cuerpo de la petición y el `PATCH` que viaja es `{}`. Un rol
activo es un enumerado sin valor por omisión, así que hoy no caería en la trampa,
pero el patrón se mantiene porque la razón para preferirlo no depende del tipo.

**Consecuencia.** `SwitchActiveRoleUseCase` valida que el rol se tenga antes de
escribir, igual que `AddRoleUseCase` valida que no se tenga. Esa validación evita
la petición inútil; la garantía sigue siendo del motor.

---

## 2026-10-01 · La reputación se separa por rol

**Contexto.** `average_rating` y `total_reviews` vivían en `profiles` desde el
2026-09-11, con el argumento de que la reputación es un atributo de la persona.
Con rol múltiple, el promedio del profesional arrastraría las calificaciones que
recibió como paciente. Peor: `reviews_select_visible` hace pública toda reseña cuyo
destinatario esté en `professional_directory`, sin mirar de qué lado del servicio
estuvo, de modo que las reseñas recibidas como paciente se volverían públicas en
cuanto esa persona fuera aprobada como profesional. RF-12.4 solo hace pública la
reputación del profesional.

**Decisión.** `average_rating` y `total_reviews` bajan a `patients` y a
`professionals`. `recalculate_reputation()` deriva el lado desde `services` y
escribe en la tabla correspondiente. `reviews_select_visible` exige que el
destinatario haya sido el profesional de ese servicio.

**Razonamiento.** La entrada de 2026-09-11 describió el costo de esta forma con
precisión —«el paciente necesitaría una columna paralela en `patients` y el
disparador de recálculo tendría que decidir a cuál escribir según el rol del
destinatario»— y lo descartó por complejidad innecesaria. Lo era, con un rol por
persona. Con dos, ese costo compra una corrección que antes no hacía falta y ahora
sí: la alternativa es una fuga de datos que contradice un requisito escrito.

El lado se deriva de `services` y no de `profile_roles` porque lo que define en qué
rol se recibió una calificación es la atención concreta, no los roles que la
persona tiene hoy. Alguien puede recibir una reseña como paciente y activar el rol
profesional al mes siguiente; la reseña no cambia de lado por eso.

**Consecuencia.** `professional_directory` y `search_nearby_professionals` leen la
reputación de `professionals`. Los guardias de columnas gestionadas por el servidor
se reparten: `profiles` conserva el de `active`, y `patients` gana el suyo, que
antes no tenía porque no tenía columnas que proteger. RN-14 recoge la regla en
`docs/requirements.md`.

---

## 2026-10-01 · El domicilio y la base profesional son direcciones distintas

**Contexto.** `professional_covers()` y `search_nearby_professionals()` usan la
única dirección `is_primary` de la persona como centro de su radio de cobertura.
Para alguien con un solo rol eso es correcto. Para alguien con dos, el domicilio
donde quiere ser atendido y la base desde la que cubre una zona colapsan en la
misma fila, y no hay forma de expresar que son distintas.

**Decisión.** `addresses` gana `is_professional_base`, con índice único parcial y
disparador de desmarcado, espejo exacto de los de `is_primary`. Las dos funciones
de cercanía pasan a usar la columna nueva.

**Razonamiento.** La alternativa era conservar una sola dirección principal para
ambos roles y anotar la limitación como trabajo futuro. Se descartó porque la
cercanía es el eje del producto: RF-06.1 y RN-02 dependen de qué punto es el centro
del radio, y dejar ese punto ambiguo compromete la historia de búsqueda del
Sprint 4, que es la que el proyecto todavía no ha podido demostrar.

La columna se agrega en lugar de reutilizar `is_primary` con otro significado según
el rol, que habría sido más barato y mucho peor: el mismo dato significando dos
cosas según quién lo lea es como se construye un esquema que nadie puede razonar.

**El relleno preserva el comportamiento actual.** La migración marca como base la
dirección principal de quien tiene el rol profesional. Sin eso, la columna nacería
vacía y todo profesional aprobado desaparecería de las búsquedas en el instante de
aplicarla, porque la unión que lo encuentra es interna.

**Consecuencia.** El cuarto criterio de HU-06 cambia de enunciado —«sin dirección
principal» pasa a «sin base profesional»— y lo sigue garantizando el mismo `join`
interno, sin código nuevo. RN-02 se reescribe. La pantalla de direcciones ofrece la
marca solo a quien tiene el rol profesional, de modo que un paciente no ve un
control que no significa nada para él.

---

## 2026-10-01 · El cambio de modelo entra como Sprint 2.5 y suspende el Sprint 3

**Contexto.** La reunión que cambió el modelo de rol ocurrió con el Sprint 3 ya
iniciado y HU-06 en curso, a falta de su verificación en dispositivo. Había que
decidir dónde entra el trabajo sin falsear la cronología de un documento que es
evidencia académica.

**Decisión.** Un sprint nuevo, numerado 2.5, que suspende el Sprint 3. Las
historias usan los números siguientes libres —HU-34 a HU-36 y HT-16— sin renumerar
nada.

**Razonamiento.** Meter el refactor dentro del Sprint 3 habría mezclado dos
objetivos de sprint distintos y dejado su retrospectiva sin poder decir nada útil
sobre ninguno. Ponerlo después del Sprint 3 habría significado escribir HU-07 dos
veces, porque la verificación ramifica por rol y con rol múltiple el conjunto de
documentos pasa a ser la unión de los roles que la persona tenga.

El número 2.5 expresa la dependencia y no la fecha: el trabajo pertenece al bloque
de perfiles que cerró el Sprint 2, y toda historia del Sprint 3 en adelante lo
presupone. SCRUM admite repriorizar el Product Backlog en cualquier momento; lo que
no admite es cambiar el alcance del sprint en curso sin dejarlo explícito, y de ahí
que el Sprint 3 quede suspendido en el documento en vez de simplemente continuar.

**Los números de historia no se reasignan.** Insertar HU-06a o correr HU-07 a
HU-33 un lugar habría roto todas las referencias cruzadas de este archivo, que ya
está entregado. El número es un identificador, no un orden de ejecución, y el plan
lo dice de forma explícita.

**Consecuencia.** HU-06 no se traslada por tercera vez: se queda en el Sprint 3 y
se cierra al reanudarlo. La velocidad del Sprint 2.5 se mide aparte, lo que da por
fin la ocasión de cerrar un sprint por plazo que la retrospectiva del Sprint 2
reclamaba, porque este sprint tiene una fecha de corte que no depende de agotar el
alcance.

---

## 2026-10-02 · Una subconsulta dentro de una política obedece a las políticas de la tabla que consulta

**Contexto.** Los experimentos SQL de HT-16 destaparon dos defectos del mismo
origen, uno preexistente y otro introducido ese mismo día.

`request_offers_insert_participants` validaba el hilo de una contraoferta con
`exists (select 1 from public.request_offers parent ...)`. Una expresión de
política que consulta su propia relación hace que PostgreSQL levante `42P17`,
«infinite recursion detected in policy», de modo que la política estaba rota para
**toda** inserción, no solo para la autonegociación. Llegó con la migración
correctiva del 2026-09-12 y nadie lo notó porque emitir una oferta es alcance del
Sprint 6 y nunca se había insertado ninguna.

`reviews_select_visible`, reescrita ese día para que solo fueran públicas las
reseñas recibidas como profesional, expresó la condición como
`exists (select 1 from public.services s ...)`. Esa subconsulta corre bajo las
políticas de `services`, y `services_select_participants` solo muestra un servicio
a su paciente o a su profesional. Para un tercero —el público de una reseña
pública— no devolvía nada, así que ninguna reseña era pública.

**Decisión.** Cuando una política necesita mirar otra tabla, la consulta va en una
función `security definer` que devuelve un booleano, o en una vista
`security_invoker = false`. Nunca como subconsulta directa dentro de la política.
Se crean `offer_continues_thread` y `was_the_professional_of` con ese patrón.

**Razonamiento.** Las dos alternativas evaluadas fueron relajar las políticas de
`services` y de `request_offers` para que el tercero pudiera leerlas, o mover la
pregunta a una función definidora. La primera es inaceptable: abriría filas
completas —montos, partes, identificadores— para contestar un booleano, y rompería
INV-13 por una comodidad de implementación.

Una función definidora es segura aquí porque devuelve exactamente un `boolean` y
nada más: de qué lado de un servicio estuvo alguien, o si una oferta padre
pertenece al mismo hilo. No entrega una fila, ni un monto, ni un identificador, y
quien llama ya conoce los datos que pasa como argumentos. `professional_directory`
funcionaba desde el principio por esta misma razón: es `security_invoker = false`,
así que corre con los privilegios de su dueño.

**Lo que delató el primer defecto, y conviene no olvidar.** El experimento probaba
las dos inserciones, la ilegítima y la legítima, y las dos fallaron con el mismo
`SQLSTATE`. Un rechazo de política y un error de recursión no son el mismo
resultado, y solo uno estaba previsto. Un experimento que hubiera comprobado
únicamente que la inserción ilegítima fallaba habría dado por buena una política
inservible. Una prueba de seguridad tiene que demostrar que **discrimina**, no solo
que deniega.

**Consecuencia.** Toda política futura que necesite consultar otra tabla sigue este
patrón. Las vigentes que ya lo hacen —`professional_services_select_public`,
`availability_slots_select_public`, las de `messages`, las de `payments` y las de
`services`— quedan por revisar con este criterio al llegar a su sprint; las dos
primeras son seguras porque consultan `professional_directory`, que es la vista
definidora. Las correcciones viven en
`20261002135541_fix_offer_policy_recursion` y
`20261002135908_fix_public_review_visibility`, que son migraciones nuevas porque
las que contenían los defectos ya estaban aplicadas.

---

## 2026-10-02 · Una fila de acciones que crece envuelve, y el control segmentado lleva etiquetas cortas

**Contexto.** La verificación en dispositivo del Sprint 2.5 encontró dos defectos
de disposición que ninguna prueba del proyecto podía atrapar.

Agregar «Usar como base profesional» como cuarta acción de la lista de direcciones
hizo que «Editar» y «Eliminar» **dejaran de componerse**. No quedaron recortadas
fuera de pantalla: desaparecieron del árbol de vistas, comprobado con
`uiautomator dump`. El `Row` que las contenía no envuelve, y una dirección pasó a
ser imposible de editar o eliminar, rompiendo dos criterios de HU-06 desde una
historia distinta.

Y el control segmentado del cambio de rol se rompía al 200 % de tamaño de fuente:
«Profesional de salud» desbordaba su segmento y se salía de la píldora.

**Decisión.** La fila de acciones pasa a `FlowRow`. El control segmentado recibe
etiquetas cortas propias —«Paciente» y «Profesional»— mediante
`UserRole.shortLabelRes()`, distintas de las que usa «Mi perfil».

**Razonamiento.** Lo del control segmentado no era una sorpresa:
`docs/design-system.md`, apartado 5, ya advierte que no admite etiquetas largas al
ancho de un teléfono, y es exactamente la razón por la que HU-04 eligió un grupo de
opciones para el tipo de profesional. Lo correcto era respetar esa guía dándole
etiquetas que quepan, no reemplazar el componente. Son inequívocas bajo un
encabezado que ya dice «Estás usando la aplicación como».

Se descartó acortar `profile_role_professional`, que es la etiqueta de «Mi perfil»
y ahí sí debe decir «Profesional de salud»: un control estrecho necesita una
etiqueta corta, y un título no.

`FlowRow` es API estable de Compose desde hace varias versiones, así que no
introduce la dependencia de una API en desarrollo que `CLAUDE.md` prohíbe.

**Consecuencia.** Toda fila de acciones cuyo número de elementos dependa de una
condición —y la de direcciones ya depende de tres— envuelve. Y la lección general
queda anotada: **el proyecto no tiene pruebas de interfaz**, de modo que un defecto
de disposición solo aparece mirando el dispositivo. La deuda de `androidTest` se
arrastra desde el Sprint 1 y ahora tiene dos ejemplos concretos de lo que deja
pasar, que es un argumento mejor que el que tenía.

---

## 2026-10-03 · Un criterio no se anota como «no verificable» sin haberlo intentado contra el entorno

**Contexto.** La verificación en dispositivo del Sprint 2.5 cerró doce de dieciséis
criterios el 2026-10-02 y declaró los otros cuatro no demostrables. Al día
siguiente, tres de los cuatro se verificaron sin escribir una línea de código.

- «El cambio de rol que falla» se había descartado porque exigía «provocar una
  caída de red a mitad de la petición». La provoca
  `adb shell cmd connectivity airplane-mode enable`, más
  `cmd wifi set-wifi-enabled disabled` porque el wifi del emulador no cae con el
  modo avión solo.
- «La elección de rol en el primer ingreso» se había descartado porque exigía «una
  cuenta de Google nueva». `adb shell dumpsys account` mostró que el emulador ya
  tenía una segunda, que nunca había ingresado a la aplicación.
- «La lista de direcciones de un paciente puro» se había descartado porque «la
  cuenta usada tiene los dos roles». Era cierto de esa cuenta, no del proyecto: la
  segunda, eligiendo «Paciente», lo demuestra.

**Decisión.** Antes de anotar un criterio como no demostrable, se intenta una vez
contra el entorno real y se registra qué comando o qué recurso faltó. «No
verificable» pasa a ser una conclusión con evidencia, no una estimación.

**Razonamiento.** Los tres motivos se dedujeron leyendo el código y los datos, que
es donde sí estaba la respuesta a «¿la regla se cumple?» pero no a «¿puedo
demostrarla?». La segunda pregunta es sobre el entorno —qué cuentas hay, qué
permite `adb`, qué estado admite el emulador— y no se contesta desde el código.
Deducirla ahí produce un falso negativo barato de cometer y caro de detectar: el
criterio queda sin marcar, la historia sin cerrar, y el sprint arrastra una deuda
que no existe.

El costo de la comprobación es asimétrico y eso cierra el argumento: intentarlo
cuesta un comando, y equivocarse cuesta evidencia que el tribunal va a echar en
falta.

**Consecuencia.** Los tres criterios quedaron marcados el 2026-10-03 con el
procedimiento que los demostró. El cuarto —«agregar dos veces el mismo rol»— se
cerró el 2026-10-04, y su historia confirma la regla una cuarta vez. Se había
dado por no demostrable porque no tiene gesto en la aplicación, lo cual es cierto,
pero la conclusión que se sacó —que por tanto no era demostrable— no lo era: solo
significaba que no era un criterio de interfaz. El camino existía en el CLI desde
siempre, `supabase db query`, y se había dado por ausente al leer una salida de
`--help` truncada en la mitad de la lista de subcomandos.

Las dos mitades del experimento confirmaron lo que se esperaba:
`select public.add_my_role('PATIENT')` levanta `P0001: role_already_held` en la
línea 30 de la función, y el `insert` directo contra la tabla levanta `23505`
contra `profile_roles_pkey`. `profile_roles` quedó con una sola fila para esa
persona, con el mismo `created_at` antes y después.

**Y hacían falta las dos.** `add_my_role` rechaza antes de llegar a la tabla, de
modo que llamarla sola prueba el guardia de la función y no la clave primaria, que
es lo que sostiene el invariante cuando alguien entra por PostgREST sin pasar por
la función. Es el mismo criterio que la entrada del 2026-10-02 fija para las
pruebas de política: hay que demostrar que **discrimina**, no solo que deniega. El
procedimiento concreto, con el comando y sus trampas de comillas, quedó en
`.claude/rules/testing.md`.

**Y una consecuencia para la deuda de `androidTest`.** Verificar que a un paciente
puro no se le ofrece la base profesional obligó a demostrar a mano que la acción
faltaba por rol y no por disposición —con una segunda dirección, que compone tres
acciones sin perder ninguna, y con la cuenta de doble rol como contraste en la
misma compilación—. Es la pregunta que una prueba de interfaz responde sola, y el
tercer ejemplo concreto de lo que esa deuda deja pasar. Ver la entrada del
2026-10-02 sobre la fila de acciones.

---

## 2026-10-03 · Eliminar una dirección marcada: el motor promueve si queda una, la persona elige si quedan varias

**Contexto.** La verificación de HU-06 del 2026-10-03 reprodujo el defecto que el
Sprint 2.5 había dejado anotado como sospecha. Con «Trabajo» como principal y
«Consultorio» sin marcar, eliminar «Trabajo» dejó a «Consultorio» como única
dirección y sin la marca. `addresses_first_is_primary` solo se dispara al
insertar, de modo que nada promueve a nadie al eliminar, y la persona queda con
direcciones y sin ninguna principal. Es el estado en el que estaban las filas del
autor.

`is_professional_base` tiene la misma forma y consecuencias peores. La unión de
`search_nearby_professionals` con `addresses` es interna, así que perder la base
saca al profesional de toda búsqueda **sin que nada se lo diga**: no recibe
solicitudes y la aplicación no tiene dónde explicárselo.

**Decisión.** Dos disparadores sobre `addresses`, en
`20261003120000_preserve_marked_addresses_on_delete`:

- `addresses_promote_last_address`, `after delete`: si la fila borrada llevaba
  alguna marca y queda **exactamente una** dirección, esa la hereda. Las dos
  marcas viajan juntas, porque quien se queda con una sola dirección no tiene
  nada que elegir sobre ninguna.
- `addresses_guard_marked_delete`, `before delete`: si la fila borrada lleva
  alguna marca y quedan **dos o más**, levanta `address_needs_successor`. La
  persona elige la heredera antes de eliminar.

El cliente resuelve la elección antes de escribir: `DeleteAddressUseCase` lee la
lista, y si hay que elegir devuelve `SuccessorRequired` sin tocar nada. La
pantalla pregunta, y con la respuesta traslada las marcas a la elegida y recién
entonces elimina.

**Razonamiento.** La alternativa evaluada fue promover siempre, con un criterio
automático —la más antigua, o la más cercana—. Se descartó por lo que significa
cada marca. La dirección principal es dónde te atienden, y cualquiera de las
tuyas es candidata; **la base profesional es desde dónde cubres tu zona**, y
elegirla por ti te pone en resultados de búsqueda centrados en una dirección que
nunca declaraste para eso. Promover en silencio una base equivocada es un defecto
peor que el que se está corrigiendo, porque llega hasta el paciente.

Con una sola superviviente no hay nada que elegir, así que ahí sí decide el motor:
preguntar una cosa cuya respuesta es única es un paso de más.

**Por qué el guardia está en el motor y no solo en la pantalla.** Quien pase por
la pantalla nunca lo ve: el cliente traslada la marca primero, de modo que la fila
que se elimina ya no la lleva. Dispara para todo lo demás, y PostgREST expone la
tabla tenga o no la aplicación una pantalla para ella. Es el mismo criterio que
INV-10 aplica a los pagos: la garantía es una restricción del motor, y lo que hay
en el cliente es la experiencia, no la garantía.

**El caso que casi rompe la eliminación de cuentas.** `addresses.profile_id`
referencia a `profiles` con `on delete cascade`, y la acción referencial corre
**después** de que la fila padre desaparece. Sin cuidado, eliminar una cuenta con
tres direcciones abortaría al llegar a la principal, porque el guardia vería dos
supervivientes y levantaría la excepción. Los dos disparadores comprueban que el
perfil siga existiendo, y la ausencia es justo lo que distingue una cascada de
alguien eliminando una dirección suya. RF-01.7 pide esa eliminación de cuenta.

**Verificado el 2026-10-04**, tras aplicar la migración. Los dos disparadores
discriminan: con un superviviente promueve —la dirección insertada sin la marca
quedó con `is_primary = true`—, con dos levanta `address_needs_successor`, y con
el perfil en vías de desaparecer no hace ninguna de las dos, de modo que eliminar
una cuenta con tres direcciones sigue funcionando. El relleno dejó cero personas
con direcciones y sin principal, y ninguna base profesional inventada. Las tres
pruebas corrieron dentro de transacciones con `rollback` y no dejaron residuo.

**Un efecto del relleno sobre datos reales.** La principal del autor quedó en la
dirección que tenía marcada como base profesional, porque era la más antigua y el
relleno marca esa. Es lo correcto según el criterio elegido —reproducir lo que
`addresses_first_is_primary` habría hecho— y a la vez semánticamente incómodo:
nadie quiere que su domicilio sea su oficina. Se corrige con un toque en la
aplicación. Vale como recordatorio de que un relleno acierta con la regla y no
necesariamente con la intención, y de que por eso conviene que sea reversible
desde la interfaz.

**El relleno corrige `is_primary` y deliberadamente no `is_professional_base`.**
Se marca como principal la dirección más antigua de quien tenga direcciones y
ninguna principal, que es la que `addresses_first_is_primary` habría marcado:
reproduce lo que debió pasar en vez de inventar una elección. Para la base no hay
equivalente, por la misma razón que arriba. Un profesional sin base simplemente no
aparece, que es lo que el cuarto criterio de HU-36 ya enuncia.

---

## 2026-10-04 · El borrado de una dirección marcada se serializa con el bloqueo que ya existía

**Contexto.** La revisión de HU-06 encontró que `addresses_guard_marked_delete` y
`promote_last_address` no estaban serializados contra
`first_address_is_primary`. Con una sola dirección marcada, eliminarla y crear
otra a la vez deja a la persona con la nueva y sin ninguna principal: el guardia
cuenta cero supervivientes, la inserción concurrente todavía ve la vieja y no
marca la nueva, y el borrado confirma después.

**Decisión.** Los dos disparadores de borrado toman el mismo bloqueo sobre la
fila de `profiles` que `first_address_is_primary` toma desde el 2026-09-15, en
`20261004090000_serialize_marked_address_delete`.

**Razonamiento.** No es un defecto nuevo: es el del 2026-09-15 visto desde el
otro lado. Aquella corrección serializó dos inserciones entre sí, y la lectura
que la justificó —no se puede bloquear una fila que todavía no existe, así que el
punto de encuentro es la fila del perfil— vale igual para una inserción contra un
borrado. Elegir otro mecanismo aquí habría dejado dos formas distintas de
proteger el mismo invariante.

Con el bloqueo, quien llegue segundo ve el resultado del primero y decide bien:
si el borrado gana, la inserción ya no ve ninguna dirección y marca la nueva; si
gana la inserción, el guardia ve una superviviente y la promueve.

**El bloqueo es `for no key update`, no `for update`.** Los dos sirven para
serializar, porque ambos entran en conflicto consigo mismos y entre sí, que es lo
único que esta corrección necesita. La diferencia está en lo que **no** bloquean:
`for update` choca con el `for key share` que toma toda inserción con clave
foránea contra `profiles` —mensajes, reseñas, `profile_roles`, documentos de
verificación, dispositivos—, de modo que eliminar una dirección detendría
inserciones que no tienen nada que ver hasta confirmar. `for no key update` las
deja pasar.

`first_address_is_primary` sigue con `for update` desde el 2026-09-15 y las dos
formas se serializan igual entre sí, así que la corrección no exige tocarla.
Queda anotado que, si alguna vez se la reescribe por otro motivo, el modo que le
corresponde es el mismo.

**Y resuelve un segundo detalle sin tratarlo aparte.** `select id into
v_survivor` no es estricto, de modo que una inserción colada entre el guardia y
el disparador posterior lo habría hecho elegir una fila arbitraria. Es la misma
carrera, así que la cierra el mismo bloqueo.

**El bloqueo reemplaza a la comprobación de existencia del perfil**, que distingue
una cascada desde `profiles` de la eliminación de una dirección suelta. `perform`
deja `found`, así que una sola sentencia bloquea cuando el perfil está y delata la
cascada cuando no. Esa distinción sigue siendo necesaria: sin ella, eliminar una
cuenta con tres direcciones aborta al llegar a la principal y RF-01.7 deja de
funcionar.

**Esta corrección no se puede verificar con el método habitual.** Las pruebas de
disparador del proyecto corren dentro de una transacción que termina en
`rollback`, y una carrera entre dos transacciones no se reproduce dentro de una
sola. Se sostiene por lectura del código y por la revisión, no por experimento, y
conviene decirlo en vez de dejar suponer que se probó como las demás.

**Lo que esto dice del método.** El defecto no lo encontró ninguna prueba ni
ningún experimento: lo encontró leer el código nuevo junto al que ya existía. Una
corrección que reproduce un invariante ya protegido en otro camino tiene que
mirar primero cómo se protegió allí, porque el mecanismo existente suele ser la
respuesta y porque dos mecanismos distintos para el mismo invariante divergen.

---

## 2026-10-04 · Recorrer la pantalla a mano deja de exigirse en toda historia

**Contexto.** El punto 11 de la Definición de Terminado pedía demostrar
manualmente cada historia en un dispositivo real. En la práctica fue el paso que
más veces bloqueó un cierre, y no por el producto: el emulador falló cuatro veces
seguidas el 2026-10-04 —dos SIGSEGV, un cuelgue del hilo principal de QEMU y una
cuarta caída al reintentar con renderizado por software—, y cada caída se llevó la
cuenta de Google que hacía falta para ingresar, porque la instantánea guardada del
AVD es anterior. HU-06 llegó a tener el escenario montado y se cayó en el toque
siguiente.

**Decisión.** El recorrido manual se gradúa en tres niveles, enumerados en
`plan.md`, «Cuándo hace falta un dispositivo»:

- **Dispositivo físico**, solo para lo que un emulador no representa con
  fidelidad: cámara real, GPS en exteriores, entrega de notificaciones push, y la
  validación con usuarios del Sprint 10.
- **Emulador**, solo para historias críticas: las que rompen el flujo central o
  pierden datos si fallan **y** cuya corrección depende de cómo se compone la
  pantalla.
- **Ninguno** para el resto, que queda cubierto por pruebas unitarias,
  experimentos SQL y previsualizaciones.

A cambio, el punto 9 de la Definición de Terminado pasa de pedir que la pantalla
«se vea correcta» a exigir **previsualización en claro, en oscuro y con la fuente
al 200 %**, en toda pantalla y sin excepción.

**Razonamiento.** La pregunta que decide es qué atrapa un recorrido manual que no
atrape otra cosa. La respuesta es estrecha: defectos de **composición** —texto
recortado, filas que desaparecen del árbol de vistas— e integraciones de
plataforma que no se pueden simular. Las reglas de negocio las cubren las pruebas
unitarias; las políticas y los disparadores, los experimentos SQL. Exigir el
recorrido para todo pagaba el costo completo por una franja estrecha de hallazgos.

Las previsualizaciones cubren la mayor parte de esa franja a coste casi nulo, y
hay evidencia de que es la franja correcta: los tres defectos de disposición que
lleva el proyecto —los dos del Sprint 2.5 y el recorte del diálogo de HU-06—
aparecen los tres en esas tres configuraciones.

**Lo que esto acepta a cambio, dicho sin adornos.** Los defectos de disposición se
van a encontrar más tarde o no se van a encontrar. Dos de los tres citados se
hallaron recorriendo a mano, no por previsualización, así que la cobertura no es
equivalente. El riesgo queda aceptado de forma consciente, y lo que lo cerraría de
verdad es la suite de `app/src/androidTest`, declarada desde el Sprint 0 y todavía
vacía. Mientras siga vacía, este cambio sube el riesgo.

**Qué lo haría revisable.** Si aparece un defecto de disposición en una historia
que esta regla eximió de recorrido, la regla está mal calibrada y la historia
correspondiente sube de nivel. Conviene anotarlo en la retrospectiva del sprint en
que ocurra en vez de tratarlo como un defecto más.

---

## Plantilla para entradas nuevas

```
## AAAA-MM-DD · Título breve de la decisión

**Contexto.** Qué situación obligó a decidir.

**Decisión.** Qué se decidió, en una frase.

**Razonamiento.** Por qué esta opción y no otra. Si se descartaron alternativas,
cuáles y por qué.

**Consecuencia.** Qué implica esto para el resto del sistema. Qué queda
condicionado a partir de ahora.
```

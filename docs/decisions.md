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

## 2026-09-09 · Sprints de dos semanas con velocidad medida, no supuesta

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

## 2026-09-11 · La reputacion vive en `profiles`, no en `professionals`

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

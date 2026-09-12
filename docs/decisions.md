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

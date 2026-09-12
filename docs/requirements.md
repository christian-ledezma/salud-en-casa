# requirements.md — Salud en Casa

Especificación de requisitos del sistema. Es la fuente de verdad del alcance.
Toda historia de usuario de `plan.md` referencia los códigos definidos aquí.

**Convención de códigos**

| Prefijo | Significado |
|---|---|
| `RF-xx` | Requisito funcional |
| `RNF-xx` | Requisito no funcional |
| `RN-xx` | Regla de negocio |
| `INV-xx` | Invariante del dominio (ver también `CLAUDE.md`) |
| `FA-xx` | Fuera de alcance en esta fase |

---

## 1. Contexto

La atención domiciliaria de salud en Bolivia se coordina hoy por vías informales:
recomendaciones personales, publicaciones en redes sociales y conversaciones de
mensajería instantánea. Esto produce tres problemas: el paciente no dispone de
información estandarizada para decidir, la coordinación de cada atención es
particular y no queda registrada, y el profesional independiente depende de su
promoción individual para acceder a trabajo.

El sistema digitaliza esa vinculación mediante una aplicación móvil que conecta a
ambas partes por cercanía geográfica, permite negociar la tarifa, deja registro de
la atención y construye reputación verificable.

## 2. Actores

| Actor | Descripción |
|---|---|
| **Paciente** | Persona que solicita atención domiciliaria para sí o para un familiar |
| **Profesional** | Médico, enfermera, fisioterapeuta o estudiante del área de salud que presta atención domiciliaria de forma independiente |
| **Administrador** | Responsable de verificar documentos y conciliar los pagos y comisiones de la plataforma |

## 3. Requisitos funcionales

### RF-01 · Autenticación e identidad

| Código | Requisito |
|---|---|
| RF-01.1 | El sistema permite el ingreso exclusivamente mediante cuenta de Google. |
| RF-01.2 | El ingreso se realiza con el selector de cuentas nativo del sistema operativo, sin abrir un navegador y sin que el usuario escriba credenciales. |
| RF-01.3 | Al ingresar por primera vez, el sistema crea automáticamente el perfil del usuario con los datos que entrega el proveedor. |
| RF-01.4 | Un usuario sin rol asignado debe elegir entre paciente y profesional antes de acceder a cualquier otra funcionalidad. |
| RF-01.5 | El rol de administrador nunca se autoasigna. Se otorga de forma manual. |
| RF-01.6 | El sistema mantiene la sesión entre ejecuciones y la renueva de forma automática. |
| RF-01.7 | El usuario puede cerrar sesión y puede solicitar la eliminación de su cuenta y sus datos. |

### RF-02 · Gestión de perfiles

| Código | Requisito |
|---|---|
| RF-02.1 | El paciente registra y edita nombre, teléfono, fotografía, fecha de nacimiento, contacto de emergencia y notas relevantes. |
| RF-02.2 | El profesional registra y edita nombre, teléfono, fotografía, tipo de profesional, especialidad, biografía, años de experiencia, tarifa base y radio de cobertura. |
| RF-02.3 | El profesional declara qué tipos de servicio presta y a qué precio de referencia cada uno. |
| RF-02.4 | El profesional declara sus franjas horarias de disponibilidad por día de la semana. |
| RF-02.5 | El profesional indica si está disponible para atención inmediata en este momento. |
| RF-02.6 | El paciente consulta el perfil público de un profesional: biografía, especialidad, servicios, tarifa, reputación y comentarios recibidos. |

### RF-03 · Ubicación

| Código | Requisito |
|---|---|
| RF-03.1 | El usuario registra una o más direcciones, cada una con alias, texto de dirección, referencia y ubicación georreferenciada. |
| RF-03.2 | La ubicación se captura sobre un mapa, con la posición actual como punto de partida y un marcador ajustable. |
| RF-03.3 | El sistema convierte entre dirección textual y coordenadas, y almacena el resultado para no repetir la consulta al proveedor. |
| RF-03.4 | El usuario marca una dirección como principal. |
| RF-03.5 | El usuario edita y elimina sus direcciones. |
| RF-03.6 | El sistema solicita el permiso de ubicación explicando previamente para qué se usa, y funciona de forma degradada si el permiso se deniega. |

### RF-04 · Verificación

| Código | Requisito |
|---|---|
| RF-04.1 | El paciente carga documento de identidad y fotografía de rostro para su verificación. |
| RF-04.2 | El profesional carga, además, título profesional y matrícula o carnet de estudiante. |
| RF-04.3 | Los documentos se almacenan en un contenedor privado, accesible únicamente por su propietario y por el administrador. |
| RF-04.4 | El administrador revisa cada documento y lo aprueba o lo rechaza indicando el motivo. |
| RF-04.5 | El usuario consulta el estado de su verificación y el motivo de un rechazo. |
| RF-04.6 | Un perfil verificado exhibe un distintivo visible para la contraparte. El archivo del documento nunca es visible para la contraparte. |

### RF-05 · Catálogo de servicios

| Código | Requisito |
|---|---|
| RF-05.1 | El sistema mantiene un catálogo de tipos de atención domiciliaria con nombre, descripción, precio de referencia y duración estimada. |
| RF-05.2 | El paciente selecciona el tipo de servicio que requiere al crear una solicitud. |

### RF-06 · Búsqueda y selección de profesional

| Código | Requisito |
|---|---|
| RF-06.1 | El paciente busca profesionales dentro de un radio configurable respecto de una dirección suya. |
| RF-06.2 | La búsqueda filtra por tipo de servicio y por disponibilidad inmediata. |
| RF-06.3 | Los resultados se presentan sobre un mapa y en lista, con distancia, tarifa base y reputación de cada profesional. |
| RF-06.4 | Los resultados se ordenan por distancia ascendente. |
| RF-06.5 | Solo aparecen profesionales verificados y activos. |

### RF-07 · Solicitud de atención

| Código | Requisito |
|---|---|
| RF-07.1 | El paciente crea una solicitud inmediata indicando tipo de servicio, dirección, descripción y presupuesto sugerido. |
| RF-07.2 | Una solicitud inmediata se publica de forma abierta a los profesionales dentro del radio. |
| RF-07.3 | El paciente crea una solicitud agendada dirigida a un profesional específico, con fecha y hora dentro de su disponibilidad declarada. |
| RF-07.4 | El profesional consulta las solicitudes publicadas dentro de su radio de cobertura, actualizadas en tiempo real. |
| RF-07.5 | Una solicitud inmediata sin ofertas expira transcurrido un plazo configurable. |
| RF-07.6 | El paciente cancela su solicitud mientras no haya sido aceptada. |

### RF-08 · Negociación de tarifas

| Código | Requisito |
|---|---|
| RF-08.1 | El profesional emite una oferta sobre una solicitud publicada, con monto y mensaje. |
| RF-08.2 | El paciente responde con una contraoferta, que referencia la oferta anterior. |
| RF-08.3 | La secuencia completa de ofertas y contraofertas es consultable como un hilo ordenado. |
| RF-08.4 | Ambas partes ven las ofertas nuevas en tiempo real, sin recargar. |
| RF-08.5 | Al aceptarse una oferta, el sistema crea el servicio y su registro de pago pendiente en una sola operación. |
| RF-08.6 | Los datos de contacto de la contraparte se revelan únicamente después de aceptada la oferta. |

### RF-09 · Comunicación

| Código | Requisito |
|---|---|
| RF-09.1 | Las partes intercambian mensajes en una conversación vinculada a la solicitud. |
| RF-09.2 | Los mensajes se reciben en tiempo real y se marcan como leídos. |
| RF-09.3 | El detalle de lo realizado durante la atención se registra en esta conversación. |
| RF-09.4 | El sistema notifica a los profesionales cercanos cuando se publica una solicitud inmediata compatible con sus servicios. |
| RF-09.5 | El sistema notifica los eventos relevantes: oferta recibida, oferta aceptada, mensaje nuevo, servicio finalizado. |

### RF-10 · Ciclo de vida del servicio

| Código | Requisito |
|---|---|
| RF-10.1 | El profesional marca su llegada al domicilio, lo que pone el servicio en curso. |
| RF-10.2 | El profesional marca el servicio como completado. |
| RF-10.3 | Cualquiera de las partes cancela el servicio antes de su finalización, indicando el motivo. |
| RF-10.4 | Ambas partes consultan el estado actual del servicio. |

### RF-11 · Pagos

| Código | Requisito |
|---|---|
| RF-11.1 | Al completarse un servicio, el sistema calcula el monto total, la comisión de la plataforma y el monto que corresponde al profesional. |
| RF-11.2 | El paciente confirma en la aplicación haber efectuado el pago, indicando el medio empleado. |
| RF-11.3 | El profesional confirma haber recibido el pago. |
| RF-11.4 | El pago alcanza el estado confirmado únicamente cuando ambas partes coinciden. |
| RF-11.5 | Si las confirmaciones no coinciden, el pago queda en disputa y se deriva a revisión administrativa. |
| RF-11.6 | El profesional consulta el saldo de comisiones acumuladas, liquidadas y pendientes. |
| RF-11.7 | El administrador registra las liquidaciones recibidas y concilia los períodos. |

### RF-12 · Calificación y reputación

| Código | Requisito |
|---|---|
| RF-12.1 | El paciente califica al profesional con un puntaje de 1 a 5 estrellas y un comentario. |
| RF-12.2 | El profesional califica al paciente de la misma forma. |
| RF-12.3 | El sistema recalcula la reputación del profesional al registrarse cada calificación. |
| RF-12.4 | La reputación y los comentarios son visibles en el perfil público del profesional. |

### RF-13 · Historial

| Código | Requisito |
|---|---|
| RF-13.1 | El paciente consulta el historial de sus atenciones con estado, monto, profesional, calificación otorgada y acceso a la conversación. |
| RF-13.2 | El profesional consulta el historial de sus atenciones con el detalle de ingresos y comisiones. |
| RF-13.3 | El administrador consulta el registro centralizado de solicitudes y atenciones, sin acceso al contenido de las conversaciones. |
| RF-13.4 | Los historiales se presentan de forma paginada y ordenados de la atención más reciente a la más antigua. |

## 4. Requisitos no funcionales

| Código | Requisito | Criterio de verificación |
|---|---|---|
| RNF-01 | La búsqueda por cercanía responde en menos de un segundo con el catálogo poblado. | Medición sobre datos de prueba con al menos 500 profesionales. |
| RNF-02 | El arranque en frío de la aplicación no supera los dos segundos. | Medición en dispositivo de gama media. |
| RNF-03 | La descarga de la aplicación no supera los 25 MB. | Tamaño del paquete entregado por la tienda. |
| RNF-04 | La información de un usuario nunca es accesible por otro usuario. | Política de base de datos verificada por prueba automatizada. |
| RNF-05 | La información viaja cifrada y se almacena cifrada en reposo. | Verificación de configuración del proveedor. |
| RNF-06 | Ninguna credencial figura en el código fuente ni en el control de versiones. | Revisión del historial del repositorio. |
| RNF-07 | La interfaz cumple los criterios de contraste de accesibilidad y responde al ajuste de tamaño de fuente del sistema. | Verificación con herramienta de contraste. |
| RNF-08 | Toda lista es paginada. Ninguna consulta devuelve una colección sin límite. | Revisión de las consultas del cliente. |
| RNF-09 | La capa de dominio no depende de Android y sus pruebas se ejecutan sin emulador. | Prueba `domainLayerHasNoPlatformImports` y regla del análisis estático. |
| RNF-10 | El costo mensual de infraestructura no supera USD 35 en operación temprana. | Panel de facturación de los proveedores. |

## 5. Reglas de negocio

| Código | Regla |
|---|---|
| RN-01 | Solo un profesional con verificación aprobada y perfil activo aparece en búsquedas y puede emitir ofertas. |
| RN-02 | La búsqueda por cercanía considera únicamente la dirección principal del profesional y su radio de cobertura declarado. |
| RN-03 | Un estudiante del área de salud puede prestar servicios y registrar lo realizado, pero no puede emitir un diagnóstico. |
| RN-04 | Una solicitud aceptada no admite ofertas nuevas. |
| RN-05 | Una oferta emitida no se edita. Modificar el monto significa emitir una contraoferta que referencia la anterior. |
| RN-06 | Los datos de contacto se revelan solo tras la aceptación de una oferta. |
| RN-07 | El monto acordado se congela en el servicio. Un cambio posterior en la tarifa del profesional no afecta servicios ya acordados. |
| RN-08 | La comisión de la plataforma es un porcentaje configurable del monto acordado. Su valor inicial es 10 %. |
| RN-09 | Un pago se confirma solo con la coincidencia de ambas partes. |
| RN-10 | Una calificación se emite una sola vez por servicio y por autor, y únicamente sobre un servicio completado. |
| RN-11 | El administrador no accede al contenido de las conversaciones. |
| RN-12 | La aplicación es un intermediario tecnológico. La responsabilidad clínica de la atención corresponde al profesional. |

## 6. Invariantes del dominio

Los invariantes están enunciados en `CLAUDE.md`, sección «Invariantes del dominio».
Se numeran allí como INV-01 a INV-14 y son de cumplimiento obligatorio en todo el código.

## 7. Matriz de permisos por rol

Un asterisco indica que la operación está restringida al ámbito del propio usuario.

| Operación | Paciente | Profesional | Administrador |
|---|---|---|---|
| Editar perfil propio | * | * | * |
| Ver perfil público de profesional | Sí | Sí | Sí |
| Cargar documentos de verificación | * | * | — |
| Revisar y aprobar documentos | — | — | Sí |
| Registrar direcciones | * | * | — |
| Buscar profesionales por cercanía | Sí | — | Sí |
| Crear solicitud | Sí | — | — |
| Ver solicitudes publicadas | * | En su radio | Sí |
| Emitir oferta | — | Sí | — |
| Emitir contraoferta | * | * | — |
| Aceptar oferta | * | * | — |
| Enviar y leer mensajes | * | * | **No** |
| Marcar llegada y finalización | — | * | — |
| Confirmar pago | * | * | — |
| Resolver disputa de pago | — | — | Sí |
| Conciliar comisiones | — | * (solo lectura) | Sí |
| Calificar | * | * | — |
| Ver historial propio | * | * | — |
| Ver registro centralizado | — | — | Sí, sin conversaciones |

## 8. Glosario de dominio

Los términos del negocio están en español; los identificadores del código están en
inglés. El mapeo completo y obligatorio se encuentra en `.claude/rules/glosario.md`.

## 9. Fuera de alcance en esta fase

| Código | Elemento | Vía de incorporación posterior |
|---|---|---|
| FA-01 | Registro clínico estructurado con diagnóstico | Tabla nueva vinculada al servicio, con autor, fecha y semántica de solo agregar. El detalle de la atención vive mientras tanto en la conversación (RF-09.3). |
| FA-02 | Pasarela de pagos integrada | Función de servidor que reciba la notificación del proveedor. Los campos `method`, `provider` y `external_reference` ya existen en el modelo. |
| FA-03 | Publicación en Google Play | Requiere cuenta de organización con número D-U-N-S. La distribución de prueba se realiza por App Distribution. |
| FA-04 | Aplicación para iOS | Requiere reescribir el cliente. La separación de capas facilita reutilizar las reglas de negocio, pero no hay código portable. |
| FA-05 | Seguimiento en vivo del profesional en camino | Requiere posición periódica del dispositivo y difusión en tiempo real. |
| FA-06 | Pago dividido o múltiples medios por atención | El modelo de pagos admite la ampliación sin cambio de esquema. |
| FA-07 | Cobertura en varias ciudades con reglas propias | El modelo ya almacena la ciudad en la dirección. |
| FA-08 | Operación sin conexión y caché local | Ningún requisito la exige en esta fase. Se agregaría con una base de datos local y su historia propia. |

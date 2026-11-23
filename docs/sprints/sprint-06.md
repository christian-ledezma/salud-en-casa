# Sprint 6 — Avisos y negociación de tarifas

> Archivo del sprint cerrado, movido desde `plan.md` el 2026-11-22 sin cambiar su
> contenido. El estado vivo del proyecto está en `plan.md`.


**Objetivo del sprint.** Las partes se enteran de lo que les toca sin tener la
aplicación abierta, acuerdan el precio mediante ofertas y contraofertas, y el
acuerdo genera el servicio.

**Objetivo específico.** 3 · Comunicación, coordinación y agenda.

**Puntos:** 42. Eran 26 al cerrar el Sprint 5; el 2026-10-15 se replanificaron las
notificaciones push, HU-20 pasó de 5 a 8 puntos y el sprint cambió de nombre. Ver
`docs/decisions.md`, 2026-10-15, «Notificaciones push». El 2026-10-18 entró HT-22
con 5 puntos, pedida por el autor en medio del sprint, como pide la retrospectiva
del Sprint 5. El 2026-10-23 entró HT-23 con 8 puntos, también pedida por el
autor en medio del sprint.

**Fecha de corte: 7 días corridos desde el primer día del sprint. Primer día del
sprint: 2026-10-16. Fecha de corte: 2026-10-22**, contando los dos extremos. Si las
cuatro historias se cierran antes, se toma la siguiente del Sprint 7 en lugar de
cerrar (retrospectiva del Sprint 5).

**Extendido el 2026-10-23, por decisión del autor, hasta terminar sus historias.**
Al llegar el corte, HT-22 había ocupado el sprint: HU-20 estaba cerrada, HT-22
esperaba la integración continua, y HU-17, HU-18 y HU-19 no habían empezado. Las
tres siguen en este sprint, en ese orden. La retrospectiva registra cuántos días
se extendió y por qué.

**Orden de ejecución: HU-20, HU-17, HU-18, HU-19.** HU-20 va primero, aunque su
número sea mayor, porque construye el canal por el que HU-17, HU-18 y HU-19 avisan
a la contraparte; cada una de ellas tiene un criterio de notificación que sin ese
canal no se puede cumplir. Lo que HU-16 le dejó a HU-17 sigue siendo lo primero de
HU-17.

**Los criterios de notificación agregados a historias ya estimadas no cambian sus
puntos.** Cada uno conecta un evento más al canal de HU-20 y no construye nada
propio. Si la medición del sprint dice otra cosa, la retrospectiva lo corrige.

**Orden desde el 2026-10-18: HT-22, cierre de HU-20, HU-17, HU-18, HU-19.** HU-20
quedó construida y verificada, pero no puede cerrarse: la cuenta de servicio de
Firebase solo se genera desde una consola a la que nadie puede entrar. HT-22 la
desbloquea.

**Orden desde el 2026-10-23: HT-23, HU-17, HU-18, HU-19.** HT-23 va antes de
HU-17 porque cambia la pantalla donde el paciente publica la inmediata, y esa es la
pantalla donde HU-17 mostrará las ofertas que llegan.

**HT-23 y HU-17 cerradas el 2026-10-23.** Sigue HU-18 (contraofertar): extiende
`NegotiationScreen` con la contraoferta del paciente y reusa la suscripción Broadcast
por paciente que HU-17 dejó.

**Orden desde el 2026-10-27: HT-24, cierre de HU-18, HU-19.** El autor recorrió HU-18 en
emuladores y el flujo de la negociación tenía demasiadas pantallas. HT-24 lo recorta antes
de seguir, porque HU-19 pone «Aceptar» justamente en las pantallas que HT-24 cambia. El
criterio 5 de HU-18 (el aviso de contraoferta hacia ambos lados) se prueba en el recorrido
de HT-24, y con eso se cierra HU-18.

**HU-18 y HT-24 cerradas el 2026-10-28.** Sigue HU-19 (aceptar una oferta): la operación
atómica en una función de servidor, el servicio y el pago pendiente creados juntos, y los
datos de contacto que se revelan recién al aceptar.

**HU-19 cerrada el 2026-10-30.** Con ella el Sprint 6 entrega su incremento: una solicitud
avisa, se oferta y contraoferta, y la aceptación crea el servicio y su pago en una sola
transacción. Queda por cerrar el sprint (retrospectiva y `supabase db reset` local).

### HT-23 · Pedir atención y agendar en menos pasos `[x]` — 8 puntos

> Como **paciente**, quiero **pedir atención inmediata desde el mapa de quienes están
> disponibles, y agendar con un profesional desde la lista**, para **resolver cada
> cosa sin recorrer pantallas que no aportan**.

**Origen.** El autor revisó el flujo del paciente el 2026-10-23. El mapa con el
radar servía para buscar a quién agendar, y la inmediata se pedía en un formulario
aparte que no mostraba a nadie. Agendar costaba cinco pantallas y unos siete toques:
inicio, mapa, pin, tarjeta, ficha, «Agendar cita» y el formulario. El razonamiento y
las cuatro decisiones tomadas con el autor están en `docs/decisions.md`, 2026-10-23.

**El flujo nuevo.**

| Paso | Antes | Después |
|---|---|---|
| Inicio del paciente | Siete botones iguales | Dos tarjetas, «Atención ahora» y «Agendar una cita», y debajo «Mis solicitudes» y la cuenta |
| Atención inmediata | Un formulario sin mapa, y después «Mis solicitudes» | Una sola pantalla: el mapa con el radar y los disponibles que atienden el servicio elegido, el formulario y «Pedir atención» fijo al pie. Al publicar, la misma pantalla pasa a «Buscando profesional» |
| Agendar | Mapa, pin, tarjeta, ficha, «Agendar cita» | Lista sin mapa. Cada tarjeta lleva «Agendar», que abre la pantalla de agendar con el servicio del filtro ya elegido. Tocar la tarjeta sigue abriendo la ficha |

**Criterios de aceptación**

- [x] Dado que soy paciente, cuando abro el inicio, entonces las dos primeras
      acciones son «Atención ahora» y «Agendar una cita».
- [x] Dado que abro «Atención ahora», cuando elijo el servicio y la dirección,
      entonces el mapa muestra, alrededor de esa dirección, a los profesionales
      disponibles en este momento que prestan ese servicio y cuya cobertura la
      alcanza, y publico con «Pedir atención» sin cambiar de pantalla.
- [x] Dado que nadie está disponible cerca, cuando miro el mapa, entonces la
      pantalla lo dice y me deja publicar igual.
- [x] Dado que publiqué, cuando la solicitud queda publicada, entonces la pantalla
      pasa a «Buscando profesional», con la hora de vencimiento y la opción de
      cancelar.
- [x] Dado que tengo una inmediata activa, cuando vuelvo a «Atención ahora»,
      entonces veo esa solicitud y no un formulario nuevo.
- [x] Dado que abro «Agendar una cita», cuando filtro por servicio y toco «Agendar»
      en una tarjeta, entonces se abre la pantalla de agendar con ese profesional y
      ese servicio ya elegidos.
- [x] Dado que la búsqueda cambió de forma, cuando se revisa RF-06, entonces sigue
      cumplido entre las dos pantallas: el mapa y la disponibilidad inmediata en
      «Atención ahora»; la lista, el radio y el tipo de servicio en «Agendar una cita».

**Evidencia de los criterios.**

- **Recorrido en emulador, 2026-10-23, confirmado por el autor.** Cubre el inicio,
  publicar, volver con la inmediata activa, cancelar y agendar desde la lista con el
  servicio ya elegido: criterios 1, 2, 4, 5 y 6.
- **El criterio 3, nadie disponible cerca, lo sostienen las pruebas**, sin evidencia
  propia del recorrido. `nobodyAvailableNearbyStillLetsThePatientPublish` y
  `aMapThatCannotBeReadLeavesTheFormUsable` en el modelo de vista, y
  `nobodyNearbyIsSaidAndStillLetsThePatientAsk` en la interfaz.
- **El criterio 7, RF-06, se cumple por revisión.** La función del motor, el orden y
  la exclusión no cambiaron, y el filtro de disponibilidad pasó a «Atención ahora»
  (`FindProfessionalsAvailableNowUseCaseTest`).

**Cerrada el 2026-10-23.**

**Requisitos:** RF-06.1 a RF-06.5, RF-07.1 a RF-07.3, RF-07.6, RF-09.13.

**Tareas**

- [x] `docs/decisions.md`, 2026-10-23, y el glosario.
- [x] «Atención ahora»: `ImmediateCareScreen` y `ImmediateCareViewModel` reemplazan
      a `CreateRequestScreen` y `CreateRequestViewModel`. Caso de uso nuevo
      `GetActiveImmediateRequestUseCase`, que lee una sola fila de
      `my_service_requests`.
- [x] «Agendar una cita»: la búsqueda queda en lista. Se retiran el modo mapa, el
      pin seleccionado y el filtro «Disponibles ahora». La tarjeta recibe la acción
      «Agendar». `ScheduleAppointmentRoute` recibe el servicio elegido.
- [x] El inicio del paciente con las dos tarjetas.
- [x] Las cadenas en `values/` y `values-es/`, y el retiro de las que quedan sin uso.
- [x] Pruebas de modelo de vista y de interfaz al 200 % y en 320 dp de las pantallas
      nuevas o cambiadas. Previsualizaciones en claro, oscuro y 200 %, abiertas y
      miradas.
- [x] `./gradlew build` en verde.
- [x] Recorrido en emulador, porque cambia la pantalla de HU-11, que es crítica: las
      dos tarjetas, publicar y cancelar una inmediata, volver con una activa, y
      agendar desde la lista.

**Estado al 2026-10-23.** `./gradlew build` en verde: 751 pruebas en 129 clases. Las pruebas
nuevas son `ImmediateCareViewModelTest` (18), `ImmediateCareScreenLargeFontTest`
(9) y `FindProfessionalsAvailableNowUseCaseTest`. Se agregaron casos a
`ScheduleAppointmentViewModelTest`, `AccountScreenLargeFontTest` y
`ProfessionalSearchScreenLargeFontTest`. Se retiraron las pruebas del modo mapa y
del filtro «Disponibles ahora», junto con lo que probaban. La prueba de la carrera
del mapa se mutó, quitando la cancelación de la búsqueda anterior, y falló como
debía. Las previsualizaciones en claro, oscuro y 200 % se miraron en las capturas
de `ScreenCatalog`.

**Sin cambios en la base.** El mapa usa `search_nearby_professionals` con «disponible
ahora» y el servicio elegido. Esa función ya exige que la cobertura del profesional
alcance el punto, así que los pines son, en la práctica, los destinatarios de la
inmediata. No son exactamente los mismos: el mapa busca hasta 10 km, y el aviso
llega a cualquiera cuya cobertura alcance la dirección. Por eso la pantalla dice
«disponibles cerca» y no «recibirán tu solicitud».

### HT-22 · Servicios de Google en un proyecto con dos dueños `[x]` — 5 puntos

> Como **autor del proyecto**, quiero **que todo lo que la aplicación usa de Google
> viva en un proyecto que dos cuentas mías puedan administrar**, para **no volver a
> quedar fuera de la configuración por perder el acceso a una sola cuenta**.

**Origen.** El 2026-10-16 la única cuenta propietaria de `salud-en-casa-508102`
quedó atrapada en la verificación en dos pasos. Después de 48 horas y del flujo
de recuperación sigue sin acceso, y ninguna otra cuenta tiene permisos en el
proyecto. Lo desplegado sigue funcionando, pero nada se puede modificar:

- Firebase, con Crashlytics, Cloud Messaging y App Distribution;
- los tres clientes OAuth;
- la clave de Maps.

El razonamiento está en `docs/decisions.md`, 2026-10-18. El procedimiento paso a
paso, con cada valor y cada clic, se le entregó al autor fuera del repositorio,
porque cita las rutas de sus secretos. Aquí constan los pasos y su verificación,
nunca un secreto.

**Qué cambia y qué no.**

| Pieza | Antes | Después |
|---|---|---|
| Propietarios de Google Cloud y Firebase | Una cuenta, inaccesible | Dos cuentas del autor, con recuperación por tres vías |
| `app/google-services.json` | Proyecto viejo | Proyecto nuevo, y el secreto de CI `GOOGLE_SERVICES_JSON` |
| Ingreso con Google | Clientes OAuth del proyecto viejo | Cliente web y dos Android nuevos (depuración y subida). `GOOGLE_WEB_CLIENT_ID` en `local.properties` y en CI. En Supabase, los dos identificadores web durante la transición |
| Pantalla de consentimiento | En prueba, con una lista de usuarios | En producción, con permisos básicos y sin logotipo |
| Maps | Clave del proyecto viejo | Clave nueva, restringida al paquete, a las dos huellas y a Maps SDK for Android |
| Facturación | En una cuenta que nadie ve | Cuenta nueva con dos administradores y alerta de presupuesto en USD 35 (RNF-10) |
| Firma de depuración | `~/.android/debug.keystore`, huella `A7:50:9A:DD…` atrapada en el proyecto viejo | Clave propia del proyecto en `DEBUG_KEYSTORE_FILE`, fuera del repositorio |
| Firma de publicación | No existía | Clave de subida generada y con su huella ya registrada, para HT-10 |
| `applicationId`, Supabase, base de datos, cuentas de usuario | — | **Sin cambios** |

**Criterios de aceptación**

- [x] Dado el proyecto nuevo, cuando consulto su IAM, entonces figuran dos cuentas
      del autor con rol Propietario, y las dos entran a la consola.
      **2026-10-21:** el IAM de `salud-en-casa-app` muestra a `chris.ledezma.s@gmail.com`
      y a `saludencasa.2073@gmail.com` como Propietario. **2026-10-22:** la segunda
      entró a la consola (confirmado por el autor).
- [x] Dadas esas dos cuentas, cuando reviso su seguridad, entonces cada una tiene
      verificación en dos pasos con app autenticadora, llave de acceso y códigos de
      respaldo guardados, y teléfono y correo de recuperación al día.
      **2026-10-22:** confirmado por el autor. La página de seguridad de la cuenta
      no es legible desde el navegador del agente.
- [x] Dado un usuario existente, cuando ingresa con la compilación nueva, entonces
      entra a su misma cuenta con sus roles, y `auth.users` no suma ninguna fila.
      **2026-10-19/20:** cuatro cuentas ingresaron con el cliente `422326765830-…`
      (`chris.ledezma.s`, `ledezma.aramayo.73`, `christian.ledezma@ucb.edu.bo` y
      `salud.en.casa.73`) y `auth.users` siguió en 9, con 9 identidades de Google.
- [x] Dada la pantalla de consentimiento, cuando ingresa una cuenta que no estaba
      en ninguna lista de prueba, entonces puede ingresar.
      **2026-10-21:** el ingreso funciona: la lista de usuarios de prueba tiene
      **cero** cuentas y las cuatro de arriba entraron. Pero el estado de
      publicación sigue en **Prueba**, no en producción como pide la tabla de
      arriba. La consola indica que falta completar la marca antes de publicar.
      **2026-10-23:** la marca tiene nombre, correo de soporte y contacto del
      desarrollador, y el estado **sigue en Prueba**: la aplicación no se llegó a
      publicar. Google exige además una página principal y una política de
      privacidad publicadas. **Decidido con el autor el 2026-10-23:** el criterio se
      cierra tal como está redactado, porque el ingreso funciona con la lista vacía,
      y la publicación en producción pasa a HT-10, antes de la prueba con
      participantes.
- [x] Dado el mapa de «Mi dirección» y el de la búsqueda, cuando abro la pantalla,
      entonces las teselas cargan. **2026-10-21:** las métricas de Maps SDK for
      Android del proyecto nuevo registran solicitudes con respuesta 2xx en la
      última semana, y ninguna con error.
- [x] Dado un fallo de prueba, cuando reabro la aplicación, entonces aparece en el
      Crashlytics del proyecto nuevo. **2026-10-21:** Crashlytics de
      `salud-en-casa-app` muestra `RemoteServiceException$CrashedByAdbException`,
      provocado con `adb shell am crash`, 1 fallo y 1 usuario.
- [x] Dado un ingreso en el teléfono, cuando consulto `device_tokens`, entonces hay
      una fila de esa cuenta registrada contra el proyecto nuevo. **2026-10-20:**
      hay filas de `chris.ledezma.s` y de `ledezma.aramayo.73`, y Firebase, con la
      cuenta de servicio del proyecto nuevo, entregó avisos a esos FID.
- [x] Dado un envío a `master`, cuando corre la integración continua, entonces
      termina en verde con los secretos nuevos. **2026-10-23:** el autor actualizó
      `GOOGLE_WEB_CLIENT_ID` y `GOOGLE_SERVICES_JSON` y envió la rama
      `hu-20-push-notifications`; la integración continua terminó en verde
      (confirmado por el autor). Corre igual sobre `master` al fusionarla.
- [x] Dada la cuenta de facturación, cuando reviso sus presupuestos, entonces hay
      una alerta de USD 35 al 50, 90 y 100 %, dirigida a las dos cuentas.
      **2026-10-21:** la cuenta `010465-855698-617093` tiene «$35 Alerta de
      presupuesto mensual», sobre toda la cuenta, con alertas al 50, 90, 100 y
      150 %. Gasto actual: USD 0. Los destinatarios no se verificaron.
- [x] Dados Supabase y GitHub, cuando reviso sus accesos, entonces cada uno tiene
      recuperación por más de una vía, y Supabase tiene un segundo miembro
      propietario. **2026-10-21:** la organización de Supabase tiene dos Owner,
      `chris.ledezma.s@gmail.com` y `saludencasa.2073@gmail.com`, los dos con MFA
      activo. GitHub: códigos de recuperación guardados, confirmado por el autor el
      2026-10-22.
- [x] Dado el cierre de la transición, cuando todo dispositivo de prueba tiene la
      compilación nueva, entonces el identificador web viejo se retira de Supabase y
      el ingreso sigue funcionando. **2026-10-21:** el proveedor de Google en
      Supabase tiene solo el identificador nuevo, con su secreto y la
      verificación del *nonce* activa. **2026-10-22:** `chris.ledezma.s` ingresó a
      las 12:45 con solo ese identificador, y `auth.users` sigue en 9.

**Requisitos:** RF-01.1, RNF-05, RNF-06, RNF-10.

**Tareas del agente (en el repositorio).**

- [x] La compilación de depuración se firma con la clave que declara
      `DEBUG_KEYSTORE_FILE`. Sin la propiedad se usa la clave de siempre: CI no
      cambia. Comprobado con `signingReport` en los dos caminos el 2026-10-18.
- [x] La decisión, en `docs/decisions.md`, 2026-10-18.
- [x] Al cerrar: `README.md` con el proyecto nuevo, la pantalla de consentimiento
      en producción, la sección de cuentas y accesos, y `DEBUG_KEYSTORE_FILE` en
      `local.properties`. Registrar el resultado de cada criterio aquí, con fecha.
      Hecho el 2026-10-23.

**Cerrada el 2026-10-23.** La publicación de la pantalla de consentimiento pasó a
HT-10.

**Tareas del autor (en las consolas), en este orden.**

1. Respaldar `local.properties` y `app/google-services.json` actuales, para poder
   volver atrás.
2. Asegurar las dos cuentas propietarias: verificación en dos pasos por tres vías
   y códigos de respaldo en el gestor de contraseñas.
3. Generar las dos claves de firma, la de depuración y la de subida, y anotar sus
   huellas SHA-1.
4. Crear el proyecto de Firebase con la cuenta A, sin Google Analytics, y agregar
   la cuenta B como Propietario. B acepta la invitación y entra.
5. Crear la cuenta de facturación, vincularla, sumar a B como administradora y
   crear el presupuesto.
6. Registrar la aplicación Android sin huellas en Firebase, descargar
   `google-services.json` y comprobar que la API de Cloud Messaging esté activa.
7. Configurar la pantalla de consentimiento externa en producción y crear los
   clientes: uno web, con la URL de retorno de Supabase, y dos Android, con
   paquete y huella.
8. Habilitar Maps SDK for Android y crear su clave restringida.
9. En Supabase, poner el identificador web nuevo primero, seguido de los viejos, y
   el secreto nuevo. Antes, guardar los valores anteriores.
10. Actualizar `local.properties`, reemplazar `google-services.json`, verificar con
    `signingReport`, desinstalar la aplicación de cada dispositivo e instalar la
    nueva.
11. Verificar el ingreso con tres cuentas (paciente, doble rol y administrador),
    midiendo `auth.users` antes y después. Verificar también el mapa y Crashlytics.
    El registro del dispositivo se verifica después del `db push` de HU-20, que
    trae la función que lo registra.
12. Actualizar los secretos de CI `GOOGLE_WEB_CLIENT_ID` y `GOOGLE_SERVICES_JSON`.
13. Completar HU-20 con la cuenta de servicio del proyecto nuevo.
14. Sumar un segundo propietario en Supabase y guardar los códigos de recuperación
    de GitHub.
15. Retirar de Supabase el identificador web viejo cuando todos los dispositivos de
    prueba estén actualizados.

**Riesgos y cómo se cubren.**

| Riesgo | Cómo se detecta | Cómo se cubre |
|---|---|---|
| La huella SHA-1 ya está registrada en otro proyecto | Google responde *already in use* al crear el cliente Android | Claves de firma nuevas. Si `signingReport` muestra `A7:50:9A:DD…`, la clave nueva no se aplicó |
| Un usuario existente aparece como cuenta nueva | `auth.users` sube después del ingreso | Se mide antes y después con una sola cuenta. Supabase acepta los dos identificadores, de modo que volver atrás es restaurar el respaldo. El duplicado se borra desde el panel |
| La pantalla de consentimiento queda en prueba | Una cuenta fuera de la lista no puede ingresar | Publicarla en producción, sin logotipo |
| El mapa no dibuja | `Authorization failure` en `adb logcat` | API habilitada, facturación vinculada, restricciones por huella y por API |
| La compilación vieja no se actualiza encima | `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Desinstalar primero. Solo se pierde la sesión |
| CI en rojo | La ejecución falla en la escritura de credenciales | Actualizar los dos secretos antes de enviar |
| La caché de configuración de Gradle conserva la firma vieja | `signingReport` sigue mostrando la clave anterior | `./gradlew --stop` y una compilación con `--no-configuration-cache` |
| Se pierde la clave de subida | — | Respaldada en el gestor de contraseñas. Con Play App Signing se puede reemplazar |
| Se repite el bloqueo de una cuenta | El control de acceso al cerrar cada sprint | Dos propietarios, tres vías de recuperación cada uno y el control en cada cierre |
| La prueba gratuita de la cuenta de facturación vence el **2027-01-18** y Google detiene el proyecto | El mapa deja de cargar | Pulsar «Activar cuenta completa» antes de esa fecha. No cobra por sí misma. Se revisa en el control de acceso de cada cierre de sprint |

**Verificación de cierre.** Todos los criterios marcados, con fecha y con el
resultado de cada consulta. `./gradlew build` en verde. El recorrido de HU-20 en el
teléfono físico se hace ya sobre el proyecto nuevo.

**Control nuevo en el cierre de cada sprint, desde el Sprint 6:** entrar a la
consola de Google Cloud con las dos cuentas propietarias, y a Supabase con sus dos
miembros. Se anota en la retrospectiva.

### HU-20 · Recibir avisos de solicitudes `[x]` — 8 puntos

> Como **profesional**, quiero **recibir una notificación cuando se publique una
> solicitud cerca de mí o cuando un paciente me agende una cita**, para **no perder
> oportunidades por no estar mirando la aplicación**. Y como **paciente**, quiero
> **saber cuándo mi solicitud venció sin acuerdo**, para **volver a pedir atención
> sin quedarme esperando**.

**Criterios de aceptación**

*Solicitudes, para el profesional:*

- [x] Dado que se publica una solicitud inmediata dentro de mi radio y de un tipo
      que presto, cuando ocurre, entonces recibo una notificación con el tipo de
      servicio y la distancia. **2026-10-20:** la inmediata de las 14:39:34 llegó
      al teléfono a las 14:39:40, con la app deslizada fuera de recientes.
- [x] Dado que un paciente me agenda una cita, cuando se crea, entonces recibo una
      notificación con el tipo de servicio y la fecha y hora de la cita.
      **2026-10-22:** cita de las 12:51:11, `sent:1`, mostrada en el teléfono
      (confirmado por el autor).
- [x] Dado que el paciente cancela la cita que me había agendado, cuando la
      cancela, entonces recibo una notificación. **2026-10-22:** cancelación de
      las 12:54:40, `sent:1`, mostrada en el teléfono (confirmado por el autor).
- [x] Dado que toco la notificación, cuando se abre la aplicación, entonces voy
      directo a esa solicitud en mi bandeja. **2026-10-22:** recorrido en el
      teléfono, filas a y d de la guía (confirmado por el autor).
- [x] Dado que no estoy verificado o no estoy disponible, cuando se publica una
      solicitud inmediata, entonces no me llega notificación. Los destinatarios los
      decide la base: experimento e01, que discrimina en cada condición. En datos
      reales, el 2026-10-20 una inmediata de un tipo que el profesional no presta
      («Sondaje vesical») no generó ningún envío.
- [x] Dado que tengo ambos roles y publico una solicitud como paciente, cuando se
      publica, entonces no me llega a mí como profesional (INV-16). Experimento
      e02, con el control de que la misma persona sí recibe la de otro paciente.

*Solicitudes, para el paciente:*

- [x] Dado que mi solicitud vence sin acuerdo, inmediata o agendada, cuando el
      barrido la marca vencida, entonces recibo una notificación que me lleva a
      «Mis solicitudes». **2026-10-20:** el emulador mostró el vencimiento de las
      14:32; e09 cubre las dos modalidades. Al tocarlo abre «Mis solicitudes»
      (2026-10-22, confirmado por el autor).

*El canal, para los dos:*

- [x] Dado que tengo ambos roles y el activo es paciente, cuando llega un aviso de
      profesional y lo toco, entonces la aplicación activa el rol profesional y abre
      la solicitud. **2026-10-22:** fila b de la guía (confirmado por el autor), más
      `OpenNotificationUseCaseTest`.
- [x] Ninguna notificación muestra la descripción de la solicitud, la dirección ni
      la identidad del paciente: solo el tipo de evento y lo necesario para abrirlo.
      Experimento e03 y la lista de claves permitidas de `send-push`.
- [x] Dado que cambio de dispositivo, cuando ingreso, entonces las notificaciones
      llegan al nuevo. **2026-10-20:** `chris.ledezma.s` pasó del emulador al
      teléfono y los avisos le llegaron al teléfono. Experimento e04.
- [x] Dado que cierro sesión, cuando ocurre un evento mío, entonces no llega a ese
      dispositivo. Experimento e05 y `StopReceivingNotificationsUseCaseTest`.
- [x] Dado que otra persona ingresa con su cuenta en el mismo teléfono, cuando
      ocurre un evento mío, entonces no le llega a ella. **2026-10-20:** el FID del
      emulador pasó de `chris.ledezma.s` a `ledezma.aramayo.73`, y desde entonces
      cada envío a `chris.ledezma.s` tuvo un solo destino, el teléfono. Experimento
      e04.
- [x] Dado que es la primera vez que la aplicación necesita el permiso de
      notificaciones, cuando lo pide, entonces antes explica para qué sirve; y si lo
      niego, la bandeja y «Mis solicitudes» siguen funcionando. **2026-10-22:**
      fila j de la guía (confirmado por el autor), más
      `NotificationPermissionViewModelTest`.

**Requisitos:** RF-09.4, RF-09.8, RF-09.10, RF-09.11, RF-09.12, RF-09.13, RN-01,
RN-13, INV-07, INV-08, INV-16.

**Tareas técnicas.**

- Dependencia de Cloud Messaging en el catálogo de versiones. Firebase ya está
  configurado desde HT-02, con Cloud Messaging habilitado y `google-services.json`
  presente; hoy el cliente solo usa Crashlytics.
- Registro y renovación del identificador de dispositivo, y su retiro al cerrar
  sesión. **El registro va por una función de definidor**: `device_tokens.token` es
  único y la política solo deja ver las filas propias, de modo que, en un teléfono
  compartido, la segunda cuenta choca contra la fila invisible de la primera. La
  función reasigna el identificador a quien acaba de ingresar.
- **Los destinatarios los decide el motor, con la misma regla que `request_inbox`**
  —verificado, activo, en radio, con el tipo declarado y nunca sobre su propia
  solicitud—, más `available_now` para las inmediatas. La de una cita va solo a su
  destinatario. La función de servidor no calcula distancias ni filtra (INV-08):
  recibe la lista ya decidida. Una sola copia de la regla, como en HU-16.
- **La primera función de servidor del proyecto.** Envía por la API HTTP v1 de
  Cloud Messaging. La cuenta de servicio de Firebase vive en los secretos de
  Supabase, igual que la clave de servicio (INV-14): nunca en el cliente, el
  repositorio ni un registro. El mecanismo de disparo desde el motor se decide al
  planificar la historia y se registra en `docs/decisions.md`. **Decidido el
  2026-10-16:** trigger en `service_requests` y `pg_net` hacia la función
  `send-push`, con el cuerpo armado por `request_push_body` y la regla de cobertura
  en una sola función, `professional_covers_for`, que ningún cliente ejecuta. Ver
  `docs/decisions.md`, 2026-10-16, «el motor decide y encola».
- **Identificador por instalación (FID), no por token de registro**, que
  `firebase-messaging` 25.1.0 deprecó. Escritura de `device_tokens` solo por
  `register_my_device` y `unregister_my_device`. Ver `docs/decisions.md`,
  2026-10-16, «el teléfono se identifica por su instalación».
- **Mensajes de datos, no de notificación**: el servidor envía el tipo de evento y
  los identificadores, y el dispositivo compone el texto desde los recursos de
  cadenas, en el idioma del teléfono (RF-09.10, `.claude/rules/i18n.md`).
- Canales de notificación de Android por categoría —solicitudes, negociación,
  mensajes, atención y pago, verificación—, con sus nombres en recursos de cadenas,
  para que la persona silencie cada uno desde el sistema. Las notificaciones de una
  misma solicitud se agrupan. **Ajustado el 2026-10-16:** HU-20 crea solo el canal
  «Solicitudes», y cada historia crea el de su evento. Con el teléfono bloqueado
  se ve una versión reservada del aviso, sin el tipo de servicio.
- Apertura del evento al tocar la notificación, con cambio de rol activo si hace
  falta (RF-09.11), por las rutas tipadas de `navigation/`. Hasta que HU-17 cree la
  pantalla de la solicitud, el destino es la bandeja con la fila resaltada.

> **Esta historia es de criticidad alta**: la entrega efectiva se demuestra en un
> dispositivo físico, con la aplicación cerrada. Ver «Cuándo hace falta un
> dispositivo».

#### Avance al 2026-10-16

**Planificación.** Nueve decisiones tomadas con el autor antes del código, cada una
con su recomendación: el mecanismo de disparo, el identificador, el destino al
tocar, el momento del permiso, los canales, la segunda barrera en el teléfono, la
cancelación, la fecha de corte y la pantalla de bloqueo. Están en
`docs/decisions.md`, 2026-10-16, en dos entradas. El diseño lo hizo un arquitecto
de feature-dev sobre dos exploraciones del código. La documentación vigente de
Firebase y Supabase se consultó con context7, y la API de 25.1.3 se leyó en el
bytecode.

**Construido.**

- Tres migraciones:
  - `20261016120000_register_device_by_function`;
  - `20261016121000_professional_covers_for_a_named_professional`;
  - `20261016122000_push_on_request_events`.
- La función `send-push`.
- El paquete `features/notifications/`.
- El resaltado en la bandeja.
- El permiso en contexto.
- La apertura con cambio de rol.

`./gradlew build` concluye sin error, después de la revisión, con lint, ktlint y la
suite completa: 734 pruebas en 128 clases, ninguna saltada ni fallida.

**Mutación de las aserciones nuevas.** Se rompió el código en doce puntos y las
doce pruebas fallaron. Los puntos fueron:

- la segunda barrera;
- el orden del cierre de sesión;
- la espera del arranque;
- el tag por solicitud;
- la versión de la pantalla bloqueada;
- la búsqueda en páginas posteriores;
- el cambio de rol;
- la repetición desde recientes;
- el texto público;
- el guardado antes de subir;
- la tarjeta que no parpadea;
- el enganche del arranque.

**Revisión de cierre con dos revisores de feature-dev** (correctitud y seguridad;
convenciones). Ninguno encontró un defecto crítico. Se corrigió:

- **Los avisos ya mostrados sobrevivían al cierre de sesión.** Quien tomara
  después el teléfono desbloqueado los leía en la bandeja del sistema. Ahora se
  retiran al cerrar sesión.
- **Un cierre de sesión rechazado dejaba el teléfono sin registro** hasta el
  siguiente arranque en frío. Ahora se vuelve a registrar.
- **Un toque pendiente se perdía** si la actividad se recreaba durante el arranque,
  por una rotación o un cambio a modo oscuro. Ahora se guarda en el estado.
- **La función podaba ante cualquier 404.** Un `project_id` mal configurado habría
  borrado todos los dispositivos con el primer envío. Ahora poda solo ante
  `UNREGISTERED`.
- **La búsqueda de la fila resaltada vivía en el modelo de vista.** Pasó al caso
  de uso `FindInboxRequestUseCase`, que trabaja sobre su propia lista, de modo que
  un refresco por señal ya no le descuenta páginas.
- **La distancia estaba escrita dos veces**, en la bandeja y en la búsqueda, con
  claves idénticas. Quedó una sola función, `distanceText`, con las claves
  `common_distance_*`.
- **Los comentarios nuevos repetían el razonamiento de `decisions.md` o describían
  el código.** Quedaron reducidos a citas y restricciones externas: otra vez lo
  encontró la revisión, no yo al escribirlos.
- **Faltaba la prueba al 200 % de «Mis solicitudes» con la tarjeta del permiso.**

**La segunda ronda de mutación encontró dos pruebas débiles.**

- **Una afirmaba contra la misma constante que se mutaba.** Repetía la
  implementación; se reescribió para afirmar la regla, cinco páginas.
- **Un guard contra la carrera con el refresco sobrevivió a la mutación.** Desde el
  caso de uso ya no protegía nada, y se retiró como código muerto.

Las otras mutaciones nuevas fueron detectadas:

- el reintento tras el cierre fallido;
- el `Bundle` del estado guardado;
- la página que falla, que no cuenta como solicitud que se fue.

**Previsualizaciones capturadas y miradas.** El catálogo de capturas ahora incluye
la tarjeta del permiso. Encontraron dos defectos que ninguna prueba veía:

- **En la tarjeta resaltada, la etiqueta «Agendada» desaparecía.** Usaba el mismo
  color contenedor que el resaltado. El resaltado pasó a ser un borde del color
  primario, con la medida nueva `Spacing.highlightBorderWidth`.
- **En oscuro, «Mis solicitudes» no pintaba su fondo** y el título quedaba
  ilegible. Es el mismo defecto de la bandeja, que también quedó corregido, como
  anotó el Sprint 5.

**Firma del JWT de `fcm.ts`.** Se verificó con Node y WebCrypto: una clave RSA
generada, la firma validada con su clave pública, y las claims y la codificación
base64url correctas.

**Experimentos SQL contra el remoto (2026-10-16, `begin…rollback`, todos en verde,
con las tres migraciones sin aplicar dentro de la transacción).** El fixture tiene
un paciente y siete profesionales sintéticos, uno a cada lado de cada condición.
La clave foránea hacia `auth.users` se suelta dentro de la transacción, con la
técnica del 2026-10-09.

| # | Prueba | Resultado |
|---|---|---|
| e01 | `nearbyRequestIsNotifiedExactlyToThoseWhoSeeItInTheirInbox` | **Pasa y discrimina.** La bandeja de la solicitud la ven {ADR, BUSY, DUAL, OK}; los destinatarios son {ADR, DUAL, OK}. Solo BUSY cae, por `available_now`. El pendiente, el inactivo, el que está fuera de radio y el del otro tipo no están en ninguno de los dos lados. Distancia 1.000 m |
| e02 | `aDualRoleUserIsNeverNotifiedOfTheirOwnRequest` | **Pasa y discrimina.** La solicitud de DUAL va a {ADR, OK}, y la del otro paciente sí incluye a DUAL |
| e03 | `notificationPayloadCarriesNoTextWrittenByAUser` | **Pasa.** En los cuatro eventos, ningún centinela de descripción, dirección, referencia ni nombre. Las claves son exactamente las de la lista permitida, y `scheduledAt` sale con `Z` |
| e04 | `aDeviceTokenBelongsOnlyToTheLastAccountThatSignedInOnIt` | **Pasa.** A registra y después B registra el mismo FID: B lo ve, A no, y queda una sola fila. El `unregister` de A deja la fila de B y el de B la borra. Una inserción o un borrado directo dan 42501, un FID corto da `invalid_device_token` y `anon` no puede llamar a la función |
| e05 | `signingOutStopsNotificationsOnThatDevice`, lado servidor | **Pasa.** Antes del cierre de sesión el vencimiento se arma; después, nada |
| e06 | `scheduledRequestIsNotifiedOnlyToItsAddressee` | **Pasa y discrimina.** La agendada va solo a ADR. Al destinatario no verificado no le llega la creación y sí la cancelación (decisión 7). El vencimiento va al paciente |
| e07 | Encolado real con secretos de Vault creados en la transacción | **Pasa.** Seis filas en `net.http_request_queue`: dos inmediatas, la agendada a ADR, dos vencimientos al paciente y la cancelación a UNV. Todas tienen la URL y el secreto correctos y ningún texto de usuario |
| e08 | El trigger nunca tumba la solicitud | **Pasa.** Con `request_push_body` renombrada dentro de la transacción, la solicitud se crea y no se encola nada |
| e09 | El barrido sin `auth.uid()` | **Pasa** dentro de e07: las dos modalidades vencidas encolan `REQUEST_EXPIRED` al paciente |
| e10 | Cancelación | **Pasa** dentro de e07: cancelar la agendada encola un aviso, y cancelar una inmediata no encola nada |
| e11 | Regresión del refactor de cobertura | **Pasa.** `professional_covers_for` da 42501 a `authenticated`. Las cinco preguntas de cobertura y la oferta dentro y fuera de radio responden **igual que la función actual del remoto**, que se corrió aparte sin la migración |
| e12 | `everyTableHasRowLevelSecurityEnabledAndAtLeastOnePolicy` | **Pasa.** Ninguna tabla queda sin RLS o sin política; `device_tokens` conserva `device_tokens_select_own` |

Sin residuo, comprobado después: `pg_net` sigue sin instalar, no hay secretos
`push_*` en Vault, quedan 0 perfiles sintéticos y la clave foránea está de vuelta.

**Cierre, 2026-10-22.** La historia se cerró ese día:

1. el `db push`, el 2026-10-20 (`db push --dry-run`: «Remote database is up to date»);
2. los secretos y el despliegue de `send-push`, el 2026-10-20, con la cuenta de
   servicio del proyecto nuevo de HT-22. La función está `ACTIVE`, versión 3;
3. las previsualizaciones del permiso y de la bandeja resaltada, abiertas y miradas
   por el autor en claro, oscuro y 200 %;
4. el recorrido en el teléfono físico, con la aplicación cerrada;
5. `./gradlew build` en verde el 2026-10-23: 734 pruebas en 128 clases, sin fallas.

El `supabase db reset` local pasa al cierre del Sprint 6, que es donde
`.claude/rules/supabase.md` lo exige. Docker no estaba disponible al cerrar la
historia.

La prueba con `curl` confirma además el campo `fid` de la API HTTP v1, el único
punto de la API que no se pudo leer en el código.

#### Recorrido en dispositivo, 2026-10-20 y 2026-10-22

Teléfono físico nubia con Android 16 y `chris.ledezma.s` como profesional;
emulador con `ledezma.aramayo.73` como paciente.

| Prueba | Resultado |
|---|---|
| Contrato de la función con `curl` | **Pasa.** Un secreto incorrecto da `401`; un cuerpo sin destinatarios da `200` con `{"sent":0,"pruned":0,"failed":0}` |
| Aviso directo con `curl` y el campo `fid` | **Pasa.** `{"sent":1}` y la notificación aparece |
| Solicitud inmediata desde el emulador, con la app del teléfono deslizada fuera de recientes | **Pasa.** El trigger la encola a las 14:39:34 y el teléfono la muestra a las 14:39:40 |
| Vencimiento al paciente por el barrido | **Pasa.** La solicitud de las 14:01 venció a las 14:32 y el emulador muestra el aviso con su `requestId` |
| Cita agendada y su cancelación | El 2026-10-20 el trigger y Firebase respondieron `sent:1`, pero el teléfono no las mostró. **Repetidas el 2026-10-22:** cita a las 12:51:11 y cancelación a las 12:54:40, `sent:1` las dos, mostradas en el teléfono (confirmado por el autor) |

**Avisos entregados que no se mostraron, causa sin determinar.** Los servicios de
Google del teléfono registraron «Successful broadcast» para los avisos de las
14:01, 14:06, 14:09 y 14:17. El registro de uso de Android no muestra ninguno de
los cuatro como notificación. La carga de la cita, reconstruida desde la base, es
válida. Lo que consta:

1. El de las 14:01 llegó **antes de que se concediera el permiso**, a las 14:05:20.
   Sin permiso no se muestra nada, como está diseñado.
2. Los de las 14:06, 14:09 y 14:17 llegaron **con el permiso ya concedido** y
   tampoco se mostraron. No se encontró la causa: los registros del teléfono
   están silenciados por el fabricante, y el diagnóstico se activó recién a las
   14:26.
3. Todos los avisos que sí se mostraron (14:26, 14:27, 14:28 y 14:39) llegaron a
   un proceso **iniciado después** de conceder el permiso: el de las 14:26 y el
   de las 14:27 arrancaron el proceso. No consta si los que fallaron llegaron
   al proceso que existía desde antes del permiso.

Es una coincidencia observada, no una causa demostrada. Falta reproducirlo:
conceder el permiso con la aplicación abierta y, sin cerrarla, enviar un aviso
con los registros de diagnóstico activos. Mientras no se reproduzca ni se
descarte, no se cambia el código.

### HU-17 · Ofertar por una solicitud `[x]` — 5 puntos

> Como **profesional**, quiero **proponer un monto por una atención**, para
> **conseguir el trabajo**.

**Criterios de aceptación**

- [x] Dado que veo una solicitud publicada, cuando emito una oferta, entonces
      indico monto y mensaje.
      *Desde HT-24 (2026-10-27), por decisión del autor, el mensaje es opcional: se
      indica el monto y, si se quiere, un mensaje.*
- [x] Dado que emito la oferta, cuando el paciente abre la solicitud, entonces la
      ve sin recargar.
- [x] Dado que la solicitud ya fue aceptada, cuando intento ofertar, entonces se
      rechaza.
- [x] Dado que no estoy verificado, cuando intento ofertar, entonces se rechaza.
- [x] Dado que emití una oferta, cuando intento editarla, entonces no es posible.
- [x] Dado que emito una oferta, cuando el paciente no tiene la aplicación abierta,
      entonces le llega una notificación con el tipo de servicio y el monto, que lo
      lleva a la negociación.

**Requisitos:** RF-08.1, RF-09.5, RF-09.10, RN-01, RN-04, RN-05, INV-06.

**Decisiones (`docs/decisions.md`, 2026-10-23, HU-17):** el profesional oferta desde
`MakeOfferScreen` (abierta desde la bandeja); el paciente ve las ofertas en
`NegotiationScreen` (abierta desde «Mis solicitudes», desde «Buscando profesional» y
desde el aviso); «sin recargar» por Broadcast sin datos por paciente; la primera
oferta mueve la solicitud a `NEGOTIATING` por trigger, con el guardia de ciclo de
vida ampliado.

**Evidencia de los criterios.**

- Criterios 1, 3, 4, 5 y la deuda heredada: experimentos SQL contra el remoto en
  `begin … rollback` el 2026-10-23 (ver «Experimentos» abajo). El criterio 5 lo
  garantiza el trigger `guard_offer_is_append_only` (errcode `issued_offer_cannot_be_edited`).
- Criterio 2 («sin recargar»): `NegotiationViewModelTest` (Turbine: la señal lleva el
  estado de `Empty` a `Content` sin pasar por `Loading`) e `ImmediateCareViewModelTest`
  (el conteo de ofertas aparece y se actualiza con la señal).
- Criterio 6 (aviso de oferta): `PushPayloadTest` (ida y vuelta de `OfferReceived`,
  monto ausente/negativo/no numérico rechazado), `NotificationContentTest`
  (`everyNotificationEventHasItsTextInAllLocales`, versión de pantalla bloqueada sin
  tipo ni monto) y `NotificationEventParityTest` (los 5 eventos del SQL igualan el enum).
- El recorrido en emulador —el «sin recargar» en vivo (criterio 2) y el aviso de
  oferta con la aplicación cerrada (criterio 6)— lo **confirmó el autor el 2026-10-26**.

**Heredado de HU-16 — saldado.**

- `service_requests_select_addressee` retirada; la decisión de ofertar vive en el
  definidor `professional_may_offer_on`, que no devuelve ubicación ni identidad.
  Experimento `debt1_addressee_cannot_read_full_row`: el destinatario ya no lee la
  fila completa de la agendada por la API.
- La rama agendada decide por `professional_id`, no por cobertura. Experimentos
  `scheduled_addressee_can_offer` (el destinatario sí) y
  `scheduled_non_addressee_cannot_offer` (un profesional que cubre pero no es el
  destinatario, no).

**Experimentos SQL (remoto, `begin … rollback`, 2026-10-23).** Migración sin aplicar,
sembrando el mundo dentro de la transacción. Todos en verde:

- Estructurales: `select_addressee` retirada; `professional_may_offer_on`,
  `move_request_to_negotiating`, `queue_offer_push` y la política
  `patient_offers_listen` existen; la inserción usa `professional_may_offer_on`;
  `request_push_body` define los 5 eventos; cobertura de RLS (toda tabla pública con
  RLS y política).
- Comportamiento: `rn13_patient_cannot_offer_own`, `rn01_unverified_cannot_offer`,
  `rn04_accepted_rejects_offer` (los tres, violación de política),
  `approved_professional_can_offer`, `first_offer_moves_to_negotiating`,
  `offer_cannot_be_edited`, `scheduled_addressee_can_offer`,
  `scheduled_non_addressee_cannot_offer`, `debt1_addressee_cannot_read_full_row`,
  `offer_push_was_enqueued` y `realtime_listens_own_topic_only`.

**Tareas.**

- [x] Migración `20261023130000_offers.sql`: `professional_may_offer_on`, reescritura
      de `request_offers_insert_participants`, retiro de `service_requests_select_addressee`,
      guardia de ciclo de vida ampliado, `move_request_to_negotiating`, `inbox_request`,
      `my_request_offers`, `request_push_body` con `OFFER_RECEIVED`, `queue_offer_push`,
      `notify_patient_offers` y la política de `realtime.messages`.
- [x] Edge Function `send-push`: `amountBob` en la lista permitida.
- [x] Android: dominio, datos y presentación de ofertas en `features/requests/`
      (`MakeOfferScreen`, `NegotiationScreen`), evento push `OFFER_RECEIVED`, enganches
      de navegación y módulo de Koin.
- [x] Cadenas en `values/` y `values-es/`, idénticas.

**Estado al 2026-10-23.** `./gradlew ktlintCheck test build` en verde: 780 pruebas,
0 fallos. Previsualizaciones de `MakeOfferScreen` y `NegotiationScreen` (claro, oscuro
y 200 %) revisadas. Experimentos SQL corridos y anotados. **Cerrada el 2026-10-23.**

**Correcciones tras el recorrido (2026-10-24, `docs/decisions.md`).** El recorrido en
emulador reveló dos defectos de cliente —no del servidor— que daban «error
inesperado» al publicar o agendar: (1) `ServiceRequestDto.suggestedBudgetBob` tipado
como `String?` no decodificaba el `numeric` que PostgREST rinde como número JSON —se
pasó a `BigDecimal` con `BigDecimalSerializer`, con prueba de regresión; (2) el motor
`ktor-client-android` no soporta WebSocket, así que Realtime no conectaba —se cambió a
`ktor-client-okhttp`. El error de publicar/agendar quedó **confirmado resuelto por el
autor** el 2026-10-24, y el recorrido completo en emulador (incluido el «sin recargar»
en vivo y el aviso de oferta con la app cerrada) el **2026-10-26**. Archivos extra:
`RequestDto.kt`, `RequestMapper.kt`, `RequestMapperTest.kt`, `gradle/libs.versions.toml`.

**Confirmar hecho (CLI, fuera del emulador):** `supabase db push` (hecho), redesplegar
`send-push` (necesario para que el aviso lleve el monto), y `supabase db reset` local
para comprobar que las migraciones reconstruyen el esquema. Falta versionar: los cuatro
commits de la entrega.

### HU-18 · Contraofertar `[x]` — 8 puntos

> Como **paciente**, quiero **responder con otro monto**, para **acordar un precio
> que me convenga**.

**Criterios de aceptación**

- [x] Dado que recibo una oferta, cuando respondo con otro monto, entonces se crea
      una contraoferta que referencia la anterior.
- [x] Dado que existe una cadena de ofertas, cuando abro la negociación, entonces
      veo el hilo completo en orden cronológico, con el emisor de cada una.
- [x] Dado que el profesional responde, cuando estoy en la pantalla, entonces veo
      la respuesta sin recargar. *Confirmado por el autor en emulador el 2026-10-27.*
- [x] Dado que rechazo una oferta, cuando confirmo, entonces la solicitud sigue
      publicada para otros profesionales.
- [x] Dado que la contraparte responde con una contraoferta, cuando no tengo la
      aplicación abierta, entonces me llega una notificación con el monto nuevo,
      sea yo el paciente o el profesional.

**Requisitos:** RF-08.2, RF-08.3, RF-08.4, RF-09.5, RF-09.10.

**Tareas técnicas.** Suscripción en tiempo real de la tabla de ofertas expuesta
como flujo · reconstrucción del hilo mediante la referencia al padre.

**Decisiones (`docs/decisions.md`, 2026-10-27, HU-18):** una pantalla de hilo por
profesional, `OfferThreadRoute(requestId, professionalId)`, compartida por los dos roles,
abierta desde la lista de hilos en que se convierte `NegotiationScreen`; el hilo es lineal
y por turnos (se contesta solo a la última oferta de la cadena, y en `PROPOSED`); rechazar
marca la oferta `REJECTED`, lo hace la contraparte del emisor y cierra esa cadena sin
tocar el estado de la solicitud; el aviso de contraoferta es el evento propio
`COUNTER_OFFER_RECEIVED`, con destinatario variable y con el `professionalId` del hilo; el
profesional gana su tema espejo de Realtime, `professional_offers:<id>`.

**Tareas**

- [x] Migración `20261027120000_counter_offers.sql`: `offer_continues_thread` con el
      emisor esperado del padre, el padre en `PROPOSED` y el turno único;
      `professional_already_offered_on`; reescritura de
      `request_offers_insert_participants`; política de rechazo
      `request_offers_reject_counterparty` en lugar de `request_offers_update_participants`;
      `guard_offer_is_append_only` congelando además `expires_at`; `notify_offer_parties`
      con sus dos disparadores y la política `professional_offers_listen`;
      `request_push_body` reescrita con seis ramas y la rama del evento nuevo;
      `queue_offer_push` eligiendo el evento por `parent_offer_id`; `my_request_offers`
      con `parent_offer_id` y `status`.
- [x] Edge Function `send-push`: `professionalId` en la lista permitida, y redespliegue.
- [x] Android, dominio y datos: `OfferStatus`, `OfferThread`, los resultados nuevos, los
      cinco casos de uso, el emisor y la vía de lectura decididos por
      `iAmTheProfessional`, el rechazo que distingue cero filas actualizadas de un éxito.
- [x] Android, presentación: `OfferThreadScreen` y su modelo de vista; `NegotiationScreen`
      como lista de hilos; el botón al hilo en `MakeOfferScreen`; la ruta nueva y su
      cableado.
- [x] Evento push en el cliente: `COUNTER_OFFER_RECEIVED`, el subtipo con su
      `professionalId`, el destino `OfferThread`, el texto y la paridad.
- [x] Cadenas en `values/` y `values-es/`, idénticas, incluida la reescritura de las que
      estaban redactadas solo para la primera oferta.
- [x] Experimentos SQL en `begin … rollback` anotados abajo con su fecha.


**Evidencia de los criterios.**

- Criterio 1 (la contraoferta referencia la anterior): experimento
  `counter_offer_references_previous_offer`, más `patient_cannot_counter_without_a_parent`
  y `patient_cannot_counter_across_threads`, que comprueban que discrimina.
  En el cliente, `SubmitCounterOfferUseCaseTest.carriesTheAnsweredOfferAndItsThread`.
- Criterio 2 (el hilo completo, en orden, con el emisor): `patient_reads_the_whole_thread`
  y `professional_reads_their_own_chain` en el motor; `OfferMapperTest`
  (`ordersTheChainByItsParentAndNotByTheTimestamp`, `groupsTheOffersIntoOneThreadPerProfessional`)
  y `OfferThreadScreenLargeFontTest.everyRoundOfTheThreadStaysOnScreen` en el cliente.
- Criterio 3 (sin recargar): `OfferThreadViewModelTest.aSignalRefreshesTheThreadWithoutLosingWhatIsTyped`
  y `NegotiationViewModelTest.aSignalRefreshesTheListWithoutReloading` (Turbine: la señal
  lleva el estado a su nuevo contenido sin pasar por `Loading`), más
  `professional_listens_own_topic_only` y `patient_listens_own_topic_only` en el motor.
  **Confirmado por el autor en el recorrido en emulador del 2026-10-27.**
- Criterio 4 (rechazar deja la solicitud publicada): `the_request_stays_open_to_other_professionals`
  y `the_rejection_leaves_the_request_negotiating`, con los cinco rechazos que lo acotan
  (`nobody_rejects_their_own_offer`, `an_answered_offer_cannot_be_rejected`,
  `a_rejected_offer_cannot_go_back_to_proposed`, `a_rejected_thread_accepts_no_more_offers`,
  `a_rejected_professional_does_not_offer_again`). En el cliente,
  `OfferThreadViewModelTest.rejectingIsConfirmedBeforeItIsWritten`.
- Criterio 5 (el aviso, sea paciente o profesional): `patient_counter_push_goes_to_the_professional`
  y `professional_answer_push_goes_to_the_patient` en el motor, con
  `push_body_carries_no_offer_message` (RF-09.10); en el cliente,
  `PushPayloadTest.parsesACounterOfferForEitherRole`,
  `OpenNotificationUseCaseTest.aCounterOfferOpensItsThreadForWhicheverSideReceivesIt`,
  `NotificationTapEffectTest.aCounterOfferTapOpensItsThread`, `NotificationEventParityTest`
  (seis eventos) y `NotificationContentTest.everyNotificationEventHasItsTextInAllLocales`.
  Confirmado por el autor en emulador, con la aplicación cerrada hacia ambos lados, el 2026-10-28.

**Experimentos SQL (remoto, `begin … rollback`, 2026-10-27).** Migración sin aplicar,
sembrando el mundo dentro de la transacción. 35 de comportamiento y 20 estructurales,
todos en verde. Los de comportamiento:

| # | Prueba | Resultado |
|---|---|---|
| e01 | `professional_first_offer_is_accepted` | pasa |
| e02 | `professional_cannot_issue_a_second_first_offer` | rechaza |
| e03 | `another_professional_can_still_offer` | pasa |
| e03b | `an_offer_cannot_be_born_accepted` | rechaza |
| e03c | `an_offer_cannot_be_born_with_an_expiry` | rechaza |
| e04 | `counter_offer_references_previous_offer` | pasa |
| e05 | `patient_cannot_counter_without_a_parent` | rechaza |
| e06 | `patient_cannot_counter_across_threads` | rechaza |
| e07 | `professional_answers_the_patients_last_offer` | pasa |
| e08 | `an_earlier_offer_of_the_thread_cannot_be_answered` | rechaza |
| e09 | `patient_reads_the_whole_thread` | 4 filas, con padre, emisor y estado |
| e10 | `professional_reads_their_own_chain` | 3 filas |
| e11 | `professional_does_not_read_another_chain` | 0 filas |
| e12 | `nobody_rejects_their_own_offer` | 0 filas actualizadas |
| e13 | `an_answered_offer_cannot_be_rejected` | rechaza |
| e14 | `expires_at_cannot_be_written` | rechaza |
| e15 | `patient_rejects_the_professionals_offer` | pasa |
| e16 | `a_rejected_offer_cannot_go_back_to_proposed` | 0 filas actualizadas |
| e17 | `a_rejected_thread_accepts_no_more_offers` | rechaza |
| e18 | `a_rejected_professional_does_not_offer_again` | rechaza |
| e19 | `the_request_stays_open_to_other_professionals` | pasa |
| e20 | `the_rejection_leaves_the_request_negotiating` | pasa |
| e20b | `the_engine_stamps_the_offer_clock` | la fecha enviada se descarta |
| e21 | `patient_counters_again_on_their_turn` | pasa |
| e22 | `professional_rejects_the_patients_counter` | pasa |
| e23 | `issued_offer_cannot_be_edited` | rechaza |
| e24 | `rn13_patient_cannot_offer_own` | rechaza |
| e25 | `rn01_unverified_cannot_offer` | rechaza |
| e26 | `first_offer_push_goes_to_the_patient` | 3 encoladas |
| e27 | `patient_counter_push_goes_to_the_professional` | 2 encoladas |
| e28 | `professional_answer_push_goes_to_the_patient` | 1 encolada |
| e29 | `push_body_carries_no_offer_message` | 0 cuerpos con el mensaje |
| e30 | `every_counter_push_carries_its_amount` | 0 sin monto |
| e31 | `professional_listens_own_topic_only` | pasa |
| e32 | `patient_listens_own_topic_only` | pasa |

Los estructurales comprueban los dos índices únicos que sostienen el hilo, el disparador
que sella la fecha, el `with check` que exige `PROPOSED` y sin vencimiento, la firma de
`offer_continues_thread` con el emisor del padre
y el retiro de la de tres argumentos, `professional_already_offered_on`, la condición del
padre (`PROPOSED` y sin respuesta), la política de inserción con las dos ramas, el retiro
de `request_offers_update_participants`, la de rechazo, el guardia de rechazo,
`expires_at` congelado, `my_request_offers` con padre y estado, los seis eventos de
`request_push_body` con el `professionalId`, el disparador de push sin filtro por emisor,
los dos disparadores de Realtime, el retiro de `notify_patient_offers`, las dos políticas
de escucha, y que toda tabla pública conserva RLS con política.

**Hallazgo del experimento.** La primera versión de la regla de turno comparaba
`created_at`, y dentro de una transacción todas las filas comparten el `now()`, de modo
que el desempate quedaba en manos del identificador. La regla pasó a ser estructural —se
contesta la oferta que todavía no tiene respuesta—, que además es la que el hilo
necesita. Está en `docs/decisions.md`, 2026-10-27, y en `OfferMapperTest`.

**Revisión de cierre (2026-10-27).** Dos revisores recorrieron la migración y la rebanada
Kotlin. Del motor salieron tres huecos, todos cerrados y anotados en `docs/decisions.md`:
una oferta podía nacer `ACCEPTED` y rodear la operación atómica de HU-19, la fecha la
ponía el cliente, y las reglas del hilo no tenían índice detrás. Del cliente salieron dos
defectos reales, también corregidos: una recarga por señal pisaba el estado transitorio
—podía reactivar el botón con un envío en vuelo, borrar el error o tumbar el formulario
con un fallo de red— y el profesional veía la cabecera de la contraparte, que es su propia
ficha, cuando el detalle de la solicitud no se podía leer. Se agregaron sus pruebas
(`aSignalWhileSubmittingDoesNotReEnableTheForm`, `aFailedSignalReloadKeepsTheThreadAndTheForm`,
`rejectingOutOfTurnWritesNothing`, `theProfessionalReadsTheRequestAndNotACounterpartHeader`,
`aChainWithoutItsRootKeepsTheOrderTheEngineSent`) y se retiró una que solo comprobaba que
el doble devolvía lo que se le había pedido devolver.

**Estado al 2026-10-27.** `./gradlew build` en verde: 825 pruebas, 0 fallos, análisis
estático y lint incluidos. Previsualizaciones de `OfferThreadScreen` (claro, oscuro,
200 %, contraofertando y cerrada) y de `NegotiationScreen` (claro, oscuro y 200 %)
renderizadas y revisadas.

**Confirmado por el autor (2026-10-28).** `supabase db push` y redespliegue de `send-push`
hechos; recorrido en emulador con dos cuentas sin observaciones. **Cerrada el 2026-10-28.**

### HT-24 · Negociar en menos pasos `[x]` — 8 puntos

> Como **paciente y como profesional**, quiero **negociar sin recorrer pantallas que no
> aportan**, para **llegar a un acuerdo en el menor número de toques**.

**Origen.** El autor recorrió HU-18 en emuladores el 2026-10-27. Funcionaba, pero el
paciente pasaba por «Buscando profesional», «Ofertas recibidas» y el hilo, y el profesional
por la bandeja, «Hacer una oferta», «Oferta enviada» y otra vez la bandeja para llegar al
hilo. El mensaje era obligatorio, el paciente podía contraofertar por debajo de lo que ya
había propuesto, y en «Atención ahora» una cuenta de prueba veía solo «Consultorio» bajo
«Dónde necesitas la atención»: era el alias de su única dirección, pero la pantalla
mostraba solo el alias. El razonamiento y las seis decisiones tomadas con el autor están en
`docs/decisions.md`, 2026-10-27, HT-24.

**El flujo nuevo.**

| Quién | Antes | Después |
|---|---|---|
| Paciente, inmediata | Espera → «Ver ofertas» → lista → hilo | Espera con las ofertas a la vista → hilo |
| Paciente, aviso de oferta | Aviso → lista → hilo | Aviso → hilo de ese profesional |
| Paciente, «Mis solicitudes» | Tarjeta → lista → hilo | Inmediata activa → la espera; agendada en negociación → su hilo |
| Profesional, bandeja | Fila → «Hacer una oferta» → «Oferta enviada» → bandeja → fila → hilo | Fila → una pantalla: oferta y negociación |
| Profesional, aviso de solicitud | Aviso → bandeja resaltada → fila → … | Aviso → la pantalla de la solicitud |

**Criterios de aceptación**

- [x] Dado que publiqué una solicitud inmediata, cuando un profesional oferta, entonces la
      oferta aparece sola en «Buscando profesional», con su calificación, su distancia y su
      monto, sin recargar.
- [x] Dado que llegan varias ofertas, cuando miro la espera, entonces aparecen una debajo
      de otra, y tocar una abre la negociación con ese profesional.
- [x] Dado que soy profesional, cuando abro una solicitud desde la bandeja o desde el
      aviso, entonces la oferto y negocio en la misma pantalla.
- [x] Dado que contraoferto como paciente, cuando el monto es menor que mi presupuesto
      sugerido o que mi última propuesta en esa negociación, entonces se rechaza y la
      pantalla me dice cuál es el mínimo.
- [x] Dado que oferto o contraoferto, cuando no escribo un mensaje, entonces la propuesta
      se envía igual.
- [x] Dado que elijo dónde atenderme, cuando miro las opciones, entonces veo cada dirección
      con su nombre y su calle, y puedo agregar otra sin salir del flujo.
- [x] Dado que me llega un aviso de oferta, de contraoferta o de una solicitud para mí,
      cuando lo toco, entonces se abre directamente la negociación o la solicitud.

**Requisitos:** RF-08.1, RF-08.2, RF-08.3, RF-08.4, RF-09.5, RF-09.10, RNF-08.

**Tareas**

- [x] Migración `20261027150000_negotiate_in_fewer_steps.sql`: `my_request_offers` con la distancia al punto publicado y el presupuesto;
      `guard_patient_counter_floor`; `my_service_requests` con `professional_id`;
      `request_push_body` con el `professionalId` también en `OFFER_RECEIVED`.
- [x] Dominio y datos: mensaje opcional, `OfferError.BelowMinimum`, `counterFloorBob`, la
      distancia y el presupuesto en el hilo, `ServiceRequest.professionalId`.
- [x] La pantalla única del profesional (`OfferThreadScreen` con el estado `Opening`) y el
      retiro de `MakeOfferScreen`.
- [x] Las tarjetas de oferta en la espera, el retiro de `NegotiationScreen` y el destino de
      cada tarjeta de «Mis solicitudes».
- [x] Los avisos abren directo; se retira el resaltado de la bandeja.
- [x] El selector de dirección con alias, calle y «Agregar otra dirección», en «Atención
      ahora» y en «Agendar».
- [x] Experimentos SQL en `begin … rollback` anotados abajo con su fecha.

**Evidencia de los criterios.**

- Criterio 1 (la oferta aparece sola, con calificación, distancia y monto):
  `ImmediateCareViewModelTest.anOfferThatLandsWhileWaitingAppearsFromTheSignal` y
  `enteringTheWaitShowsTheOffersThatAlreadyExist`; la distancia en el motor
  (`the_offer_distance_is_rounded_to_the_hundred_metres`, medida desde el punto publicado) y
  en el mapeador (`OfferMapperTest.eachThreadCarriesItsDistanceAndTheBudget`). Confirmado por
  el autor en emulador el 2026-10-28.
- Criterio 2 (una debajo de otra, y tocar abre la negociación):
  `ImmediateCareScreenLargeFontTest.threeOffersStackAsCardsThatFitTheScreen`;
  `aRejectedNegotiationLeavesTheWait`. Confirmado por el autor en emulador el 2026-10-28.
- Criterio 3 (el profesional oferta y negocia en la misma pantalla):
  `OfferThreadViewModelTest.theProfessionalWithoutAThreadSeesTheFirstOfferForm`,
  `sendingTheFirstOfferTurnsTheScreenIntoTheNegotiation`, `aRefusedFirstOfferKeepsTheFormWithItsError`
  y `aRequestThatCanNoLongerBeOfferedOnShowsGone`; `OfferThreadScreenLargeFontTest.theFirstOfferFormStaysOnScreen`.
- Criterio 4 (el mínimo del paciente): experimentos `patient_cannot_counter_below_the_budget`,
  `patient_cannot_counter_below_their_last_offer`, `patient_can_raise_their_offer`,
  `professional_has_no_floor`, `without_a_budget_the_first_counter_is_free` y
  `a_stranger_never_learns_the_budget`; `SubmitCounterOfferUseCaseTest` (bajo el presupuesto,
  bajo la última propuesta, **exactamente el mínimo pasa**, sin mínimo para el profesional);
  `OfferThreadScreenLargeFontTest.thePatientSeesTheirMinimumUnderTheAmount` y
  `aCounterBelowTheMinimumSaysItOnTheField`.
- Criterio 5 (sin mensaje): `patient_can_counter_at_the_floor_without_a_message` en el motor;
  `SubmitOfferUseCaseTest.anOfferWithoutAMessageIsSent` y
  `SubmitCounterOfferUseCaseTest.carriesTheOfferOnTheTableAndNoMessage`.
- Criterio 6 (dirección con su calle y agregar otra): `AddressSelectionTest` (la recién
  agregada queda elegida),
  `ImmediateCareViewModelTest.comingBackWithANewAddressChoosesItAndKeepsTheForm` y
  `ImmediateCareScreenLargeFontTest.theAddressShowsItsStreetAndTheWayToAddAnother`.
- Criterio 7 (los avisos abren directo): `offer_push_carries_the_professional` en el motor;
  `PushPayloadTest.rejectsAnOfferWithoutItsProfessional`,
  `OpenNotificationUseCaseTest.eachEventOpensItsOwnDestination` y
  `NotificationTapEffectTest.aTapWaitsWhileTheStartupScreenIsShowing`. Confirmado por el autor
  en emulador, con la aplicación cerrada, el 2026-10-28.
- «Mis solicitudes» abre donde está la negociación: `NegotiationEntryTest`.

**Experimentos SQL (remoto, `begin … rollback`, 2026-10-27).** Migración sin aplicar. 14 de
comportamiento y 6 estructurales, todos en verde, y además **la batería completa de HU-18
(35 + 20) corrida otra vez sobre la migración nueva, también en verde**:

| # | Prueba | Resultado |
|---|---|---|
| t01 | `patient_cannot_counter_below_the_budget` | rechaza |
| t02 | `patient_can_counter_at_the_floor_without_a_message` | pasa |
| t03 | `professional_has_no_floor` | pasa |
| t04 | `patient_cannot_counter_below_their_last_offer` | rechaza |
| t05 | `patient_can_raise_their_offer` | pasa |
| t06 | `without_a_budget_the_first_counter_is_free` | pasa |
| t07 | `a_stranger_never_learns_the_budget` | rechaza por la política, nunca por el mínimo |
| t08 | `the_offer_distance_is_rounded_to_the_hundred_metres` | 1200 m |
| t09 | `each_offer_carries_the_request_budget` | 4 filas |
| t10 | `my_service_requests_carries_the_addressee` | pasa |
| t11 | `my_service_requests_stays_read_only` | rechaza |
| t12 | `offer_push_carries_the_professional` | 1 encolada |
| t13 | `counter_push_still_carries_the_professional` | 3 encoladas |
| t14 | `a_deactivated_professional_gives_no_distance` | 0 filas con distancia |

Los estructurales comprueban el guardia del mínimo, solo para el paciente; la distancia y el
presupuesto en `my_request_offers`, medida desde el punto publicado; `professional_id` en
`my_service_requests`; los seis eventos de `request_push_body`, y RLS en toda tabla.

**Un canal que se evitó al escribir el guardia.** El guardia del mínimo es definidor y corre
antes que la política, así que, sin más, habría contestado con su propia etiqueta a quien
insertara sobre una solicitud ajena, y eso deja bisecar el presupuesto de otro paciente
monto por monto. Solo actúa para el dueño de la solicitud; a los demás los rechaza la
política, sin decir nada del presupuesto. El experimento t07 lo comprueba.

**De la revisión de cierre.** La distancia se calculaba uniendo la base por el profesional de
la oferta, así que seguía saliendo para uno desactivado, cuyo punto ya no publica la búsqueda.
Se une por el directorio: fuera de él, la distancia es nula (t14).

**Revisión de cierre (2026-10-27).** Dos revisores, motor y cliente. Del motor, la distancia
de un profesional desactivado (ya anotada arriba). Del cliente, cinco defectos reales,
corregidos y con prueba:

- **«Agregar otra dirección» no volvía al formulario:** guardar llevaba siempre a «Mis
  direcciones». Ahora vuelve a quien la pidió, que relee sus direcciones y elige la nueva
  (criterio 6).
- **Volver de un hilo borraba las tarjetas de la espera** y un fallo pasajero tumbaba la
  pantalla. Ahora se conservan mientras se releen
  (`comingBackToTheWaitKeepsTheCardsWhenTheReadFails`, `aFailedReadOnComingBackKeepsTheWait`).
- **La espera y el hilo compartían el canal de Realtime,** y el que se cerraba primero lo
  cortaba para el otro; además, la limpieza no corría al cancelarse. Ahora hay un flujo
  compartido por tema, que vive mientras alguien escucha, y la limpieza corre aunque la
  corrutina esté cancelada (también en la señal de la bandeja, que tenía el mismo defecto).
  **Se confirma en el recorrido: abrir un hilo, volver, y que una oferta nueva aparezca sola.**
- **La primera oferta enviada con la relectura fallida dejaba el formulario trabado.** Ahora
  la pantalla carga de nuevo y, si falla, ofrece reintentar
  (`aFirstOfferWhoseThreadCannotBeReadDoesNotLeaveTheFormStuck`).
- **Una negativa del motor no releía el hilo,** y dos respuestas a la misma oferta acababan en
  «error inesperado». Ahora la negativa relee el hilo y el índice único se lee como negativa
  (`aRefusedCounterOfferReadsTheThreadAgain`, `OfferErrorMapperTest`).

También: el aviso de contraoferta al profesional abre su hilo sin su propio identificador, de
modo que la pantalla escucha por su lado desde antes de leer; «Mis solicitudes» vuelve a la
espera que ya estaba abierta en vez de apilar otra; las tarjetas tienen identidad estable.
Queda sin cambiar, a sabiendas: tocar un aviso deja «atrás» en el inicio y no en la espera, y
`OfferableRequest.alreadyOffered` se sigue leyendo aunque ya ninguna pantalla lo usa.

**Estado al 2026-10-27.** `./gradlew build` en verde: 827 pruebas, 0 fallos. Se retiraron las
pruebas de las pantallas eliminadas y lo que verificaban de valor se reescribió sobre las que
quedan. Previsualizaciones revisadas: la espera con ofertas (claro, oscuro y 200 %), la pantalla
del profesional antes de ofertar (claro y 200 %), el hilo contraofertando con el mínimo, y el
selector de dirección con su calle.

**Confirmado por el autor (2026-10-28).** `supabase db push`, redespliegue de `send-push` y
recorrido en emulador con dos cuentas hechos, sin observaciones. **Cerrada el 2026-10-28.**

### HU-19 · Aceptar una oferta `[x]` — 8 puntos

> Como **cualquiera de las partes**, quiero **aceptar el monto propuesto**, para
> **cerrar el acuerdo y dar inicio a la atención**.

**Criterios de aceptación**

- [x] Dado que acepto una oferta, cuando confirmo, entonces se marca aceptada, la
      solicitud pasa a aceptada, se crea el servicio y se crea el pago pendiente.
- [x] Dado que la operación falla a la mitad, cuando consulto la base de datos,
      entonces no queda ningún registro parcial.
- [x] Dado que se acepta la oferta, cuando abro la atención, entonces veo los datos
      de contacto de la contraparte, que antes no eran visibles.
- [x] Dado que se acepta la oferta, cuando consulto el servicio, entonces el monto
      congelado coincide con el aceptado.
- [x] Dado que la tarifa base del profesional cambia después, cuando consulto el
      servicio, entonces el monto no cambia.
- [x] Dado que la contraparte acepta mi oferta, cuando se cierra el acuerdo,
      entonces me llega una notificación que me lleva a la atención.
- [x] Dado que la aceptación falla a la mitad, cuando la transacción se revierte,
      entonces no sale ninguna notificación: el aviso se emite solo si la
      transacción se confirma.

**Requisitos:** RF-08.5, RF-08.6, RF-09.5, RF-09.10, RN-06, RN-07, INV-04.

**Tareas técnicas.** Función de servidor que ejecute la aceptación en una sola
transacción · prueba de atomicidad.

**Decisiones (2026-10-29, `docs/decisions.md`).** La aceptación es un definidor
`accept_offer`; el guardia de ciclo de vida —y el de aceptación de la oferta—
reconocen la operación por la existencia del servicio, no por una bandera; la
comisión entra como columna configurable en `platform_settings` (10 % inicial) y
se congela al aceptar; «la atención» es una pantalla propia (`features/services`);
«Aceptar» va en el hilo con diálogo.

**Experimentos SQL (2026-10-29, remoto, `begin … rollback`, sobre la migración sin
aplicar `20261029120000`).** Todos en verde:

- `contact_hidden_before_and_visible_after` — la contraparte no se lee antes de
  aceptar y sí después (A, G).
- `a_client_cannot_set_the_request_to_accepted_directly` — el update directo lo
  rechaza el guardia porque no existe servicio (B).
- `the_issuer_cannot_accept_their_own_offer` (C) y `an_accepted_offer_rejects_a_second_acceptance` (H).
- `accepting_creates_the_service_and_pending_payment_in_one_transaction` — servicio
  congelado a 100,00 ASSIGNED con el profesional fijado; pago PENDING con
  100 = 10 de comisión + 90 del profesional (F1, F2, F3).
- `a_later_base_rate_change_does_not_move_the_frozen_amount` (I).
- `the_acceptance_push_targets_the_issuer_of_the_accepted_offer` — rol PROFESSIONAL,
  el profesional como destino, con monto y sin `professionalId` (J).
- `an_accepted_request_is_no_longer_offerable` (K).
- `my_attention_gives_each_party_the_other` — al paciente el profesional con
  teléfono y el monto congelado; al profesional, el paciente (L, M).
- `a_failed_acceptance_leaves_no_partial_record` — un fallo forzado en el pago
  revierte el servicio, el pago, el estado y el aviso encolado (criterios 2 y 7).

**Corrección tras el recorrido del autor (2026-10-30).** La atención ya no muestra teléfono ni
llamada: el paciente abre la ficha pública del profesional que ya existe y el profesional ve el
nombre del paciente. Migración nueva `20261030120000_attention_without_phone.sql` (`my_attention`
sin teléfono), verificada en el remoto (`begin … rollback`): devuelve la contraparte sin la
columna de teléfono y sigue reservada a una parte del servicio. Registrado en `docs/decisions.md`,
2026-10-30. Pendiente del autor: `supabase db push` de esta migración.

Pendiente del autor (primera migración, ya hecho): `supabase db push`. `send-push` no se
redespliega.

**Estado al 2026-10-30.** `./gradlew build` en verde: análisis estático, lint y pruebas de
interfaz incluidos, sin pruebas apagadas. Pruebas nuevas de la rebanada Kotlin: la aceptación
en `OfferThreadViewModelTest`, `AttentionMapperTest`, `AttentionViewModelTest`,
`AttentionScreenLargeFontTest`, y las de aviso (`PushPayloadTest`, `OpenNotificationUseCaseTest`,
`NotificationTapEffectTest`); `NotificationEventParityTest` sigue verde con `OFFER_ACCEPTED`.
Previsualizaciones de `AttentionScreen` (claro, oscuro, 200 %) renderizadas.

**Confirmado por el autor (2026-10-30).** `supabase db push` de la primera migración hecho;
recorrido en emulador con dos cuentas, aceptación por ambos lados, sin observaciones sobre el
flujo. Tras el recorrido pidió que la atención reutilizara la ficha pública del profesional y
no mostrara teléfono ni llamada; hecho, con la migración `20261030120000` y la entrada de
`docs/decisions.md`, 2026-10-30. **El criterio 3 se cumple por la revelación de identidad:** el
profesional ve el nombre del paciente, que la bandeja ocultaba, y el paciente abre la ficha
pública del profesional; la coordinación del servicio es por la conversación de HU-21, no por
un teléfono. **Cerrada el 2026-10-30.** Pendiente del autor: `supabase db push` de la segunda
migración y los commits.

## Incremento del sprint

Una solicitud publicada dispara una notificación a los profesionales cercanos, y
una cita agendada, a su destinatario; uno oferta y al paciente le llega el aviso,
el paciente contraoferta y al profesional le llega el suyo, el profesional acepta
y el servicio queda creado con su pago pendiente. Nadie tuvo que estar mirando la
aplicación. **El mecanismo distintivo del producto está completo.**

## Retrospectiva

> El Sprint 6 se ejecutó del **16/10/2026** al **30/10/2026**.

Fue el sprint más grande del proyecto —42 puntos, replanificados desde 26 el 2026-10-15 al
sumar HT-22, HT-23 y HT-24— y el más largo, quince días. Entregó el mecanismo distintivo del
producto de punta a punta: una solicitud avisa a los profesionales cercanos, uno oferta, el
paciente contraoferta, y la aceptación crea el servicio y su pago pendiente en una sola
transacción, sin que nadie tuviera que estar mirando la aplicación.

**Qué se aprendió.**

1. **El recorrido temprano del autor ahorra reescritura.** Recorrer la negociación en
   emulador reveló que el flujo tenía demasiadas pantallas; cortarlas (HT-23 y HT-24) a mitad
   de sprint fue caro, pero más barato que arrastrarlas hasta que HU-19 las tocara. Las
   historias de negociación se recorren antes de construir sobre ellas.
2. **Una dependencia externa con un solo dueño bloquea el cierre.** HU-20 quedó construida y
   verificada pero sin poder cerrarse: la cuenta de servicio de Firebase solo se generaba desde
   una consola a la que nadie podía entrar (HT-22). De ahí el control nuevo de cada cierre:
   ingresar con las dos cuentas propietarias de Google Cloud y los dos miembros de Supabase,
   para que el bloqueo no se repita.
3. **Los invariantes se atan a filas reales, no a permisos.** La aceptación atómica de HU-19
   deja pasar la transición a `ACCEPTED` solo si ya existe el servicio, no por una bandera de
   sesión: la política de cancelación del paciente le deja actualizar su solicitud, así que una
   bandera habría sido suya para fijar. La existencia de una fila que solo el definidor puede
   crear no lo es.
4. **Reutilizar lo que existe antes que construir otra vista.** Tras el recorrido de HU-19, la
   pantalla de la atención se recortó: reutiliza la ficha pública del profesional y no muestra
   teléfono ni llamada. La coordinación del servicio es la conversación de HU-21.

**Verificación de cierre.** `./gradlew build` en verde, sin pruebas apagadas. Experimentos SQL
de política y disparadores anotados por historia. `supabase db diff --linked --schema public`
sin deriva el 2026-10-30, tras aplicar las 46 migraciones —incluidas las dos de HU-19— sobre la
base de sombra. Control de acceso de cierre con las dos cuentas propietarias de Google Cloud y
los dos miembros de Supabase (rutina desde el Sprint 6).

**Velocidad.** 42 puntos en quince días: por debajo del ritmo diario de los sprints previos, lo
que se explica por el reflujo de interfaz a mitad de camino y el bloqueo externo de HU-20. El
Sprint 7 tiene 26 puntos y entra holgado; su historia de menor prioridad, HU-37, es la primera
que se traslada si el corte muerde.

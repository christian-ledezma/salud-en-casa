# Salud en Casa

Aplicación móvil Android que vincula pacientes con profesionales de salud para
atención domiciliaria en Bolivia.

Proyecto de grado. El desarrollo sigue SCRUM con sprints de una semana.

| Documento | Contenido |
|---|---|
| `plan.md` | Estado del trabajo, sprints e historias de usuario |
| `docs/requirements.md` | Especificación de requisitos. Fuente de verdad del alcance |
| `docs/decisions.md` | Registro de decisiones técnicas |
| `docs/design-system.md` | Paleta, tipografía y componentes |
| `docs/architecture/` | Diagramas en Mermaid |
| `CLAUDE.md` | Instrucciones para el asistente de desarrollo |

## Requisitos del entorno

| Herramienta | Versión |
|---|---|
| JDK | 17 |
| Android Studio | Quail o posterior |
| Node.js | 20 LTS o superior, solo para la CLI de Supabase |
| Docker | Para el entorno local de Supabase. Opcional: solo hace falta para `supabase start` y `supabase db reset` |

Dispositivo o emulador con **Android 8.0 (API 26)** o superior.

## Puesta en marcha

### 1. Archivos que no están en el repositorio

Dos archivos contienen credenciales y están excluidos del control de versiones.
Hay que obtenerlos antes de compilar.

**`local.properties`** en la raíz del proyecto:

```properties
sdk.dir=/ruta/al/sdk/de/android

SUPABASE_URL=https://<referencia>.supabase.co
SUPABASE_ANON_KEY=<clave anónima>
GOOGLE_WEB_CLIENT_ID=<identificador de cliente OAuth de tipo Web>
MAPS_API_KEY=<clave de Google Maps>
DEBUG_KEYSTORE_FILE=<ruta absoluta a la clave de depuración del proyecto>
```

`DEBUG_KEYSTORE_FILE` es opcional. Sin ella, la versión de depuración se firma con
`~/.android/debug.keystore`; con ella, se firma con la clave del proyecto, cuya
huella es la registrada en Google Cloud. La ruta debe ser absoluta: Gradle no
expande `~`. Ver `docs/decisions.md`, 2026-10-18.

Sin estos valores el proyecto **compila igual**, pero la aplicación muestra un
error de configuración al intentar ingresar. Es deliberado: una compilación que
falla por un archivo ausente no le dice nada útil a quien clona el proyecto.

Los valores se obtienen así:

| Valor | Dónde |
|---|---|
| `SUPABASE_URL` y `SUPABASE_ANON_KEY` | Panel de Supabase → Configuración → API |
| `GOOGLE_WEB_CLIENT_ID` | Google Cloud Console, proyecto `salud-en-casa-app` → Credenciales → ID de cliente OAuth de tipo **Web** |
| `MAPS_API_KEY` | Google Cloud Console → API y servicios → Credenciales. Antes hay que habilitar **Maps SDK for Android** en ese proyecto: sin eso la clave existe pero el mapa no dibuja |

La **clave de servicio** de Supabase no se usa en la aplicación y nunca debe
figurar en este archivo.

`GOOGLE_WEB_CLIENT_ID` es el identificador de tipo **Web**, no el de Android.
La aplicación pide un token destinado a su servidor, y ese servidor es Supabase.
Usar el de Android hace que Supabase rechace el ingreso. Está explicado en
`docs/decisions.md`.

**`app/google-services.json`**:

Consola de Firebase → Configuración del proyecto → Tus aplicaciones → descargar
el archivo de configuración. Se coloca en `app/`, no en la raíz.

A diferencia de `local.properties`, **sin este archivo el proyecto no compila**:
los complementos de Google Services y de Crashlytics lo leen en tiempo de
compilación. El mensaje de Gradle dice exactamente qué falta, así que el fallo es
accionable.

### 2. Compilar

```bash
./gradlew assembleDebug
```

### 3. Base de datos

La CLI de Supabase es una dependencia de desarrollo del propio proyecto, con su
versión fijada en `package.json`. No se instala de forma global: así todos
trabajan con la misma versión.

```bash
npm install
npx supabase login
npx supabase link --project-ref <referencia del proyecto>
npx supabase db push
```

`npx supabase link` pide la contraseña de la base de datos una sola vez y la
guarda en el llavero del sistema operativo.

El trabajo diario va **contra el proyecto remoto de desarrollo**. Para comprobar
que las migraciones reconstruyen la base desde cero, que es lo que exige el cierre
de cada sprint que toque el esquema, hay un entorno local con Docker:

```bash
npx supabase start     # levanta el entorno y aplica todas las migraciones
npx supabase db reset  # recrea la base y las vuelve a aplicar desde cero
npx supabase stop      # libera los contenedores
```

`db reset` sin `--linked` actúa **solo sobre el entorno local**. Nunca se corre
`db reset --linked`: borraría los datos del proyecto remoto.

### 4. Notificaciones push

Las notificaciones las envía la función de servidor `send-push`, y la base la
llama desde un trigger. Hacen falta dos secretos, que **nunca se versionan**
(`docs/decisions.md`, 2026-10-16):

| Dónde | Nombre | Contenido |
|---|---|---|
| Secretos de las funciones | `FCM_SERVICE_ACCOUNT` | El JSON de la cuenta de servicio de Firebase, en una línea |
| Secretos de las funciones | `PUSH_WEBHOOK_SECRET` | Un valor aleatorio |
| Vault de la base | `push_webhook_secret` | El mismo valor aleatorio |
| Vault de la base | `push_project_url` | `https://<referencia del proyecto>.supabase.co` |

La cuenta de servicio se descarga en la consola de Firebase, en Configuración
del proyecto → Cuentas de servicio → Generar nueva clave privada, y se guarda
fuera del repositorio.

```bash
SECRET=$(openssl rand -hex 32)
npx supabase secrets set PUSH_WEBHOOK_SECRET="$SECRET" FCM_SERVICE_ACCOUNT="$(jq -c . <ruta de la clave>.json)"
npx supabase db query --linked "select vault.create_secret('$SECRET', 'push_webhook_secret'); select vault.create_secret('https://<referencia del proyecto>.supabase.co', 'push_project_url');"
npx supabase functions deploy send-push
```

Sin los secretos de Vault el trigger no envía nada, y la aplicación funciona
igual; así se comporta el entorno local.

## Comandos

| Comando | Qué hace |
|---|---|
| `./gradlew assembleDebug` | Compila la versión de depuración |
| `./gradlew test` | Pruebas unitarias, incluidas las de interfaz al 200 % de fuente |
| `./gradlew connectedAndroidTest` | Pruebas instrumentadas, requiere dispositivo. Hoy no hay ninguna |
| `./gradlew ktlintCheck` | Estilo de código |
| `./gradlew staticAnalysis` | Estilo más las reglas propias: capas y texto escrito en el código |
| `./gradlew build` | Verificación completa |
| `./gradlew signingReport` | Muestra las huellas SHA-1 del proyecto |
| `npx supabase migration new <nombre>` | Crea una migración |
| `npx supabase db push --dry-run` | Muestra qué migraciones se aplicarían |
| `npx supabase db push` | Aplica las migraciones pendientes al proyecto remoto |
| `npx supabase start` | Entorno local con Docker |
| `npx supabase db reset` | Reconstruye la base local desde las migraciones |
| `npx supabase db diff --linked --schema public` | Compara el remoto contra las migraciones |
| `npx supabase functions deploy send-push` | Publica la función que envía las notificaciones |

## Integración continua

`.github/workflows/ci.yml` se ejecuta en cada envío y en cada solicitud de
fusión: prepara el entorno con JDK 17, escribe las credenciales desde los
secretos, y corre análisis estático, pruebas y compilación de depuración.

Necesita cuatro secretos del repositorio, en Settings → Secrets and variables →
Actions:

| Secreto | Contenido |
|---|---|
| `SUPABASE_URL` | El mismo valor de `local.properties` |
| `SUPABASE_ANON_KEY` | El mismo valor de `local.properties` |
| `GOOGLE_WEB_CLIENT_ID` | El mismo valor de `local.properties` |
| `GOOGLE_SERVICES_JSON` | `app/google-services.json` codificado en base64 |

El último se obtiene así, y se pega tal cual en el secreto:

```bash
base64 -i app/google-services.json | pbcopy     # macOS
base64 -w0 app/google-services.json             # Linux
```

## Monitoreo de fallos

Los fallos se reportan a Firebase Crashlytics. El panel está en la consola de
Firebase → Crashlytics.

Crashlytics **no sube el reporte en el momento del fallo**, sino en el arranque
siguiente: para verlo en el panel hay que volver a abrir la aplicación después de
que se cierre. Para comprobarlo, con la aplicación abierta en un dispositivo
conectado:

```bash
adb shell am crash bo.saludencasa.app
```

## Verificar la configuración

Si la autenticación con Google falla, el orden de revisión es:

1. `./gradlew signingReport` y comparar la huella SHA-1 de depuración contra la
   registrada en Google Cloud Console.
2. Revisar el estado de publicación en Google Auth Platform → Público. Hoy está
   en «Prueba» con la lista de usuarios de prueba vacía, y el ingreso funciona. Si
   una cuenta nueva no puede ingresar, se la suma a esa lista. El paso a producción
   está previsto en HT-10 (`plan.md`).
3. Confirmar que en Supabase el identificador de cliente **Web** figura primero en
   la lista del proveedor de Google.

Si la aplicación funciona en depuración y falla firmada, falta registrar la huella
SHA-1 de la clave de publicación en Google Cloud Console.

Si el mapa de «Mi dirección» aparece como un recuadro en blanco con el logotipo de
Google encima, el resto de la pantalla funciona pero las teselas no cargan. El
registro lo dice con todas las letras:

```bash
adb logcat -d | grep -i "Google Android Maps SDK"
```

`Authorization failure` significa una de tres cosas, en este orden de
probabilidad: **Maps SDK for Android** no está habilitado en el proyecto de Google
Cloud; `MAPS_API_KEY` está vacío o no existe en `local.properties`; o la clave
tiene restricción de API y no incluye ese servicio. La clave que trae
`app/google-services.json` **no sirve** para esto: es la de Firebase y responde
`Authorization failure`.

## Cuentas y accesos

Los servicios de Google viven en el proyecto `salud-en-casa-app`: Firebase, los
clientes OAuth y la clave de Maps. Tanto ese proyecto como la organización de
Supabase tienen **dos propietarios**, cada uno con verificación en dos pasos. Así
perder el acceso a una cuenta no deja a nadie fuera de la configuración
(`docs/decisions.md`, 2026-10-18).

La cuenta de facturación de Google tiene una alerta de presupuesto de USD 35 al
mes. Nació en prueba gratuita, que vence el **2027-01-18**: antes de esa fecha hay
que pulsar «Activar cuenta completa» en Facturación, o Google detiene el proyecto
y el mapa deja de cargar.

Al cerrar cada sprint se entra con las dos cuentas a Google Cloud y a Supabase.

## Estructura

```
app/src/main/java/bo/saludencasa/
├── core/            Errores, red, utilidades y objetos de valor transversales
├── di/              Módulos de inyección de dependencias
├── navigation/      Grafo de navegación
├── ui/              Tema, componentes y animaciones
└── features/        Un paquete por característica, con data, domain y presentation
```

`namespace` es `bo.saludencasa` y `applicationId` es `bo.saludencasa.app`. La
diferencia es deliberada y está explicada en `docs/decisions.md`.

## Convenciones

- Identificadores de código en inglés; documentos del proyecto en español.
- Ningún texto visible escrito en el código: todo en recursos de cadenas.
- Ninguna dependencia en versión `alpha`, `beta`, `rc` ni `SNAPSHOT`.
- Toda tabla se crea con sus políticas de seguridad en la misma migración.
- La suite de pruebas pasa completa en cada iteración.

El detalle está en `CLAUDE.md` y en `.claude/rules/`.

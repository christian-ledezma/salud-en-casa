# Salud en Casa

Aplicación móvil Android que vincula pacientes con profesionales de salud para
atención domiciliaria en Bolivia.

Proyecto de grado. El desarrollo sigue SCRUM con sprints de dos semanas.

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
```

Sin estos valores el proyecto **compila igual**, pero la aplicación muestra un
error de configuración al intentar ingresar. Es deliberado: una compilación que
falla por un archivo ausente no le dice nada útil a quien clona el proyecto.

Los valores se obtienen así:

| Valor | Dónde |
|---|---|
| `SUPABASE_URL` y `SUPABASE_ANON_KEY` | Panel de Supabase → Configuración → API |
| `GOOGLE_WEB_CLIENT_ID` | Google Cloud Console → Credenciales → ID de cliente OAuth de tipo **Web** |
| `MAPS_API_KEY` | Google Cloud Console → API y servicios → Credenciales |

La **clave de servicio** de Supabase no se usa en la aplicación y nunca debe
figurar en este archivo.

`GOOGLE_WEB_CLIENT_ID` es el identificador de tipo **Web**, no el de Android.
La aplicación pide un token destinado a su servidor, y ese servidor es Supabase.
Usar el de Android hace que Supabase rechace el ingreso. Está explicado en
`docs/decisions.md`.

**`app/google-services.json`**:

Consola de Firebase → Configuración del proyecto → Tus aplicaciones → descargar
el archivo de configuración. Se coloca en `app/`, no en la raíz.

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

En esta etapa se trabaja **contra el proyecto remoto de desarrollo**. El entorno
local con Docker (`supabase start`, `supabase db reset`) está fuera de alcance;
la razón está en `docs/decisions.md`.

## Comandos

| Comando | Qué hace |
|---|---|
| `./gradlew assembleDebug` | Compila la versión de depuración |
| `./gradlew test` | Pruebas unitarias |
| `./gradlew connectedAndroidTest` | Pruebas instrumentadas, requiere dispositivo |
| `./gradlew ktlintCheck` | Análisis estático |
| `./gradlew build` | Verificación completa |
| `./gradlew signingReport` | Muestra las huellas SHA-1 del proyecto |
| `npx supabase migration new <nombre>` | Crea una migración |
| `npx supabase db push --dry-run` | Muestra qué migraciones se aplicarían |
| `npx supabase db push` | Aplica las migraciones pendientes |

## Verificar la configuración

Si la autenticación con Google falla, el orden de revisión es:

1. `./gradlew signingReport` y comparar la huella SHA-1 de depuración contra la
   registrada en Google Cloud Console.
2. Confirmar que el correo de prueba figura en la lista de usuarios de prueba de
   la pantalla de consentimiento de OAuth. Mientras la aplicación esté en estado
   de prueba, ninguna otra cuenta puede ingresar.
3. Confirmar que en Supabase el identificador de cliente **Web** figura primero en
   la lista del proveedor de Google.

Si la aplicación funciona en depuración y falla firmada, falta registrar la huella
SHA-1 de la clave de publicación en Google Cloud Console.

## Estructura

```
app/src/main/java/bo/saludencasa/
├── core/            Errores, red y utilidades transversales
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

# Secuencia de autenticación con Google

Secuencia completa del ingreso, incluidos los caminos de error que la capa de
dominio modela como `AuthError`. Se deriva de los archivos de
`features/auth/`, de `handle_new_user()` en la migración `functions`, y de la
decisión de usar Credential Manager con intercambio de token de identidad
registrada en `docs/decisions.md` (2026-09-05).

**El recorrido está repartido entre tres piezas y ninguna hace más de lo suyo:**
`GoogleCredentialClient`, en la capa de presentación, es el único que habla con
Credential Manager y el único que necesita una actividad;
`SignInWithGoogleUseCase` valida el token y el nonce antes de que crucen al
dominio; `AuthRepository`, sobre `SupabaseAuthDataSource`, hace el intercambio
con Supabase y es el único lugar donde una excepción de la biblioteca se
convierte en un tipo de error. El motivo del reparto está en
`docs/decisions.md` (2026-09-12).

## Diagrama

```mermaid
sequenceDiagram
    actor U as Persona
    participant VM as WelcomeViewModel
    participant GC as GoogleCredentialClient
    participant CM as Credential Manager
    participant GIS as Servicios de identidad de Google
    participant UC as SignInWithGoogleUseCase
    participant Repo as AuthRepository
    participant Auth as Supabase Auth
    participant DB as PostgreSQL

    U->>VM: pulsa "Continuar con Google"
    VM->>GC: requestIdToken(actividad)

    alt falta el identificador de cliente Web
        GC-->>VM: AuthError.MissingConfiguration
    else configuracion presente
        GC->>GC: genera un nonce y su hash SHA-256
        GC->>CM: getCredential(nonce hasheado, id de cliente Web)
        CM->>GIS: solicita una cuenta de Google
        GIS-->>U: muestra el selector de cuenta
        U-->>GIS: elige una cuenta o cancela

        alt cancela el selector
            GIS-->>GC: GetCredentialCancellationException
            GC-->>VM: AuthError.Cancelled
            VM-->>U: vuelve al estado de reposo, sin error
        else sin cuenta de Google en el dispositivo
            GIS-->>GC: NoCredentialException
            GC-->>VM: AuthError.NoGoogleAccount
        else credencial de un tipo inesperado
            GIS-->>GC: credencial que no es GoogleIdTokenCredential
            GC-->>VM: AuthError.CredentialProviderFailure
        else credencial obtenida
            GIS-->>CM: credencial con el ID token
            CM-->>GC: GoogleIdTokenCredential
            GC-->>VM: token + nonce crudo
            VM->>UC: invoke(idToken, rawNonce)

            alt token o nonce vacios
                UC-->>VM: AuthError.CredentialProviderFailure
            else precondiciones cumplidas
                UC->>Repo: signInWithGoogle(idToken, rawNonce)

                alt faltan las credenciales de Supabase
                    Repo-->>UC: AuthError.MissingConfiguration
                else configuracion presente
                    Repo->>Auth: signInWith(IDToken), token + nonce sin hashear
                    Auth->>GIS: verifica la firma y el nonce hasheado
                    GIS-->>Auth: resultado de la verificacion

                    alt la red falla o expira el tiempo de espera
                        Auth-->>Repo: HttpRequestException / HttpRequestTimeoutException
                        Repo-->>UC: AuthError.NetworkUnavailable
                    else token invalido o Supabase lo rechaza
                        Auth-->>Repo: RestException o sin usuario autenticado
                        Repo-->>UC: AuthError.TokenRejected
                    else token aceptado

                        alt primer ingreso
                            Auth->>DB: inserta en auth.users
                            DB->>DB: dispara handle_new_user()
                            DB->>DB: inserta en profiles, role nulo
                        end

                        Auth-->>Repo: sesion con userId
                        Repo->>Repo: DataStoreSessionManager guarda la sesion
                        Repo-->>UC: AuthResult.Success(AuthSession)
                        UC-->>VM: AuthResult.Success
                        VM-->>U: navega a "Mi cuenta"
                    end
                end
            end
        end
    end
```

## El nonce cruzado

Google firma sobre el **hash** del nonce; Supabase verifica contra el **valor
crudo**. `GoogleCredentialClient` genera ambos con `Nonce.generate()` y los
envía cada uno a quien lo espera: el hasheado a `GetGoogleIdOption.setNonce()`,
el crudo viaja hasta `signInWith(IDToken) { nonce = ... }`. Invertirlos produce
un rechazo silencioso, no un error legible, así que hay dos pruebas que lo
fijan: `NonceTest` comprueba que el hash es SHA-256 en minúsculas y que cada
nonce crudo se empareja con el suyo, y `SignInWithGoogleUseCaseTest` comprueba
que el caso de uso entrega al repositorio el token y el nonce crudo en ese
orden, sin intercambiarlos.

## Por qué el identificador de cliente es el Web

`GetGoogleIdOption.setServerClientId()` recibe el identificador de cliente
**Web**, no el de Android, porque el token va dirigido a Supabase —el
servidor—, no a esta aplicación. Usar el de Android hace que Supabase rechace
el token con `AuthError.TokenRejected`. Registrado en `docs/decisions.md`
(2026-09-05).

## Dónde nace `profiles`

`handle_new_user()` es un disparador sobre `auth.users`, no una llamada que
haga el cliente: la fila de `profiles` aparece sola en el primer ingreso, con
`role` nulo hasta que la persona elige si es paciente o profesional (HU-02). Si
alguna vez `profiles` no se crea al ingresar por primera vez, el disparador —no
el cliente— es lo primero que hay que revisar.

## Los errores y dónde se traducen

`AuthError` es un tipo sellado sin ninguna frase.
`features/auth/presentation/AuthErrorMessages.kt` traduce cada variante a una
clave de recurso, nunca al revés, de modo que la regla de negocio no conoce el
idioma del usuario (`.claude/rules/i18n.md`).

| Variante | Cuándo ocurre | Clave de recurso |
|---|---|---|
| `MissingConfiguration` | `local.properties` no tiene las credenciales | `error_sign_in_missing_configuration` |
| `Cancelled` | La persona cierra el selector de cuenta | ninguna: el modelo de vista lo convierte en reposo antes de llegar a la pantalla |
| `NoGoogleAccount` | El dispositivo no tiene ninguna cuenta de Google | `error_sign_in_no_google_account` |
| `CredentialProviderFailure` | Credential Manager falla, devuelve un tipo de credencial inesperado, o entrega un token vacío | `error_sign_in_credential_provider_failure` |
| `NetworkUnavailable` | La petición no llega a Supabase, o expira el tiempo de espera | `error_network_unavailable` |
| `TokenRejected` | Supabase no acepta el token | `error_sign_in_token_rejected` |
| `Unexpected` | Cualquier otra falla | `error_unexpected` |

**`Cancelled` es el único que nunca se muestra.** Cerrar el selector es una
decisión, no un fallo: `WelcomeViewModel` lo convierte en el estado de reposo y
la pantalla vuelve intacta, con el botón disponible. Es un criterio de
aceptación de HU-01 y `WelcomeViewModelTest` lo fija.

**Cuidado con el tiempo de espera agotado.** supabase-kt relanza
`HttpRequestTimeoutException` sin envolverla y solo envuelve las demás fallas de
transporte en su propia `HttpRequestException`, así que ambas tienen que
reconocerse por separado para que una conexión lenta no se presente como una
falla inexplicable. `AuthErrorMapperTest` lo fija. Registrado en
`docs/decisions.md` (2026-09-13).

## Qué ocurre al cerrar sesión

`SignOutUseCase` llega al mismo repositorio. El alcance del cierre es local, y
cuando la petición al servidor no llega —sin red o con el tiempo agotado— el
repositorio limpia igual la sesión guardada y lo informa como éxito, porque de
lo contrario la persona pulsaría «Cerrar sesión» y volvería a encontrar su
cuenta abierta. Registrado en `docs/decisions.md` (2026-09-12).

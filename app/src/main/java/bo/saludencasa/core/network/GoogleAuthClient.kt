package bo.saludencasa.core.network

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import bo.saludencasa.core.util.Nonce
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface SessionState {
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(
        val userId: String,
    ) : SessionState
}

sealed interface SignInResult {
    data class Success(
        val userId: String,
    ) : SignInResult

    data class Failure(
        val error: SignInError,
    ) : SignInResult
}

sealed interface SignInError {
    data object MissingConfiguration : SignInError

    data object Cancelled : SignInError

    data object NoGoogleAccount : SignInError

    data object CredentialManagerFailure : SignInError

    data object TokenRejected : SignInError
}

// Web client identifier, not Android: the token must be addressed to Supabase,
// not to this application (docs/decisions.md, 2026-09-05).
class GoogleAuthClient(
    private val supabase: SupabaseClient,
    private val supabaseUrl: String,
    private val supabaseAnonKey: String,
    private val webClientId: String,
) {
    // Sessions restore asynchronously on start up. A single read at
    // construction time always runs before that finishes and reports nobody
    // signed in, which is why this is a flow instead of a one-shot value.
    val sessionState: Flow<SessionState> =
        supabase.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    status.session.user
                        ?.id
                        ?.let(SessionState::SignedIn)
                        ?: SessionState.SignedOut
                }

                SessionStatus.Initializing -> {
                    SessionState.Loading
                }

                else -> {
                    SessionState.SignedOut
                }
            }
        }

    suspend fun signInWithGoogle(activityContext: Context): SignInResult {
        if (webClientId.isBlank() || supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            return SignInResult.Failure(SignInError.MissingConfiguration)
        }

        val nonce = Nonce.generate()
        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(
                    GetGoogleIdOption
                        .Builder()
                        // False so a first time user still sees their accounts
                        // instead of an empty chooser.
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        // Google signs over the hash; Supabase verifies the raw value.
                        .setNonce(nonce.hashed)
                        .build(),
                ).build()

        val idToken =
            try {
                val response =
                    CredentialManager
                        .create(activityContext)
                        .getCredential(activityContext, request)
                val credential = response.credential
                if (credential !is CustomCredential ||
                    credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    return SignInResult.Failure(SignInError.CredentialManagerFailure)
                }
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (_: GetCredentialCancellationException) {
                return SignInResult.Failure(SignInError.Cancelled)
            } catch (_: NoCredentialException) {
                return SignInResult.Failure(SignInError.NoGoogleAccount)
            } catch (_: GetCredentialException) {
                return SignInResult.Failure(SignInError.CredentialManagerFailure)
            }

        return try {
            supabase.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
                this.nonce = nonce.raw
            }
            val userId =
                supabase.auth.currentUserOrNull()?.id
                    ?: return SignInResult.Failure(SignInError.TokenRejected)
            SignInResult.Success(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            SignInResult.Failure(SignInError.TokenRejected)
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}

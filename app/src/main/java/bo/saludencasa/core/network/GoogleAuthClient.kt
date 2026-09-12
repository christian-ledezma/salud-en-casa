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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Whether somebody is signed in. [Loading] is not a detail: the client restores
 * the stored session asynchronously on start up, so asking once and synchronously
 * always answers "nobody", and the session looks lost after every restart.
 */
sealed interface SessionState {
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(
        val userId: String,
    ) : SessionState
}

/**
 * Result of the Google sign in exchange. Expected conditions are modelled as a
 * sealed hierarchy instead of thrown exceptions.
 */
sealed interface SignInResult {
    data class Success(
        val userId: String,
    ) : SignInResult

    data class Failure(
        val error: SignInError,
    ) : SignInResult
}

/**
 * Why a sign in did not succeed. Each case carries a type, never a sentence:
 * the presentation layer is what turns it into a string resource. A message
 * built here would be stuck in one language and in one screen's wording.
 */
sealed interface SignInError {
    /** local.properties has no Google web client identifier or no Supabase key. */
    data object MissingConfiguration : SignInError

    /** The person dismissed the account chooser. */
    data object Cancelled : SignInError

    /** The device has no Google account that can be offered. */
    data object NoGoogleAccount : SignInError

    /** Credential Manager refused or failed to produce a credential. */
    data object CredentialManagerFailure : SignInError

    /** Supabase did not accept the identity token. */
    data object TokenRejected : SignInError
}

/**
 * Signs in with Google through Credential Manager and exchanges the resulting
 * identity token with Supabase (RF-01.1, RF-01.2).
 *
 * The client identifier handed to Credential Manager is the **Web** one, not
 * the Android one: the application asks for a token addressed to its server,
 * and that server is Supabase. Registered in docs/decisions.md.
 */
class GoogleAuthClient(
    private val supabase: SupabaseClient,
    private val webClientId: String,
) {
    /**
     * Emits again whenever the session changes, including the moment the stored
     * one finishes loading. Observing this is what makes RF-01.6 hold.
     */
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
        if (webClientId.isBlank()) return SignInResult.Failure(SignInError.MissingConfiguration)

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
        } catch (_: Exception) {
            SignInResult.Failure(SignInError.TokenRejected)
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}

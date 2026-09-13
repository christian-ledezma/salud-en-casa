package bo.saludencasa.features.auth.presentation

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import bo.saludencasa.core.util.Nonce
import bo.saludencasa.features.auth.domain.model.AuthError
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed interface GoogleCredentialResult {
    data class Success(
        val idToken: String,
        val rawNonce: String,
    ) : GoogleCredentialResult

    data class Failure(
        val error: AuthError,
    ) : GoogleCredentialResult
}

// The client identifier handed to Credential Manager is the Web one, not the
// Android one (docs/decisions.md, 2026-09-05).
class GoogleCredentialClient(
    private val webClientId: String,
) {
    // Credential Manager needs the activity to raise the account chooser, which
    // is why the context arrives per call instead of being held: this class
    // outlives any single screen, the activity does not.
    suspend fun requestIdToken(activityContext: Context): GoogleCredentialResult {
        if (webClientId.isBlank()) return GoogleCredentialResult.Failure(AuthError.MissingConfiguration)

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

        return try {
            val response = CredentialManager.create(activityContext).getCredential(activityContext, request)
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleCredentialResult.Failure(AuthError.CredentialProviderFailure)
            } else {
                GoogleCredentialResult.Success(
                    idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken,
                    rawNonce = nonce.raw,
                )
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleCredentialResult.Failure(AuthError.Cancelled)
        } catch (_: NoCredentialException) {
            GoogleCredentialResult.Failure(AuthError.NoGoogleAccount)
        } catch (_: GetCredentialException) {
            GoogleCredentialResult.Failure(AuthError.CredentialProviderFailure)
        }
    }
}

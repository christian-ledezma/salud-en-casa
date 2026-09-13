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

class GoogleCredentialClient(
    private val webClientId: String,
) {
    suspend fun requestIdToken(activityContext: Context): GoogleCredentialResult {
        if (webClientId.isBlank()) return GoogleCredentialResult.Failure(AuthError.MissingConfiguration)

        val nonce = Nonce.generate()
        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(
                    GetGoogleIdOption
                        .Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
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

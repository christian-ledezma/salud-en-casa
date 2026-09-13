package bo.saludencasa.features.auth.domain.usecase

import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthResult
import bo.saludencasa.features.auth.domain.repository.IAuthRepository

class SignInWithGoogleUseCase(
    private val authRepository: IAuthRepository,
) {
    // The two strings cross into the domain from Credential Manager, which
    // reports success with an empty token when the account chooser returns a
    // credential it could not read. Sending that on produces a rejection from
    // Supabase that blames the provider configuration instead.
    suspend operator fun invoke(
        idToken: String,
        rawNonce: String,
    ): AuthResult =
        if (idToken.isBlank() || rawNonce.isBlank()) {
            AuthResult.Failure(AuthError.CredentialProviderFailure)
        } else {
            authRepository.signInWithGoogle(idToken = idToken, rawNonce = rawNonce)
        }
}

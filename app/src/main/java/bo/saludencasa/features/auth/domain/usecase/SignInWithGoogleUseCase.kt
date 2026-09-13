package bo.saludencasa.features.auth.domain.usecase

import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthResult
import bo.saludencasa.features.auth.domain.repository.IAuthRepository

class SignInWithGoogleUseCase(
    private val authRepository: IAuthRepository,
) {
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

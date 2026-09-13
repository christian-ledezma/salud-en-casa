package bo.saludencasa.features.auth.domain.usecase

import bo.saludencasa.features.auth.domain.model.SignOutResult
import bo.saludencasa.features.auth.domain.repository.IAuthRepository

class SignOutUseCase(
    private val authRepository: IAuthRepository,
) {
    suspend operator fun invoke(): SignOutResult = authRepository.signOut()
}

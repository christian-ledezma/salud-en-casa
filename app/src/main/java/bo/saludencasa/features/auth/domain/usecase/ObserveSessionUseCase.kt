package bo.saludencasa.features.auth.domain.usecase

import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase(
    private val authRepository: IAuthRepository,
) {
    operator fun invoke(): Flow<SessionState> = authRepository.observeSession()
}

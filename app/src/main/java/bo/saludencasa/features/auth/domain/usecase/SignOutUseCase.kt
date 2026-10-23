package bo.saludencasa.features.auth.domain.usecase

import bo.saludencasa.features.auth.domain.model.SignOutResult
import bo.saludencasa.features.auth.domain.repository.IAuthRepository
import bo.saludencasa.features.notifications.domain.usecase.StartReceivingNotificationsUseCase
import bo.saludencasa.features.notifications.domain.usecase.StopReceivingNotificationsUseCase

class SignOutUseCase(
    private val authRepository: IAuthRepository,
    private val stopReceivingNotifications: StopReceivingNotificationsUseCase,
    private val startReceivingNotifications: StartReceivingNotificationsUseCase,
) {
    // docs/decisions.md, 2026-10-16: the device goes before the session does.
    suspend operator fun invoke(): SignOutResult {
        stopReceivingNotifications()
        val result = authRepository.signOut()
        if (result is SignOutResult.Failure) startReceivingNotifications()
        return result
    }
}

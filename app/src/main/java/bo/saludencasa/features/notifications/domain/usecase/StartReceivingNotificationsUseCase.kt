package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.repository.INotificationRepository

// docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 1.
class StartReceivingNotificationsUseCase(
    private val repository: INotificationRepository,
) {
    suspend operator fun invoke() {
        repository.requestRegistration()
    }
}

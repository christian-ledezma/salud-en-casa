package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.repository.INotificationRepository

// docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 3.
class StopReceivingNotificationsUseCase(
    private val repository: INotificationRepository,
) {
    suspend operator fun invoke() {
        repository.storedInstallationId()?.let { repository.removeDevice(it) }
        repository.stopRegistration()
    }
}

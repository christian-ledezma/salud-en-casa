package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.model.DeviceResult
import bo.saludencasa.features.notifications.domain.repository.INotificationRepository

class RegisterDeviceUseCase(
    private val repository: INotificationRepository,
) {
    suspend operator fun invoke(installationId: String): DeviceResult {
        repository.saveInstallationId(installationId)
        return repository.uploadDevice(installationId)
    }
}

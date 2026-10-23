package bo.saludencasa.features.notifications.domain.repository

import bo.saludencasa.features.notifications.domain.model.DeviceResult
import kotlinx.coroutines.flow.Flow

interface INotificationRepository {
    suspend fun awaitSignedInUserId(): String?

    fun requestRegistration()

    suspend fun saveInstallationId(installationId: String)

    suspend fun storedInstallationId(): String?

    suspend fun uploadDevice(installationId: String): DeviceResult

    suspend fun removeDevice(installationId: String): DeviceResult

    suspend fun stopRegistration()

    fun observePromptAnswered(): Flow<Boolean>

    suspend fun markPromptAnswered()
}

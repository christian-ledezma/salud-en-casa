package bo.saludencasa.features.notifications.data.repository

import bo.saludencasa.features.notifications.data.datasource.FcmRegistrationDataSource
import bo.saludencasa.features.notifications.data.datasource.NotificationPreferencesDataSource
import bo.saludencasa.features.notifications.data.datasource.ShownNotificationsDataSource
import bo.saludencasa.features.notifications.data.datasource.SupabaseDeviceDataSource
import bo.saludencasa.features.notifications.data.mapper.toNotificationError
import bo.saludencasa.features.notifications.domain.model.DeviceResult
import bo.saludencasa.features.notifications.domain.model.NotificationError
import bo.saludencasa.features.notifications.domain.model.NotificationSlot
import bo.saludencasa.features.notifications.domain.repository.INotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicReference

class NotificationRepository(
    private val deviceDataSource: SupabaseDeviceDataSource,
    private val registration: FcmRegistrationDataSource,
    private val preferences: NotificationPreferencesDataSource,
    private val shownNotifications: ShownNotificationsDataSource,
) : INotificationRepository {
    private val openConversation = AtomicReference<String?>(null)

    override suspend fun awaitSignedInUserId(): String? = deviceDataSource.awaitUserId()

    override fun requestRegistration() {
        registration.register()
    }

    override suspend fun saveInstallationId(installationId: String) {
        preferences.saveInstallationId(installationId)
    }

    override suspend fun storedInstallationId(): String? = preferences.installationId()

    override suspend fun uploadDevice(installationId: String): DeviceResult {
        if (deviceDataSource.currentUserId() == null) {
            return DeviceResult.Failure(NotificationError.NotSignedIn)
        }
        return device { deviceDataSource.register(installationId) }
    }

    override suspend fun removeDevice(installationId: String): DeviceResult =
        device { deviceDataSource.unregister(installationId) }

    // docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 3.
    override suspend fun stopRegistration() {
        registration.unregister()
        preferences.clearInstallationId()
        shownNotifications.clearAll()
    }

    override fun observePromptAnswered(): Flow<Boolean> = preferences.promptAnswered()

    override suspend fun markPromptAnswered() {
        preferences.markPromptAnswered()
    }

    override fun enterConversation(requestId: String) {
        openConversation.set(requestId)
        shownNotifications.clear(requestId, NotificationSlot.MESSAGE)
    }

    // Only the screen that entered clears it: a conversation opened over another
    // pauses the first after the second has already entered.
    override fun leaveConversation(requestId: String) {
        openConversation.compareAndSet(requestId, null)
    }

    override fun openConversation(): String? = openConversation.get()

    private inline fun device(call: () -> Unit): DeviceResult =
        try {
            call()
            DeviceResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            DeviceResult.Failure(failure.toNotificationError())
        }
}

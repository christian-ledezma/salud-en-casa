package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.FakeNotificationRepository
import bo.saludencasa.features.notifications.domain.model.DeviceResult
import bo.saludencasa.features.notifications.domain.model.NotificationError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StopReceivingNotificationsUseCaseTest {
    @Test
    fun removesTheServerRowAndThenStopsThePlatformRegistration() =
        runTest {
            val repository = FakeNotificationRepository(storedInstallationId = "fid-1")

            StopReceivingNotificationsUseCase(repository)()

            assertEquals(listOf("remove:fid-1", "stopRegistration"), repository.calls)
        }

    // Offline the server never hears about it; unregistering the installation
    // still makes FCM answer UNREGISTERED, and the send function prunes the row.
    @Test
    fun stillStopsThePlatformRegistrationWhenTheServerCallFails() =
        runTest {
            val repository =
                FakeNotificationRepository(
                    storedInstallationId = "fid-1",
                    removeResult = DeviceResult.Failure(NotificationError.NetworkUnavailable),
                )

            StopReceivingNotificationsUseCase(repository)()

            assertEquals("stopRegistration", repository.calls.last())
        }

    @Test
    fun withNothingRegisteredOnlyThePlatformIsStopped() =
        runTest {
            val repository = FakeNotificationRepository(storedInstallationId = null)

            StopReceivingNotificationsUseCase(repository)()

            assertEquals(listOf("stopRegistration"), repository.calls)
        }
}

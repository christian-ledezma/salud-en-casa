package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.FakeNotificationRepository
import bo.saludencasa.features.notifications.domain.model.DeviceResult
import bo.saludencasa.features.notifications.domain.model.NotificationError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RegisterDeviceUseCaseTest {
    // A failed upload still leaves the installation id where signing out will
    // look for it; otherwise a row that did reach the server could never be
    // removed from this phone.
    @Test
    fun keepsTheInstallationIdWhenTheUploadFails() =
        runTest {
            val repository =
                FakeNotificationRepository(uploadResult = DeviceResult.Failure(NotificationError.NetworkUnavailable))

            RegisterDeviceUseCase(repository)("fid-2")

            assertEquals(listOf("save:fid-2", "upload:fid-2"), repository.calls)
            assertEquals("fid-2", repository.storedInstallationId)
        }
}

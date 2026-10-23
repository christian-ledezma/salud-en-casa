package bo.saludencasa.features.auth.domain.usecase

import bo.saludencasa.features.auth.FakeAuthRepository
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.SignOutResult
import bo.saludencasa.features.notifications.FakeNotificationRepository
import bo.saludencasa.features.notifications.domain.model.DeviceResult
import bo.saludencasa.features.notifications.domain.model.NotificationError
import bo.saludencasa.features.notifications.domain.usecase.StartReceivingNotificationsUseCase
import bo.saludencasa.features.notifications.domain.usecase.StopReceivingNotificationsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SignOutUseCaseTest {
    // signingOutStopsNotificationsOnThatDevice: unregister_my_device needs the
    // JWT, so the row has to go while the session still exists.
    @Test
    fun signingOutRemovesTheDeviceBeforeTheSessionCloses() =
        runTest {
            val auth = FakeAuthRepository()
            var signOutsWhenRemoved = -1
            val notifications =
                FakeNotificationRepository(
                    storedInstallationId = "fid-1",
                    onRemoveDevice = { signOutsWhenRemoved = auth.signOutAttempts },
                )

            signOut(auth, notifications)()

            assertEquals(0, signOutsWhenRemoved)
            assertEquals(1, auth.signOutAttempts)
        }

    // A phone without network still signs out.
    @Test
    fun signOutProceedsWhenTheDeviceCannotBeRemoved() =
        runTest {
            val auth = FakeAuthRepository()
            val notifications =
                FakeNotificationRepository(
                    storedInstallationId = "fid-1",
                    removeResult = DeviceResult.Failure(NotificationError.NetworkUnavailable),
                )

            val result = signOut(auth, notifications)()

            assertEquals(SignOutResult.Success, result)
            assertEquals(1, auth.signOutAttempts)
        }

    // A sign-out the server refused leaves the person signed in, and a phone
    // already unregistered would stay silent until the next cold start.
    @Test
    fun aFailedSignOutStartsReceivingNotificationsAgain() =
        runTest {
            val auth = FakeAuthRepository(signOutResult = SignOutResult.Failure(AuthError.Unexpected))
            val notifications = FakeNotificationRepository(storedInstallationId = "fid-1")

            signOut(auth, notifications)()

            assertEquals(listOf("remove:fid-1", "stopRegistration", "requestRegistration"), notifications.calls)
        }

    private fun signOut(
        auth: FakeAuthRepository,
        notifications: FakeNotificationRepository,
    ) = SignOutUseCase(
        auth,
        StopReceivingNotificationsUseCase(notifications),
        StartReceivingNotificationsUseCase(notifications),
    )
}

package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.FakeNotificationRepository
import bo.saludencasa.features.notifications.OTHER_USER
import bo.saludencasa.features.notifications.domain.model.DiscardReason
import bo.saludencasa.features.notifications.domain.model.IncomingNotification
import bo.saludencasa.features.notifications.nearbyRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveIncomingNotificationUseCaseTest {
    @Test
    fun showsANotificationAddressedToWhoeverIsSignedIn() =
        runTest {
            val payload = nearbyRequest()

            val incoming = ResolveIncomingNotificationUseCase(FakeNotificationRepository())(payload.toMap())

            assertEquals(IncomingNotification.Show(payload), incoming)
        }

    // RF-09.12: someone else signed in on this phone after the server last
    // heard about it. Their lock screen must not show another person's event.
    @Test
    fun discardsANotificationAddressedToAnotherAccount() =
        runTest {
            val incoming =
                ResolveIncomingNotificationUseCase(FakeNotificationRepository())(
                    nearbyRequest(recipientId = OTHER_USER).toMap(),
                )

            assertEquals(IncomingNotification.Discarded(DiscardReason.OTHER_ACCOUNT), incoming)
        }

    // Signing out offline leaves the row on the server; the phone still shows
    // nothing (signingOutStopsNotificationsOnThatDevice, the device half).
    @Test
    fun discardsEverythingWhileSignedOut() =
        runTest {
            val repository = FakeNotificationRepository(signedInUserId = null)

            val incoming = ResolveIncomingNotificationUseCase(repository)(nearbyRequest().toMap())

            assertEquals(IncomingNotification.Discarded(DiscardReason.NO_SESSION), incoming)
        }

    @Test
    fun discardsAMalformedPayload() =
        runTest {
            val incoming = ResolveIncomingNotificationUseCase(FakeNotificationRepository())(mapOf("event" to "X"))

            assertEquals(IncomingNotification.Discarded(DiscardReason.MALFORMED), incoming)
        }
}

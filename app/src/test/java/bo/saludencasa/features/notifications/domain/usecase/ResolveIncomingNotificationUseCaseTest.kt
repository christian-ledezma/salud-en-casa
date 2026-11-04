package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.FakeNotificationRepository
import bo.saludencasa.features.notifications.OTHER_USER
import bo.saludencasa.features.notifications.REQUEST_ID
import bo.saludencasa.features.notifications.domain.model.DiscardReason
import bo.saludencasa.features.notifications.domain.model.IncomingNotification
import bo.saludencasa.features.notifications.messageReceived
import bo.saludencasa.features.notifications.nearbyRequest
import bo.saludencasa.features.notifications.offerAccepted
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

    // HU-21, criterion 5: the message is already on screen.
    @Test
    fun aMessageOfTheConversationOnScreenIsNotShown() =
        runTest {
            val repository = FakeNotificationRepository().apply { enterConversation(REQUEST_ID) }

            val incoming = ResolveIncomingNotificationUseCase(repository)(messageReceived().toMap())

            assertEquals(IncomingNotification.Discarded(DiscardReason.CONVERSATION_OPEN), incoming)
        }

    @Test
    fun aMessageOfAnotherConversationIsShown() =
        runTest {
            val repository = FakeNotificationRepository().apply { enterConversation(OTHER_REQUEST_ID) }
            val payload = messageReceived()

            val incoming = ResolveIncomingNotificationUseCase(repository)(payload.toMap())

            assertEquals(IncomingNotification.Show(payload), incoming)
        }

    // Only a message is already on screen; any other event of that request is not.
    @Test
    fun anotherEventOfTheRequestOnScreenIsStillShown() =
        runTest {
            val repository = FakeNotificationRepository().apply { enterConversation(REQUEST_ID) }
            val payload = offerAccepted()

            val incoming = ResolveIncomingNotificationUseCase(repository)(payload.toMap())

            assertEquals(IncomingNotification.Show(payload), incoming)
        }

    // Leaving the conversation is what lets the next message ring the phone.
    @Test
    fun aMessageIsShownAgainOnceTheConversationIsLeft() =
        runTest {
            val repository =
                FakeNotificationRepository().apply {
                    enterConversation(REQUEST_ID)
                    leaveConversation(REQUEST_ID)
                }
            val payload = messageReceived()

            val incoming = ResolveIncomingNotificationUseCase(repository)(payload.toMap())

            assertEquals(IncomingNotification.Show(payload), incoming)
        }

    @Test
    fun discardsAMalformedPayload() =
        runTest {
            val incoming = ResolveIncomingNotificationUseCase(FakeNotificationRepository())(mapOf("event" to "X"))

            assertEquals(IncomingNotification.Discarded(DiscardReason.MALFORMED), incoming)
        }

    private companion object {
        const val OTHER_REQUEST_ID = "b1c2d3e4-0000-4000-8000-000000000000"
    }
}

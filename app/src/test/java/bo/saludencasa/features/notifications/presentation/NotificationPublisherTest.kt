package bo.saludencasa.features.notifications.presentation

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.features.notifications.REQUEST_ID
import bo.saludencasa.features.notifications.SIGNED_IN_USER
import bo.saludencasa.features.notifications.appointmentCancelled
import bo.saludencasa.features.notifications.appointmentScheduled
import bo.saludencasa.features.notifications.documentVerdict
import bo.saludencasa.features.notifications.messageReceived
import bo.saludencasa.features.notifications.nearbyRequest
import bo.saludencasa.features.notifications.offerAccepted
import bo.saludencasa.features.notifications.pushSubject
import bo.saludencasa.features.notifications.verificationApproved
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class NotificationPublisherTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        NotificationChannelSpec.registerAll(context)
    }

    private fun grant() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun posted(): List<Notification> = shadowOf(manager).allNotifications

    // The scheduled appointment and its cancellation are one story; two
    // notifications for it would leave the stale one standing.
    @Test
    fun twoEventsOfTheSameRequestLeaveOneNotification() {
        grant()
        val publisher = NotificationPublisher(context)

        publisher.show(appointmentScheduled())
        publisher.show(appointmentCancelled())

        assertEquals(1, posted().size)
    }

    // HU-21: a message has its own slot, so it does not erase the acceptance of
    // the same request, and each tap still opens its own event.
    @Test
    fun aMessageNeverReplacesAnotherEventOfTheSameRequest() {
        grant()
        val publisher = NotificationPublisher(context)
        val accepted = offerAccepted()
        val message = messageReceived()

        publisher.show(accepted)
        publisher.show(message)

        val opened = posted().map { shadowOf(it.contentIntent).savedIntent.toPushPayload() }.toSet()
        assertEquals(setOf(accepted, message), opened)
    }

    // A second message replaces the first instead of piling up.
    @Test
    fun twoMessagesOfTheSameConversationLeaveOneNotification() {
        grant()
        val publisher = NotificationPublisher(context)

        publisher.show(messageReceived(senderName = "Ana Quispe"))
        publisher.show(messageReceived(senderName = "Ana Quispe"))

        assertEquals(1, posted().size)
    }

    // The lock screen says a message arrived, never who wrote it.
    @Test
    fun theLockScreenVersionOfAMessageNamesNoSender() {
        grant()

        NotificationPublisher(context).show(messageReceived(senderName = "Ana Quispe"))

        val publicVersion = posted().single().publicVersion.extras
        assertFalse(publicVersion.getCharSequence(Notification.EXTRA_TITLE).toString().contains("Ana"))
        assertFalse(publicVersion.getCharSequence(Notification.EXTRA_TEXT).toString().contains("Ana"))
    }

    // RF-09.13: a denied permission is a phone that keeps working, quietly.
    @Test
    fun postsNothingWithoutThePermission() {
        NotificationPublisher(context).show(nearbyRequest())

        assertTrue(posted().isEmpty())
    }

    @Test
    fun theLockScreenGetsTheReservedVersion() {
        grant()

        NotificationPublisher(context).show(nearbyRequest())

        val notification = posted().single()
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        val publicText =
            notification.publicVersion.extras
                .getCharSequence(Notification.EXTRA_TEXT)
                .toString()
        assertFalse(publicText.contains(checkNotNull(pushSubject().serviceTypeName)))
    }

    // Tapping opens the request the notification is about, and the intent
    // carries the payload and nothing a user wrote.
    @Test
    fun theTapOpensTheRequestItIsAbout() {
        grant()
        val payload = nearbyRequest()

        NotificationPublisher(context).show(payload)

        val intent = shadowOf(posted().single().contentIntent).savedIntent
        assertEquals(payload, intent.toPushPayload())
        assertEquals(REQUEST_ID, intent.data?.lastPathSegment)
    }

    // Neither HU-37 event has a request, so the recipient keeps a second
    // verdict for the same person from piling up beside the first.
    @Test
    fun twoVerificationEventsOfTheSamePersonLeaveOneNotification() {
        grant()
        val publisher = NotificationPublisher(context)

        publisher.show(documentVerdict())
        publisher.show(verificationApproved())

        assertEquals(1, posted().size)
        val intent = shadowOf(posted().single().contentIntent).savedIntent
        assertEquals(SIGNED_IN_USER, intent.data?.lastPathSegment)
    }
}

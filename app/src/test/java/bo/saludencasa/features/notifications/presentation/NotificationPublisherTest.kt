package bo.saludencasa.features.notifications.presentation

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.features.notifications.REQUEST_ID
import bo.saludencasa.features.notifications.appointmentCancelled
import bo.saludencasa.features.notifications.appointmentScheduled
import bo.saludencasa.features.notifications.nearbyRequest
import bo.saludencasa.features.notifications.pushSubject
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
        assertFalse(publicText.contains(pushSubject().serviceTypeName))
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
}

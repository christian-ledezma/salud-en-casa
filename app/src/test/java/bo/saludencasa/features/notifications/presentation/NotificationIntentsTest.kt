package bo.saludencasa.features.notifications.presentation

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.features.notifications.nearbyRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationIntentsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun aNotificationIntentGivesItsPayloadBack() {
        val payload = nearbyRequest()

        assertEquals(payload, notificationIntent(context, payload).toPushPayload())
    }

    // Reopening from the recents screen hands back the last launch intent; a
    // tap already handled would open the inbox again on every return.
    @Test
    fun anIntentRelaunchedFromHistoryIsNotATap() {
        val intent =
            notificationIntent(context, nearbyRequest()).apply {
                addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
            }

        assertNull(intent.toPushPayload())
    }

    @Test
    fun theLauncherIntentIsNotATap() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        assertNull(launcher.toPushPayload())
    }

    // A tap still pending when the activity is recreated survives in its
    // saved state.
    @Test
    fun aPendingTapSurvivesTheSavedState() {
        val payload = nearbyRequest()

        assertEquals(payload, payload.toBundle().toPushPayload())
    }
}

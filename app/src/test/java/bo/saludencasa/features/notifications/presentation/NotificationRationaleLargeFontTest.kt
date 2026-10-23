package bo.saludencasa.features.notifications.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationRationaleLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // The card lives inside screens that scroll, so it is tested inside one.
    private fun show(body: Int) {
        composeRule.setContentAtDoubleFontScale {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                NotificationRationale(body = body, onEnable = {}, onDismiss = {})
            }
        }
    }

    @Test
    fun theExplanationStaysOnScreen() {
        show(R.string.notification_permission_body_professional)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.notification_permission_title)))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.notification_permission_body_professional)),
        )
    }

    @Test
    fun bothChoicesStayOnScreen() {
        show(R.string.notification_permission_body_patient)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.notification_permission_enable)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.notification_permission_dismiss)))
    }
}

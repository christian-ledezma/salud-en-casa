package bo.saludencasa.features.auth.presentation

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show() {
        composeRule.setContentAtDoubleFontScale {
            WelcomeContent(uiState = WelcomeUiState.Idle, onSignInClick = {}, onTermsClick = {}, onPrivacyClick = {})
        }
    }

    // Nothing is pinned to the foot of the screen, so the way in is wherever the
    // content ends: at 200 %, below the brand and the three points.
    @Test
    fun theWayInStaysReachable() {
        show()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.auth_welcome_sign_in_with_google)))
    }

    @Test
    fun theTermsAndThePrivacyPolicyStayReachable() {
        show()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.auth_welcome_terms)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.auth_welcome_privacy)))
    }

    // At 200 % on 320 dp the label wraps to two lines. A button fixed at 56 dp
    // stays 56 dp tall and cuts the second line inside itself, which no bound
    // outside the button shows; the button has to grow instead.
    @Test
    fun theSignInButtonGrowsWithItsLabelInsteadOfCuttingIt() {
        show()
        val button = composeRule.onNodeWithText(string(R.string.auth_welcome_sign_in_with_google))
        composeRule.assertFitsTheScreen(button)

        val height = button.getUnclippedBoundsInRoot().height

        assertTrue("The button is $height tall, as if its height were fixed.", height > RESTING_BUTTON_HEIGHT)
    }
}

private val RESTING_BUTTON_HEIGHT = 56.dp

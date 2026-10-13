package bo.saludencasa.features.auth.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.presentation.messageRes
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // The name is drawn, not set in type, so it keeps its width at 200 %; the
    // trace takes whatever width is left. Both have to fit 320 dp.
    @Test
    fun theNameAndTheHeartbeatFitTheNarrowestScreen() {
        composeRule.setContentAtDoubleFontScale {
            StartupContent(destination = StartupDestination.Home, playIntro = false, onRetryClick = {})
        }

        composeRule.assertFitsTheScreen(composeRule.onNodeWithContentDescription(string(R.string.app_name)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithContentDescription(string(R.string.cd_loading)))
    }

    // The mark and the name take most of a short screen, which pushes the way
    // out of a failed start below them.
    @Test
    fun theWayOutOfAFailedStartStaysReachable() {
        val error = ProfileError.NetworkUnavailable
        composeRule.setContentAtDoubleFontScale {
            StartupContent(destination = StartupDestination.Error(error), playIntro = false, onRetryClick = {})
        }

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(error.messageRes())))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }
}

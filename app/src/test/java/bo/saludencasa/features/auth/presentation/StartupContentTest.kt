package bo.saludencasa.features.auth.presentation

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.ui.animations.INTRO_MILLIS
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// StartupScreen navigates only once the content reports the intro over, so
// these fix when that report comes (docs/decisions.md, 2026-10-13, the startup
// screen).
@RunWith(AndroidJUnit4::class)
class StartupContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var introFinished = false

    private fun show(playIntro: Boolean) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SaludEnCasaTheme(darkTheme = false) {
                StartupContent(
                    destination = StartupDestination.Home,
                    playIntro = playIntro,
                    onRetryClick = {},
                    onIntroFinished = { introFinished = true },
                )
            }
        }
    }

    // Reporting early cuts the heartbeat off halfway; never reporting strands
    // the person on the logo.
    @Test
    fun aLaunchLeavesOnlyOnceTheHeartbeatHasBeenDrawn() {
        show(playIntro = true)

        composeRule.mainClock.advanceTimeBy(INTRO_MILLIS - SHORT_OF_THE_END_MILLIS)
        composeRule.waitForIdle()
        assertFalse(introFinished)

        composeRule.mainClock.advanceTimeBy(2 * SHORT_OF_THE_END_MILLIS)
        composeRule.waitForIdle()
        assertTrue(introFinished)
    }

    // Coming back from sign-in is not a launch: the destination is the only
    // thing worth waiting for.
    @Test
    fun aReturnFromSignInDoesNotWaitForTheIntro() {
        show(playIntro = false)

        composeRule.mainClock.advanceTimeBy(ONE_FRAME_MILLIS)
        composeRule.waitForIdle()

        assertTrue(introFinished)
    }

    // Remove animations is the system's reduced motion setting. With it on,
    // making the person wait for a heartbeat that is never drawn is pure delay.
    @Test
    fun withAnimationsRemovedALaunchDoesNotWait() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        show(playIntro = true)

        composeRule.mainClock.advanceTimeBy(ONE_FRAME_MILLIS)
        composeRule.waitForIdle()

        assertTrue(introFinished)
    }
}

private const val SHORT_OF_THE_END_MILLIS = 200L
private const val ONE_FRAME_MILLIS = 32L

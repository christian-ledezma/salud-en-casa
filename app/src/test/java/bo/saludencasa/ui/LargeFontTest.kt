package bo.saludencasa.ui

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // Without this every dialog test passes at the normal font size and says nothing
    // (docs/decisions.md, 2026-11-04, HT-29).
    @Test
    fun aDialogWindowIsAtDoubleFontScaleToo() {
        var fontScale = 0f

        composeRule.setContentAtDoubleFontScale {
            Dialog(onDismissRequest = {}) {
                fontScale = LocalDensity.current.fontScale
            }
        }
        composeRule.waitForIdle()

        assertEquals(2f, fontScale)
    }
}

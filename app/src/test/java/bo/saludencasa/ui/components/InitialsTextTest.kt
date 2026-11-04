package bo.saludencasa.ui.components

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.theme.Spacing
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InitialsTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    // HU-19: at 200 % the initials touched the edge of the circle, which does not
    // grow with the font scale.
    @Test
    fun theInitialsKeepAMarginInsideTheCircleAtDoubleFontSize() {
        composeRule.setContentAtDoubleFontScale {
            CardAvatar(name = "Wendy Mamani", photoUrl = null)
        }

        val width: Dp = composeRule.onNodeWithText("WM").getUnclippedBoundsInRoot().width

        assertTrue("The initials are $width wide.", width < Spacing.scale48 - Spacing.scale8)
    }
}

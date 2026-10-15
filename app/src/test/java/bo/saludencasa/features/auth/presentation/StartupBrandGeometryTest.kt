package bo.saludencasa.features.auth.presentation

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.ui.string
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBrandGeometryTest {
    @get:Rule
    val composeRule = createComposeRule()

    // The welcome entrance starts from settledStartupBrand, not from the startup
    // screen itself. If StartupContent moves its brand and the function does not
    // follow, the brand jumps when one screen hands over to the other.
    @Test
    fun theWelcomeEntranceStartsWhereTheStartupLeavesTheBrand() {
        composeRule.setContent {
            SaludEnCasaTheme(darkTheme = false) {
                StartupContent(destination = StartupDestination.Home, playIntro = false, onRetryClick = {})
            }
        }
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val wordmark = composeRule.onNodeWithContentDescription(string(R.string.app_name)).getUnclippedBoundsInRoot()
        val trace = composeRule.onNodeWithContentDescription(string(R.string.cd_loading)).getUnclippedBoundsInRoot()

        with(composeRule.density) {
            val expected =
                settledStartupBrand(
                    container = Size(root.width.toPx(), root.height.toPx()),
                    wordmark = Size(wordmark.width.toPx(), wordmark.height.toPx()),
                )

            assertCentredOn(expected.wordmark, wordmark)
            assertCentredOn(expected.trace, trace)
        }
    }
}

private fun Density.assertCentredOn(
    expected: BrandPart,
    actual: DpRect,
) {
    val tolerance = 1.dp.toPx()
    assertEquals(expected.center.x, ((actual.left + actual.right) / 2).toPx(), tolerance)
    assertEquals(expected.center.y, ((actual.top + actual.bottom) / 2).toPx(), tolerance)
    assertEquals(expected.width, actual.width.toPx(), tolerance)
}

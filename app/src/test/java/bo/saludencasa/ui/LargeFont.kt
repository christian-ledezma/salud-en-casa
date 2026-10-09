package bo.saludencasa.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import org.junit.Assert.assertTrue

// The interface tests run on the JVM through Robolectric, not on an emulator
// (docs/decisions.md, 2026-10-08). Rounding in the layout leaves sub-pixel
// overshoots that are not defects.
private val OVERFLOW_TOLERANCE: Dp = 1.dp

// Downloadable Inter is unreachable without Play Services, so the composition
// falls back to the platform font and the measured widths are approximate
// (docs/decisions.md, 2026-10-08).
fun ComposeContentTestRule.setContentAtDoubleFontScale(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
            SaludEnCasaTheme(darkTheme = darkTheme) { content() }
        }
    }
}

fun string(
    @StringRes id: Int,
    vararg formatArgs: Any,
): String = ApplicationProvider.getApplicationContext<Context>().getString(id, *formatArgs)

fun plural(
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any,
): String =
    ApplicationProvider
        .getApplicationContext<Context>()
        .resources
        .getQuantityString(id, quantity, *formatArgs)

// Scrolls first: every screen of this project is scrollable, so a control below
// the fold is reachable rather than clipped, and asserting it is on screen without
// scrolling would fail for a layout that is perfectly fine.
fun ComposeContentTestRule.assertFitsTheScreen(node: SemanticsNodeInteraction) {
    node.performScrollTo()
    node.assertIsDisplayed()
    val bounds = node.getUnclippedBoundsInRoot()
    val screen = onRoot().getUnclippedBoundsInRoot()
    assertTrue(
        "The node reaches ${bounds.right}, past the right edge at ${screen.right}.",
        bounds.right <= screen.right + OVERFLOW_TOLERANCE,
    )
    assertTrue(
        "The node starts at ${bounds.left}, past the left edge at ${screen.left}.",
        bounds.left >= screen.left - OVERFLOW_TOLERANCE,
    )
}

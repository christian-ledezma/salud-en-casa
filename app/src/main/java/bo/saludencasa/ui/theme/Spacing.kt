package bo.saludencasa.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// docs/design-system.md, section 4. The named fields are the doc's specific
// uses; the base scale covers combinations the doc does not name.
object Spacing {
    val labelToField: Dp = 8.dp
    val listItemGap: Dp = 12.dp
    val cardPadding: Dp = 16.dp
    val screenMargin: Dp = 20.dp
    val sectionGap: Dp = 24.dp
    val minTouchTarget: Dp = 48.dp

    // docs/design-system.md, section 5: the map of the address screen. Tall
    // enough to tell one block from the next and short enough to leave the
    // written address on screen beside it.
    val mapHeight: Dp = 280.dp

    val scale4: Dp = 4.dp
    val scale8: Dp = 8.dp
    val scale12: Dp = 12.dp
    val scale16: Dp = 16.dp
    val scale20: Dp = 20.dp
    val scale24: Dp = 24.dp
    val scale32: Dp = 32.dp
    val scale40: Dp = 40.dp
    val scale48: Dp = 48.dp
}

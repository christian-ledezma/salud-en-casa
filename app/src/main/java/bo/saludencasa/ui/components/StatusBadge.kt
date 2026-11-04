package bo.saludencasa.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

enum class StatusTone {
    OPEN,
    NEUTRAL,
    RUNNING,
    POSITIVE,
    NEGATIVE,
}

// Amber only as a fill with dark text on top (.claude/rules/compose.md).
@Composable
fun StatusBadge(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val statusColors = SaludEnCasaTheme.statusColors
    val (container, onContainer) =
        when (tone) {
            StatusTone.OPEN -> colors.secondaryContainer to colors.onSecondaryContainer
            StatusTone.NEUTRAL -> colors.surfaceVariant to colors.onSurface
            StatusTone.RUNNING -> colors.tertiaryContainer to colors.onTertiaryContainer
            StatusTone.POSITIVE -> statusColors.positiveContainer to statusColors.onPositiveContainer
            StatusTone.NEGATIVE -> colors.errorContainer to colors.onErrorContainer
        }
    Surface(modifier = modifier, shape = ExtraShapes.pill, color = container, contentColor = onContainer) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.scale12, vertical = Spacing.scale4),
        )
    }
}

// docs/decisions.md, 2026-11-04, HU-23, the pill beside a title.
@Composable
@ReadOnlyComposable
fun badgeFitsBesideTitle(): Boolean = LocalDensity.current.fontScale <= 1f

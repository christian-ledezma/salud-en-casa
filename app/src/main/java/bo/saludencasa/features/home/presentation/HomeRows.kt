package bo.saludencasa.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// The tone of a row's leading square. The colour carries the state and the
// title says it in words, so nothing depends on the colour alone.
internal enum class RowTone { NEUTRAL, PENDING, DANGER }

// Long service type names wrap once and then end in an ellipsis, so one row can
// never push the rest of the list off the screen (docs/decisions.md, 2026-10-30, HT-25).
private const val TITLE_MAX_LINES = 2

@Composable
internal fun HomeRowGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(content = content)
    }
}

@Composable
internal fun HomeRowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun HomeRow(
    title: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    details: List<String> = emptyList(),
    tone: RowTone = RowTone.NEUTRAL,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
) {
    val (container, onContainer) = tone.colors()
    val titleColor =
        if (tone == RowTone.DANGER) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = Spacing.minTouchTarget)
                .padding(horizontal = Spacing.cardPadding, vertical = Spacing.scale12),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(Spacing.scale40)
                    .clip(MaterialTheme.shapes.small)
                    .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(Spacing.scale20),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = titleColor,
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            details.forEach { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.scale20),
            )
        }
    }
}

@Composable
internal fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

@Composable
internal fun StatusPill(
    text: String,
    tone: RowTone,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, onContainer) =
        when (tone) {
            RowTone.NEUTRAL -> colors.secondaryContainer to colors.onSecondaryContainer
            RowTone.PENDING -> colors.surfaceVariant to SaludEnCasaTheme.statusColors.pending
            RowTone.DANGER -> colors.errorContainer to colors.error
        }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = onContainer,
        modifier =
            modifier
                .clip(SaludEnCasaTheme.extraShapes.pill)
                .background(container)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale4),
    )
}

@Composable
private fun RowTone.colors(): Pair<Color, Color> =
    when (this) {
        RowTone.NEUTRAL -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        RowTone.PENDING -> MaterialTheme.colorScheme.surfaceVariant to SaludEnCasaTheme.statusColors.pending
        RowTone.DANGER -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
    }

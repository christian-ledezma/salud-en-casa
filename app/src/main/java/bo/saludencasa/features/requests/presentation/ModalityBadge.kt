package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.Spacing

// Amber only as a fill with dark text on top (.claude/rules/compose.md).
@Composable
internal fun ModalityBadge(
    modality: RequestModality,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) =
        when (modality) {
            RequestModality.IMMEDIATE -> colors.tertiaryContainer to colors.onTertiaryContainer
            RequestModality.SCHEDULED -> colors.primaryContainer to colors.onPrimaryContainer
        }

    Text(
        text = stringResource(modality.labelRes()),
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier =
            modifier
                .clip(ExtraShapes.pill)
                .background(container)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale4),
    )
}

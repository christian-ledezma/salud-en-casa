package bo.saludencasa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// docs/design-system.md, section 5: two or three exclusive options in a pill,
// the active one filled in primary. Modality and history filters share this
// component, so the label comes from the caller rather than being hardcoded.
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelectedChange: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(ExtraShapes.pill)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(Spacing.scale4)
                .selectableGroup(),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.minTouchTarget)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelectedChange(option) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
    }
}

private enum class PreviewModality { IMMEDIATE, SCHEDULED }

@Preview(showBackground = true)
@Composable
private fun SegmentedControlLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        SegmentedControl(
            options = PreviewModality.entries,
            selected = PreviewModality.IMMEDIATE,
            onSelectedChange = {},
            optionLabel = { if (it == PreviewModality.IMMEDIATE) "Inmediata" else "Agendada" },
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedControlDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        SegmentedControl(
            options = PreviewModality.entries,
            selected = PreviewModality.SCHEDULED,
            onSelectedChange = {},
            optionLabel = { if (it == PreviewModality.IMMEDIATE) "Inmediata" else "Agendada" },
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

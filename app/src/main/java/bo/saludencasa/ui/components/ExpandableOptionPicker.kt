package bo.saludencasa.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// docs/decisions.md, 2026-10-09, a Material chip cannot carry the name of a
// service type: the twelve of them are a list that opens. The open state is the
// caller's, so a preview and a layout test can show the list open.
@Composable
fun <T> ExpandableOptionPicker(
    label: String,
    selectedLabel: String,
    toggleDescription: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    options: List<T>,
    selected: T?,
    onSelectedChange: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.minTouchTarget)
                    .clip(MaterialTheme.shapes.small)
                    .border(Spacing.borderWidth, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = Spacing.scale12, vertical = Spacing.scale4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            // One icon turned over rather than two: the core icon set is the
            // only one this project ships, because RNF-03 caps the package at
            // 25 MB, and it has no arrow pointing up.
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = toggleDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }

        if (expanded) {
            RadioOptionGroup(
                label = label,
                options = options,
                selected = selected,
                onSelectedChange = onSelectedChange,
                optionLabel = optionLabel,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun ExpandableOptionPickerLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ExpandableOptionPicker(
            label = "Tipo de atención",
            selectedLabel = "Vacunación",
            toggleDescription = "",
            expanded = true,
            onToggle = {},
            options = listOf("Vacunación", "Colocación y control de vía venosa"),
            selected = "Vacunación",
            onSelectedChange = {},
            optionLabel = { it },
        )
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
private fun ExpandableOptionPickerLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        ExpandableOptionPicker(
            label = "Tipo de atención",
            selectedLabel = "Colocación y control de vía venosa",
            toggleDescription = "",
            expanded = false,
            onToggle = {},
            options = listOf("Vacunación"),
            selected = null,
            onSelectedChange = {},
            optionLabel = { it },
        )
    }
}

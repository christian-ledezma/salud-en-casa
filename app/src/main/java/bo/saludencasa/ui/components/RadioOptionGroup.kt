package bo.saludencasa.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// docs/design-system.md, section 5: the exclusive group for more options than
// the segmented control can hold. The whole row is the touch target, not the
// circle, so it clears 48 dp and stays reachable when the system font size
// grows.
@Composable
fun <T> RadioOptionGroup(
    label: String,
    options: List<T>,
    selected: T?,
    onSelectedChange: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        options.forEach { option ->
            val isSelected = option == selected
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.minTouchTarget)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelectedChange(option) },
                            role = Role.RadioButton,
                        ).padding(vertical = Spacing.scale4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private enum class PreviewType { DOCTOR, NURSE, PHYSIOTHERAPIST, STUDENT }

@Preview(showBackground = true)
@Composable
private fun RadioOptionGroupLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        RadioOptionGroup(
            label = "Tipo de profesional",
            options = PreviewType.entries,
            selected = PreviewType.NURSE,
            onSelectedChange = {},
            optionLabel = { it.name.lowercase() },
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadioOptionGroupDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        RadioOptionGroup(
            label = "Tipo de profesional",
            options = PreviewType.entries,
            selected = null,
            onSelectedChange = {},
            optionLabel = { it.name.lowercase() },
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

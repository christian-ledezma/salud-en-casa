package bo.saludencasa.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// docs/design-system.md, section 5: inactive is an icon-only circle; active
// expands to a labeled pill. The touch target stays 48 dp even though the
// visible circle is smaller (compose.md).
@Composable
fun SpecialtyChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier =
            modifier
                .heightIn(min = Spacing.minTouchTarget)
                .widthIn(min = Spacing.minTouchTarget)
                .clip(ExtraShapes.pill)
                .background(containerColor)
                .clickable(onClick = onClick)
                .semantics {
                    this.selected = selected
                    role = Role.Checkbox
                }.animateContentSize()
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (selected) null else label,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        AnimatedVisibility(visible = selected) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpecialtyChipLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
            modifier = Modifier.padding(Spacing.screenMargin),
        ) {
            SpecialtyChip(
                label = "Enfermería",
                icon = Icons.Filled.Favorite,
                selected = true,
                onClick = {},
            )
            SpecialtyChip(
                label = "Fisioterapia",
                icon = Icons.Filled.Favorite,
                selected = false,
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpecialtyChipDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
            modifier = Modifier.padding(Spacing.screenMargin),
        ) {
            SpecialtyChip(
                label = "Enfermería",
                icon = Icons.Filled.Favorite,
                selected = true,
                onClick = {},
            )
            SpecialtyChip(
                label = "Fisioterapia",
                icon = Icons.Filled.Favorite,
                selected = false,
                onClick = {},
            )
        }
    }
}

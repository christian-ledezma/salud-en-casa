package bo.saludencasa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// docs/design-system.md, section 5: the live availability of a professional,
// inside the card or the profile it belongs to.
@Composable
fun AvailabilityPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(ExtraShapes.pill)
                .background(SaludEnCasaTheme.statusColors.positiveContainer)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvailabilityDot()
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = SaludEnCasaTheme.statusColors.onPositiveContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AvailabilityPillLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AvailabilityPill(text = "Disponible ahora")
    }
}

@Preview(showBackground = true)
@Composable
private fun AvailabilityPillDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        AvailabilityPill(text = "Disponible ahora")
    }
}

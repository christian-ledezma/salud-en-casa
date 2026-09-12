package bo.saludencasa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import java.util.Locale

// docs/design-system.md, section 5: pill in tertiaryContainer, filled star,
// number in onTertiaryContainer. The number never sits directly on the raw
// amber tertiaryContainer fill — onTertiaryContainer is the corrected value
// verified for that contrast (docs/design-system.md, section 1).
@Composable
fun RatingBadge(
    rating: Double,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val semanticsModifier =
        if (contentDescription != null) {
            Modifier.semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
        } else {
            Modifier
        }

    Row(
        modifier =
            modifier
                .then(semanticsModifier)
                .clip(ExtraShapes.pill)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = Spacing.scale8, vertical = Spacing.scale4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale4),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = String.format(Locale.ROOT, "%.1f", rating),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingBadgeLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        RatingBadge(rating = 4.8, modifier = Modifier.padding(Spacing.screenMargin))
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingBadgeDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        RatingBadge(rating = 4.8, modifier = Modifier.padding(Spacing.screenMargin))
    }
}

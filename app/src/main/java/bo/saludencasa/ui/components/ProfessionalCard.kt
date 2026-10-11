package bo.saludencasa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import coil3.compose.AsyncImage

// docs/design-system.md, section 5: photo bleeds the right edge, information
// on the left in this exact order — name, specialty, rating, distance, rate —
// with the action button anchored bottom-right. Distance is required here,
// unlike the reference: it is the primary selection criterion (RN-02).
@Composable
fun ProfessionalCard(
    name: String,
    specialty: String,
    // Null when nobody has rated this person yet. The average alone cannot say
    // so: a professional with no votes carries an average of zero, and drawing
    // the badge for it shows the worst possible score as if someone had given
    // it (docs/decisions.md, 2026-10-09).
    rating: Double?,
    noRatingLabel: String,
    distanceText: String,
    rateText: String,
    photoUrl: String?,
    actionIcon: ImageVector,
    actionContentDescription: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(Spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    Text(text = name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = specialty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // FlowRow and not Row: without a rating the badge's place is
                    // taken by a sentence, and in a plain Row the distance only
                    // got the width that sentence left over -- which at a 200 %
                    // font scale was none, and the distance stopped being drawn
                    // at all. Here it drops to the line below instead.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                    ) {
                        if (rating == null) {
                            Text(
                                text = noRatingLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            RatingBadge(rating = rating)
                        }
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(text = rateText, style = MaterialTheme.typography.titleMedium)
                }

                Box(
                    modifier =
                        Modifier
                            .width(96.dp)
                            .fillMaxHeight(),
                ) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initialsOf(name),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            FilledIconButton(
                onClick = onActionClick,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Spacing.scale12)
                        .size(Spacing.minTouchTarget),
            ) {
                Icon(imageVector = actionIcon, contentDescription = actionContentDescription)
            }
        }
    }
}

private fun initialsOf(name: String): String =
    name
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString(separator = "")

@Preview(showBackground = true)
@Composable
private fun ProfessionalCardLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ProfessionalCard(
            name = "Ana Pérez",
            specialty = "Enfermería",
            rating = 4.8,
            noRatingLabel = "Sin calificaciones",
            distanceText = "1.2 km",
            rateText = "Bs 80",
            photoUrl = null,
            actionIcon = Icons.Filled.Call,
            actionContentDescription = "Solicitar atención",
            onActionClick = {},
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfessionalCardDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        ProfessionalCard(
            name = "Ana Pérez",
            specialty = "Enfermería",
            rating = 4.8,
            noRatingLabel = "Sin calificaciones",
            distanceText = "1.2 km",
            rateText = "Bs 80",
            photoUrl = null,
            actionIcon = Icons.Filled.Call,
            actionContentDescription = "Solicitar atención",
            onActionClick = {},
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

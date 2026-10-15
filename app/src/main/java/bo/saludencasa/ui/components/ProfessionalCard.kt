package bo.saludencasa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import coil3.compose.AsyncImage

// docs/design-system.md, section 5: the whole card opens the profile. Avatar,
// name, specialty, rating and distance on top; the base rate and the live
// availability at its foot. Distance is required here, unlike the reference:
// it is the primary selection criterion (RN-02).
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
    // Null when the professional is not available right now.
    availableNowText: String?,
    photoUrl: String?,
    openContentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Spacing.cardPadding,
                            top = Spacing.cardPadding,
                            end = Spacing.scale8,
                            bottom = Spacing.scale12,
                        ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardAvatar(name = name, photoUrl = photoUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.scale4),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Spacing.scale16),
                            )
                            Text(
                                text = distanceText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = openContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.cardPadding, vertical = Spacing.scale12),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
            ) {
                Text(
                    text = rateText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                availableNowText?.let { AvailabilityPill(text = it) }
            }
        }
    }
}

// The initials are drawn always and the photograph on top of them, so they
// serve as the placeholder while it loads and as the error state if it never
// arrives, which .claude/rules/compose.md requires of every image.
@Composable
internal fun CardAvatar(
    name: String,
    photoUrl: String?,
) {
    Box(
        modifier =
            Modifier
                .size(Spacing.scale48)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun initialsOf(name: String): String =
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
            availableNowText = "Disponible ahora",
            photoUrl = null,
            openContentDescription = "Ver la ficha de Ana Pérez",
            onClick = {},
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
            rating = null,
            noRatingLabel = "Sin calificaciones",
            distanceText = "1.2 km",
            rateText = "Bs 80",
            availableNowText = null,
            photoUrl = null,
            openContentDescription = "Ver la ficha de Ana Pérez",
            onClick = {},
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

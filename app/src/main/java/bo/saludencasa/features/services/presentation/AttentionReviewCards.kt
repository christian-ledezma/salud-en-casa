package bo.saludencasa.features.services.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.features.services.domain.model.MyReview
import bo.saludencasa.ui.components.StarPicker
import bo.saludencasa.ui.components.StarsRead
import bo.saludencasa.ui.theme.Spacing

// docs/decisions.md, 2026-11-26, HU-29, and 2026-11-27, HU-30: tapping a star opens the rating with that star
// already chosen, so the card asks nothing the next screen would ask again.
@Composable
internal fun RateAttentionCard(
    counterpartName: String,
    iAmThePatient: Boolean,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            Text(
                text =
                    stringResource(
                        if (iAmThePatient) R.string.review_card_title else R.string.review_card_title_professional,
                        counterpartName,
                    ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.review_card_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StarPicker(rating = 0, onPick = onPick)
        }
    }
}

// What the reader rated, which is the only rating this screen ever shows: what the
// other party rated of them does not come back (docs/decisions.md, 2026-11-26, HU-29).
@Composable
internal fun MyReviewCard(
    review: MyReview,
    counterpartName: String,
    iAmThePatient: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            Text(
                text = stringResource(R.string.review_mine_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                StarsRead(rating = review.rating)
                Text(
                    text = stringResource(review.rating.ratingLabelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            review.comment?.let { comment ->
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text =
                    stringResource(
                        if (iAmThePatient) {
                            R.string.review_mine_visibility
                        } else {
                            R.string.review_mine_visibility_professional
                        },
                        counterpartName,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Int.ratingLabelRes(): Int =
    when (this) {
        1 -> R.string.review_rating_1
        2 -> R.string.review_rating_2
        3 -> R.string.review_rating_3
        4 -> R.string.review_rating_4
        else -> R.string.review_rating_5
    }

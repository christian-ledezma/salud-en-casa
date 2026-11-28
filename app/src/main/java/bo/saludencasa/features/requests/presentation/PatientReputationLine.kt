package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.features.requests.domain.model.PatientReputation
import bo.saludencasa.ui.components.RatingBadge
import bo.saludencasa.ui.theme.Spacing
import java.util.Locale

// The average and the count, where the professional decides whether to open the
// request (docs/decisions.md, 2026-11-27, HU-30).
@Composable
internal fun PatientReputationLine(
    reputation: PatientReputation,
    modifier: Modifier = Modifier,
) {
    if (!reputation.isRated) {
        Text(
            text = stringResource(R.string.patient_reputation_none),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        RatingBadge(rating = reputation.averageRating.toDouble())
        Text(
            text =
                pluralStringResource(
                    R.plurals.patient_reputation_count,
                    reputation.totalReviews,
                    reputation.totalReviews,
                ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// The same fact as one line of text, for the rows that are lists of text.
@Composable
internal fun patientReputationText(reputation: PatientReputation): String =
    if (reputation.isRated) {
        pluralStringResource(
            R.plurals.patient_reputation_home,
            reputation.totalReviews,
            String.format(Locale.ROOT, "%.1f", reputation.averageRating.toDouble()),
            reputation.totalReviews,
        )
    } else {
        stringResource(R.string.patient_reputation_none)
    }

package bo.saludencasa.features.search.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.profile.presentation.labelRes
import bo.saludencasa.features.search.domain.model.NearbyProfessional
import bo.saludencasa.ui.components.ProfessionalCard

// One result of the nearby search, the same card in the list to schedule from
// and under the map of «Atención ahora».
@Composable
internal fun NearbyProfessionalCard(
    professional: NearbyProfessional,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val supporting =
        professional.specialty
            ?: professional.professionalType?.let { stringResource(it.labelRes()) }
            ?: ""

    ProfessionalCard(
        name = professional.fullName,
        specialty = supporting,
        // The badge only when votes hold it up: an average of zero and no
        // average at all are the same number (docs/decisions.md, 2026-10-09).
        rating = professional.averageRating.takeIf { professional.totalReviews > 0 },
        noRatingLabel = stringResource(R.string.search_no_rating_yet),
        distanceText = distanceText(professional.distanceM),
        rateText = stringResource(R.string.public_profile_base_rate, formatBob(professional.baseRateBob, locale)),
        availableNowText = stringResource(R.string.search_available_now_badge).takeIf { professional.availableNow },
        photoUrl = professional.photoUrl,
        openContentDescription = stringResource(R.string.cd_open_professional_profile, professional.fullName),
        onClick = onClick,
        modifier = modifier,
        actionText = actionText,
        onAction = onAction,
    )
}

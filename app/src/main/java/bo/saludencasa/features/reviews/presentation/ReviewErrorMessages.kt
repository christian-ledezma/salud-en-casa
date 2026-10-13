package bo.saludencasa.features.reviews.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.reviews.domain.model.ReviewError

@StringRes
internal fun ReviewError.messageRes(): Int =
    when (this) {
        ReviewError.NotSignedIn -> R.string.error_reviews_not_signed_in
        ReviewError.UnreadableResult -> R.string.error_reviews_unreadable_result
        ReviewError.NetworkUnavailable -> R.string.error_network_unavailable
        ReviewError.Unexpected -> R.string.error_unexpected
    }

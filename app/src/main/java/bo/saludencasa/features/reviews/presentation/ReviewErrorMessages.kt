package bo.saludencasa.features.reviews.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewError

@StringRes
internal fun ReviewError.messageRes(): Int =
    when (this) {
        ReviewError.NotSignedIn -> R.string.error_reviews_not_signed_in
        ReviewError.UnreadableResult -> R.string.error_reviews_unreadable_result
        ReviewError.NotAllowed -> R.string.error_review_not_allowed
        ReviewError.AlreadyRated -> R.string.error_review_already_rated
        ReviewError.NetworkUnavailable -> R.string.error_network_unavailable
        ReviewError.Unexpected -> R.string.error_unexpected
    }

@StringRes
internal fun SubmitReviewError.messageRes(): Int =
    when (this) {
        SubmitReviewError.RatingMissing -> R.string.error_review_rating_missing
        SubmitReviewError.CommentTooLong -> R.string.error_review_comment_too_long
        is SubmitReviewError.Review -> error.messageRes()
    }

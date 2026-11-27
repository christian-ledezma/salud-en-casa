package bo.saludencasa.features.reviews.domain.model

import bo.saludencasa.features.services.domain.model.ServiceError

sealed interface ReviewError {
    data object NotSignedIn : ReviewError

    // A row this version cannot read, such as one without a usable date. The
    // page fails instead of shrinking (docs/decisions.md, 2026-10-06).
    data object UnreadableResult : ReviewError

    // The attention is not one this person may rate: not theirs, not completed, or
    // not found. The engine's refusal says no more than that (docs/decisions.md,
    // 2026-11-26, HU-29).
    data object NotAllowed : ReviewError

    data object AlreadyRated : ReviewError

    data object NetworkUnavailable : ReviewError

    data object Unexpected : ReviewError
}

// Reading the attention to rate it can fail the way any read of it does.
internal fun ServiceError.toReviewError(): ReviewError =
    when (this) {
        ServiceError.NotSignedIn -> ReviewError.NotSignedIn
        ServiceError.NetworkUnavailable -> ReviewError.NetworkUnavailable
        ServiceError.AttentionChanged, ServiceError.Unexpected -> ReviewError.Unexpected
    }

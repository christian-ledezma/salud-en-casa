package bo.saludencasa.features.reviews.domain.model

sealed interface ReviewError {
    data object NotSignedIn : ReviewError

    // A row this version cannot read, such as one without a usable date. The
    // page fails instead of shrinking (docs/decisions.md, 2026-10-06).
    data object UnreadableResult : ReviewError

    data object NetworkUnavailable : ReviewError

    data object Unexpected : ReviewError
}

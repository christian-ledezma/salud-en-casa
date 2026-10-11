package bo.saludencasa.features.search.domain.model

sealed interface SearchError {
    data object NotSignedIn : SearchError

    data object NoPrimaryAddress : SearchError

    // A row this version cannot read, such as a result without a rate. The
    // page fails instead of shrinking (docs/decisions.md, 2026-10-09).
    data object UnreadableResult : SearchError

    data object NetworkUnavailable : SearchError

    data object Unexpected : SearchError
}

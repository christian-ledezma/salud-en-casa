package bo.saludencasa.features.services.domain.model

// The error carries a type, never a phrase (.claude/rules/i18n.md). The
// presentation layer turns it into a resource key.
sealed interface ServiceError {
    data object NotSignedIn : ServiceError

    data object NetworkUnavailable : ServiceError

    data object Unexpected : ServiceError
}

sealed interface AttentionResult {
    data class Loaded(
        val attention: Attention,
    ) : AttentionResult

    // No service for that request visible to this caller: not accepted yet, or
    // not one of its two parties.
    data object NotFound : AttentionResult

    data class Failure(
        val error: ServiceError,
    ) : AttentionResult
}

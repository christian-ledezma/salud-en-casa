package bo.saludencasa.features.requests.domain.model

// The error carries a type, never a phrase (.claude/rules/i18n.md). The policy
// refuses with a single generic denial, so the client cannot tell a closed
// request from an ineligible professional from one's own request: the detail
// screen shows those proactively, and NotAllowed covers the refusal.
sealed interface OfferError {
    data object NotSignedIn : OfferError

    data object InvalidAmount : OfferError

    data object MessageRequired : OfferError

    data object NotAllowed : OfferError

    data object NetworkUnavailable : OfferError

    data object Unexpected : OfferError
}

sealed interface OfferSubmitResult {
    data object Success : OfferSubmitResult

    data class Failure(
        val error: OfferError,
    ) : OfferSubmitResult
}

sealed interface RequestOffersResult {
    data class Loaded(
        val offers: List<RequestOffer>,
    ) : RequestOffersResult

    data class Failure(
        val error: OfferError,
    ) : RequestOffersResult
}

sealed interface OfferableRequestResult {
    data class Loaded(
        val request: OfferableRequest,
    ) : OfferableRequestResult

    // The request is not visible to this professional: never offerable, already
    // accepted and not theirs, or gone.
    data object NotFound : OfferableRequestResult

    data class Failure(
        val error: OfferError,
    ) : OfferableRequestResult
}

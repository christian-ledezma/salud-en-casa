package bo.saludencasa.features.requests.domain.model

import java.math.BigDecimal

// The error carries a type, never a phrase (.claude/rules/i18n.md). The policy
// refuses with a single generic denial, so the client cannot tell a closed
// request from an ineligible professional from one's own request: the detail
// screen shows those proactively, and NotAllowed covers the refusal.
sealed interface OfferError {
    data object NotSignedIn : OfferError

    data object InvalidAmount : OfferError

    // The patient's counter-offer is below the floor. minimumBob is null when it
    // was the engine that refused, with a thread older than the one on screen.
    data class BelowMinimum(
        val minimumBob: BigDecimal?,
    ) : OfferError

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

// The threads on the patient's own request, one per professional who offered.
sealed interface OfferThreadsResult {
    data class Loaded(
        val threads: List<OfferThread>,
    ) : OfferThreadsResult

    data class Failure(
        val error: OfferError,
    ) : OfferThreadsResult
}

sealed interface OfferThreadResult {
    data class Loaded(
        val thread: OfferThread,
    ) : OfferThreadResult

    // No chain with that professional on that request, for this caller.
    data object NotFound : OfferThreadResult

    data class Failure(
        val error: OfferError,
    ) : OfferThreadResult
}

// Rejecting an offer. A refusal updates no row rather than raising, so the
// repository tells the two apart by what came back.
sealed interface OfferActionResult {
    data object Success : OfferActionResult

    data class Failure(
        val error: OfferError,
    ) : OfferActionResult
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

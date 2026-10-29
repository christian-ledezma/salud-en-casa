package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.domain.model.OfferError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

// The insert policy refuses with the platform's row-level security error, which
// names no reason the person can act on: a closed request, an ineligible
// professional, one's own request, a thread that is not theirs to answer and one
// already closed all look the same here. The screens show the state of the thread
// before offering the control; this maps the refusal to a single NotAllowed.
internal fun Throwable.toOfferError(): OfferError {
    message?.let { body ->
        // The screen already checked the floor; the engine says so only when the
        // thread on screen was older than the one it read.
        if (body.contains(BELOW_FLOOR)) return OfferError.BelowMinimum(null)
        if (REFUSAL_LABELS.any(body::contains)) {
            return OfferError.NotAllowed
        }
    }

    return when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> OfferError.NetworkUnavailable
        else -> OfferError.Unexpected
    }
}

private const val BELOW_FLOOR = "counter_offer_below_your_last_offer"

private val REFUSAL_LABELS =
    listOf(
        "row-level security",
        "request_status_change_requires_the_atomic_operation",
        "issued_offer_cannot_be_edited",
        "answered_offer_cannot_be_rejected",
        "offer_acceptance_requires_the_atomic_operation",
        // accept_offer refuses when the thread moved on before the tap landed:
        // the offer was answered, rejected or already accepted, or the request is
        // no longer open. The screen reloads to show the new state (HU-19).
        "offer_not_open",
        "offer_already_answered",
        "offer_not_found",
        "request_not_open",
        "not_your_offer_to_accept",
        // Two answers to the same offer, or two first offers, from two taps or
        // two phones: the second loses to the unique index (HU-18).
        "idx_request_offers_one_answer_per_parent",
        "idx_request_offers_one_first_offer",
    )

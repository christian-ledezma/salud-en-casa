package bo.saludencasa.features.requests.presentation

import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.model.ServiceRequest

// Where a card of «Mis solicitudes» takes the patient, so no list stands between
// the card and the negotiation (docs/decisions.md, 2026-10-27, HT-24).
sealed interface NegotiationEntry {
    // An immediate request still open: the waiting screen, with its offers.
    data object ImmediateCare : NegotiationEntry

    // A scheduled request has one professional, so the thread is the only place
    // to go once they have offered.
    data class Thread(
        val requestId: String,
        val professionalId: String,
    ) : NegotiationEntry
}

// Null when there is nothing to negotiate: the request was cancelled, expired or
// accepted, or a scheduled one is still waiting for its first offer.
internal fun ServiceRequest.negotiationEntry(): NegotiationEntry? =
    when (modality) {
        RequestModality.IMMEDIATE -> {
            NegotiationEntry.ImmediateCare.takeIf {
                status == RequestStatus.PUBLISHED || status == RequestStatus.NEGOTIATING
            }
        }

        RequestModality.SCHEDULED -> {
            professionalId
                ?.takeIf { status == RequestStatus.NEGOTIATING }
                ?.let { NegotiationEntry.Thread(id, it) }
        }
    }

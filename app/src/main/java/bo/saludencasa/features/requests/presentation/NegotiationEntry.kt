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

    // Once an offer is accepted there is nothing left to negotiate: the card opens
    // the sealed agreement (HU-19), also while it runs and after it ends (HU-22).
    data class Attention(
        val requestId: String,
    ) : NegotiationEntry
}

private val STATUSES_WITH_AN_ATTENTION =
    setOf(RequestStatus.ACCEPTED, RequestStatus.IN_PROGRESS, RequestStatus.COMPLETED)

// Null when there is nothing to open: the request expired or was cancelled
// (docs/decisions.md, 2026-11-03, HU-22), or a scheduled one is still waiting for
// its first offer.
internal fun ServiceRequest.negotiationEntry(): NegotiationEntry? =
    when {
        status in STATUSES_WITH_AN_ATTENTION -> {
            NegotiationEntry.Attention(id)
        }

        modality == RequestModality.IMMEDIATE -> {
            NegotiationEntry.ImmediateCare.takeIf {
                status == RequestStatus.PUBLISHED || status == RequestStatus.NEGOTIATING
            }
        }

        else -> {
            professionalId
                ?.takeIf { status == RequestStatus.NEGOTIATING }
                ?.let { NegotiationEntry.Thread(id, it) }
        }
    }

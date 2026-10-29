package bo.saludencasa.features.requests.domain.model

import java.math.BigDecimal
import java.time.Instant

// INV-06: an offer is append only. A counter-offer is a new row pointing at its
// parent, never an edit. The first offer of a thread is always the
// professional's, so a patient-issued row always carries a parent.
enum class OfferIssuer {
    PROFESSIONAL,
    PATIENT,
}

enum class OfferStatus {
    PROPOSED,
    ACCEPTED,
    REJECTED,
    EXPIRED,
}

// An offer as either side reads it: the amount, the message and who made it, with
// the professional's public name and reputation. The author of a patient
// counter-offer is the patient themselves; professionalId still names the thread.
data class RequestOffer(
    val id: String,
    val parentOfferId: String?,
    val professionalId: String,
    val professionalName: String?,
    val professionalPhotoUrl: String?,
    val professionalRating: BigDecimal?,
    val amountBob: BigDecimal,
    val message: String?,
    val issuer: OfferIssuer,
    val status: OfferStatus,
    val createdAt: Instant,
)

// One professional's chain on a request, in the order it was written, as either
// side reads it. Which side the reader is on is decided by the signed-in
// identifier against professionalId, and INV-16 keeps that unambiguous: nobody is
// both parties of the same service.
data class OfferThread(
    val professionalId: String,
    val professionalName: String?,
    val professionalPhotoUrl: String?,
    val professionalRating: BigDecimal?,
    val offers: List<RequestOffer>,
    val iAmTheProfessional: Boolean,
    // Only the patient reads these two: the distance from the published point of
    // the professional's base, and the budget the patient suggested when publishing.
    val distanceM: Int? = null,
    val suggestedBudgetBob: BigDecimal? = null,
) {
    // The patient never counters below what they already offered: the budget they
    // published, which is their first offer, or the highest amount they proposed on
    // this thread. The professional has no floor. docs/decisions.md, 2026-10-27, HT-24.
    val counterFloorBob: BigDecimal?
        get() {
            if (iAmTheProfessional) return null
            val highestOwn = offers.filter { it.issuer == OfferIssuer.PATIENT }.maxOfOrNull { it.amountBob }
            return listOfNotNull(suggestedBudgetBob, highestOwn).maxOrNull()
        }

    val myIssuer: OfferIssuer
        get() = if (iAmTheProfessional) OfferIssuer.PROFESSIONAL else OfferIssuer.PATIENT

    val lastOffer: RequestOffer?
        get() = offers.lastOrNull()

    // A rejection closes the chain: the engine takes no offer that answers a
    // rejected one, and no offer is rejected once it has been answered.
    val isClosed: Boolean
        get() = lastOffer?.status != OfferStatus.PROPOSED

    // Whoever did not write the offer on the table is the one who answers it.
    val isMyTurn: Boolean
        get() = !isClosed && lastOffer?.issuer != myIssuer
}

// The one request the professional opens to offer on, with whether they already
// did. Same projection as the inbox row: no exact location, no patient identity.
data class OfferableRequest(
    val id: String,
    val serviceTypeName: String,
    val city: String,
    val description: String?,
    val suggestedBudgetBob: BigDecimal?,
    val distanceM: Int,
    val status: RequestStatus,
    val modality: RequestModality,
    val scheduledAt: Instant?,
    val alreadyOffered: Boolean,
)

package bo.saludencasa.features.requests.domain.model

import java.math.BigDecimal
import java.time.Instant

// INV-06: an offer is append only. A counter-offer is a new row pointing at its
// parent, never an edit. HU-17 issues only the first offer of a thread, always
// from the professional; the patient's counter-offer is HU-18.
enum class OfferIssuer {
    PROFESSIONAL,
    PATIENT,
}

// An offer as the patient reads it: the amount, the message and who made it,
// with the professional's public name and reputation. The author of a patient
// counter-offer is the patient themselves; professionalId still names the thread.
data class RequestOffer(
    val id: String,
    val professionalId: String,
    val professionalName: String?,
    val professionalPhotoUrl: String?,
    val professionalRating: BigDecimal?,
    val amountBob: BigDecimal,
    val message: String?,
    val issuer: OfferIssuer,
    val createdAt: Instant,
)

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

package bo.saludencasa.features.requests.domain.model

import java.math.BigDecimal
import java.time.Instant

// What the patient reads back about a request they published. The location is
// not here: INV-03 keeps it as a snapshot the list never needs to draw, and the
// text of the address is what a person recognises.
data class ServiceRequest(
    val id: String,
    val serviceTypeName: String,
    val addressText: String,
    val city: String,
    val status: RequestStatus,
    val modality: RequestModality,
    val description: String?,
    val suggestedBudgetBob: BigDecimal?,
    val expiresAt: Instant?,
    val createdAt: Instant,
)

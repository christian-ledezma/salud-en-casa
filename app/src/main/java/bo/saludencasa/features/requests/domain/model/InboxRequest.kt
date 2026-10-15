package bo.saludencasa.features.requests.domain.model

import java.math.BigDecimal
import java.time.Instant

// A request as the professional reads it in their inbox before offering. It
// carries the distance from their base and nothing that identifies the patient
// or pins the exact place: RF-08.6 reveals contact details only after the offer
// is accepted, and the exact location never leaves the engine (the inbox decides
// on distance and type, docs/decisions.md 2026-10-13).
data class InboxRequest(
    val id: String,
    val serviceTypeName: String,
    val city: String,
    val description: String?,
    val suggestedBudgetBob: BigDecimal?,
    // Already rounded to the hundred metres by the engine, so zero is a value
    // that happens, like the nearby search.
    val distanceM: Int,
    val status: RequestStatus,
    val createdAt: Instant,
    val modality: RequestModality,
    // Present exactly when the request is scheduled; the mapper enforces it.
    val scheduledAt: Instant?,
)

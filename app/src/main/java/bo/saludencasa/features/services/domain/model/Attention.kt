package bo.saludencasa.features.services.domain.model

import java.math.BigDecimal
import java.time.Instant

enum class ServiceStatus {
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

// The other party of a sealed service, revealed once the offer was accepted
// (RN-06, RF-08.6). No phone travels: the patient reuses the professional's
// public profile, the professional sees the patient's name now that it is no
// longer hidden, and coordination happens in the conversation (HU-21).
// docs/decisions.md, 2026-10-30.
data class Counterpart(
    val id: String,
    val name: String,
    val photoUrl: String?,
)

// A sealed agreement as one of its two parties reads it: the frozen amount
// (INV-04, RN-07), the service type and the counterpart. iAmThePatient says which
// side the reader is on, so the screen can name the counterpart accordingly.
data class Attention(
    val serviceId: String,
    val requestId: String,
    val status: ServiceStatus,
    val finalAmountBob: BigDecimal,
    val serviceTypeName: String,
    val counterpart: Counterpart,
    val iAmThePatient: Boolean,
    val createdAt: Instant,
)

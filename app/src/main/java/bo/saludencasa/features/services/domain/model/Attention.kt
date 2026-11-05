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
    val scheduledAt: Instant?,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val description: String?,
    val place: AttentionPlace,
    val conversationOpen: Boolean,
    val conversationWritableUntil: Instant? = null,
    val cancellation: AttentionCancellation? = null,
) {
    val nextStepForMe: AttentionStep? get() = if (iAmThePatient) null else status.nextStep()

    // RF-10.3: either party, until it is completed.
    val canCancel: Boolean get() = status == ServiceStatus.ASSIGNED || status == ServiceStatus.IN_PROGRESS
}

data class AttentionCancellation(
    val reason: String,
    val byMe: Boolean,
    val at: Instant,
)

// docs/decisions.md, 2026-11-04, HU-23: the engine leaves out what this reader may
// no longer see, so everything but the city can be absent.
data class AttentionPlace(
    val city: String,
    val addressText: String?,
    val reference: String?,
    val latitude: Double?,
    val longitude: Double?,
)

sealed interface AttentionStep {
    val targetStatus: ServiceStatus

    data object MarkArrival : AttentionStep {
        override val targetStatus: ServiceStatus get() = ServiceStatus.IN_PROGRESS
    }

    data object MarkCompleted : AttentionStep {
        override val targetStatus: ServiceStatus get() = ServiceStatus.COMPLETED
    }
}

fun ServiceStatus.nextStep(): AttentionStep? =
    when (this) {
        ServiceStatus.ASSIGNED -> AttentionStep.MarkArrival
        ServiceStatus.IN_PROGRESS -> AttentionStep.MarkCompleted
        ServiceStatus.COMPLETED, ServiceStatus.CANCELLED -> null
    }

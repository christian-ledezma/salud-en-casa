package bo.saludencasa.features.disputes.domain.model

import bo.saludencasa.features.services.domain.model.PaymentMethod
import java.math.BigDecimal
import java.time.Instant

// The email is there so the administrator can ask before resolving (RF-11.7);
// the conversation never is (INV-12).
data class DisputeParty(
    val name: String,
    val email: String,
)

// A payment the professional said they did not receive, as an administrator
// reviews it. Every time is sealed by the engine.
data class PaymentDispute(
    val paymentId: String,
    val requestId: String,
    val serviceTypeName: String,
    val amountBob: BigDecimal,
    val method: PaymentMethod?,
    val finishedAt: Instant?,
    val patientConfirmedAt: Instant,
    val disputedAt: Instant,
    val patient: DisputeParty,
    val professional: DisputeParty,
)

enum class DisputeResolution {
    PAID,
    NOT_PAID,
}

sealed interface DisputeError {
    data object NetworkUnavailable : DisputeError

    data object NotSignedIn : DisputeError

    // Someone resolved it first: a resolution is final.
    data object AlreadyResolved : DisputeError

    data object Unexpected : DisputeError
}

sealed interface DisputesResult {
    data class Loaded(
        val disputes: List<PaymentDispute>,
    ) : DisputesResult

    data class Failure(
        val error: DisputeError,
    ) : DisputesResult
}

sealed interface DisputesPageResult {
    data class Loaded(
        val disputes: List<PaymentDispute>,
        val endReached: Boolean,
    ) : DisputesPageResult

    data class Failure(
        val error: DisputeError,
    ) : DisputesPageResult
}

sealed interface DisputeResult {
    data class Loaded(
        val dispute: PaymentDispute,
    ) : DisputeResult

    // Resolved, or never a dispute this reader may see.
    data object NotFound : DisputeResult

    data class Failure(
        val error: DisputeError,
    ) : DisputeResult
}

sealed interface ResolveDisputeResult {
    data object Resolved : ResolveDisputeResult

    data class Failure(
        val error: DisputeError,
    ) : ResolveDisputeResult
}

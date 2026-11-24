package bo.saludencasa.features.services.domain.model

import java.time.Instant

enum class PaymentStatus {
    PENDING,
    PATIENT_CONFIRMED,
    PROFESSIONAL_CONFIRMED,
    BOTH_CONFIRMED,
    DISPUTED,
    RESOLVED_PAID,
    RESOLVED_NOT_PAID,
    CANCELLED,
}

enum class PaymentMethod {
    CASH,
    DIRECT_QR,
    GATEWAY,
    ;

    companion object {
        // FA-02: there is no gateway, and guard_payment_confirmation refuses it
        // from a client. Whoever declares first names one of these.
        val declarable: List<PaymentMethod> = listOf(CASH, DIRECT_QR)
    }
}

// The payment of an attention as either party reads it. The amount is the
// attention's own, whole for the professional (INV-10).
data class AttentionPayment(
    val status: PaymentStatus,
    val method: PaymentMethod?,
    val patientConfirmedAt: Instant?,
    val professionalConfirmedAt: Instant?,
    val disputedAt: Instant? = null,
    val resolvedAt: Instant? = null,
) {
    companion object {
        val Pending =
            AttentionPayment(
                PaymentStatus.PENDING,
                method = null,
                patientConfirmedAt = null,
                professionalConfirmedAt = null,
            )
    }
}

// RF-11.3, RF-11.5: what the professional says of the patient's confirmation.
enum class PaymentAnswer {
    RECEIVED,
    NOT_RECEIVED,
}

sealed interface ConfirmPaymentError {
    data object MethodNotAllowed : ConfirmPaymentError

    data class Service(
        val error: ServiceError,
    ) : ConfirmPaymentError
}

sealed interface ConfirmPaymentResult {
    data object Confirmed : ConfirmPaymentResult

    data class Failure(
        val error: ConfirmPaymentError,
    ) : ConfirmPaymentResult
}

sealed interface AnswerPaymentResult {
    data object Answered : AnswerPaymentResult

    data class Failure(
        val error: ServiceError,
    ) : AnswerPaymentResult
}

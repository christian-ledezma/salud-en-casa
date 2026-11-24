package bo.saludencasa.features.services.data.mapper

import bo.saludencasa.features.services.data.model.AttentionDto
import bo.saludencasa.features.services.data.model.AttentionSummaryDto
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionCancellation
import bo.saludencasa.features.services.domain.model.AttentionPayment
import bo.saludencasa.features.services.domain.model.AttentionPlace
import bo.saludencasa.features.services.domain.model.AttentionSummary
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.services.domain.model.PaymentStatus
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime

// A status or timestamp the client does not recognise is a contract mismatch, not
// a value to guess at: it maps to null so the repository reports Unexpected.
internal fun AttentionDto.toAttention(): Attention? {
    val parsedStatus = ServiceStatus.entries.firstOrNull { it.name == status } ?: return null
    val parsedCreatedAt = parseInstant(createdAt) ?: return null

    return Attention(
        serviceId = serviceId,
        requestId = requestId,
        status = parsedStatus,
        finalAmountBob = finalAmountBob,
        serviceTypeName = serviceTypeName,
        counterpart =
            Counterpart(
                id = counterpartId,
                name = counterpartName,
                photoUrl = counterpartPhotoUrl,
            ),
        iAmThePatient = iAmPatient,
        createdAt = parsedCreatedAt,
        scheduledAt = scheduledAt?.let { parseInstant(it) ?: return null },
        startedAt = startedAt?.let { parseInstant(it) ?: return null },
        finishedAt = finishedAt?.let { parseInstant(it) ?: return null },
        description = description,
        place =
            AttentionPlace(
                city = city,
                addressText = addressText,
                reference = reference,
                latitude = latitude,
                longitude = longitude,
            ),
        conversationOpen = conversationOpen,
        conversationWritableUntil = conversationWritableUntil?.let { parseInstant(it) ?: return null },
        cancellation =
            cancelledAt?.let { at ->
                AttentionCancellation(
                    reason = cancellationReason ?: return null,
                    byMe = cancelledByMe ?: return null,
                    at = parseInstant(at) ?: return null,
                )
            },
        payment =
            AttentionPayment(
                status = PaymentStatus.entries.firstOrNull { it.name == paymentStatus } ?: return null,
                method =
                    paymentMethod?.let { raw ->
                        PaymentMethod.entries.firstOrNull { it.name == raw } ?: return null
                    },
                patientConfirmedAt = patientConfirmedAt?.let { parseInstant(it) ?: return null },
                professionalConfirmedAt = professionalConfirmedAt?.let { parseInstant(it) ?: return null },
                disputedAt = disputedAt?.let { parseInstant(it) ?: return null },
                resolvedAt = resolvedAt?.let { parseInstant(it) ?: return null },
            ),
    )
}

internal fun AttentionSummaryDto.toAttentionSummary(): AttentionSummary? {
    val parsedStatus = ServiceStatus.entries.firstOrNull { it.name == status } ?: return null
    return AttentionSummary(
        requestId = requestId,
        status = parsedStatus,
        finalAmountBob = finalAmountBob,
        serviceTypeName = serviceTypeName,
        counterpartName = counterpartName,
        createdAt = parseInstant(createdAt) ?: return null,
        scheduledAt = scheduledAt?.let { parseInstant(it) ?: return null },
        startedAt = startedAt?.let { parseInstant(it) ?: return null },
    )
}

// PostgREST writes the offset as +00:00, which Instant.parse does not take.
private fun parseInstant(text: String): Instant? = runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()

internal fun Throwable.toServiceError(): ServiceError =
    when {
        TRANSITION_REFUSALS.any { message?.contains(it) == true } -> {
            ServiceError.AttentionChanged
        }

        this is HttpRequestTimeoutException || this is HttpRequestException || this is IOException -> {
            ServiceError.NetworkUnavailable
        }

        else -> {
            ServiceError.Unexpected
        }
    }

// Raised by guard_service_transition and, when both parties cancel at once, by
// guard_service_direct_update; the last five by guard_payment_confirmation, when
// the payment or the attention moved before its party declared or answered.
private val TRANSITION_REFUSALS =
    listOf(
        "invalid_service_transition",
        "service_status_is_terminal",
        "cancellation_reason_is_written_once",
        "patient_confirms_a_pending_payment_once",
        "payment_confirmation_requires_a_completed_service",
        "professional_answers_a_patient_confirmed_payment_once",
        "professional_declares_a_pending_payment_once",
        "patient_only_accepts_the_professionals_receipt",
    )

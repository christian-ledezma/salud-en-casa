package bo.saludencasa.features.disputes.data.mapper

import bo.saludencasa.features.disputes.data.model.PaymentDisputeDto
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.model.DisputeParty
import bo.saludencasa.features.disputes.domain.model.PaymentDispute
import bo.saludencasa.features.services.domain.model.PaymentMethod
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime

// A method or time the client does not recognise is a contract mismatch: it maps
// to null so the repository reports Unexpected.
internal fun PaymentDisputeDto.toPaymentDispute(): PaymentDispute? =
    PaymentDispute(
        paymentId = paymentId,
        requestId = requestId,
        serviceTypeName = serviceTypeName,
        amountBob = totalAmountBob,
        method = method?.let { raw -> PaymentMethod.entries.firstOrNull { it.name == raw } ?: return null },
        finishedAt = finishedAt?.let { parseInstant(it) ?: return null },
        patientConfirmedAt = parseInstant(patientConfirmedAt) ?: return null,
        disputedAt = parseInstant(disputedAt) ?: return null,
        patient = DisputeParty(name = patientName, email = patientEmail),
        professional = DisputeParty(name = professionalName, email = professionalEmail),
    )

// PostgREST writes the offset as +00:00, which Instant.parse does not take.
private fun parseInstant(text: String): Instant? = runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()

internal fun Throwable.toDisputeError(): DisputeError =
    when {
        // Raised by guard_payment_confirmation when the payment is no longer a
        // dispute by the time the resolution reaches it.
        message?.contains("an_administrator_only_resolves_a_dispute") == true -> {
            DisputeError.AlreadyResolved
        }

        this is HttpRequestTimeoutException || this is HttpRequestException || this is IOException -> {
            DisputeError.NetworkUnavailable
        }

        else -> {
            DisputeError.Unexpected
        }
    }

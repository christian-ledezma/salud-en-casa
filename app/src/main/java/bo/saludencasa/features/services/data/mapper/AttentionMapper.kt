package bo.saludencasa.features.services.data.mapper

import bo.saludencasa.features.services.data.model.AttentionDto
import bo.saludencasa.features.services.data.model.AttentionSummaryDto
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionSummary
import bo.saludencasa.features.services.domain.model.Counterpart
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
    )
}

internal fun AttentionSummaryDto.toAttentionSummary(): AttentionSummary? {
    val parsedStatus = ServiceStatus.entries.firstOrNull { it.name == status } ?: return null
    return AttentionSummary(
        requestId = requestId,
        status = parsedStatus,
        finalAmountBob = finalAmountBob,
        serviceTypeName = serviceTypeName,
        patientName = patientName,
        createdAt = parseInstant(createdAt) ?: return null,
        scheduledAt = scheduledAt?.let { parseInstant(it) ?: return null },
    )
}

// PostgREST writes the offset as +00:00, which Instant.parse does not take.
private fun parseInstant(text: String): Instant? = runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()

internal fun Throwable.toServiceError(): ServiceError =
    when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> ServiceError.NetworkUnavailable
        else -> ServiceError.Unexpected
    }

package bo.saludencasa.features.services.data.mapper

import bo.saludencasa.features.services.data.model.AttentionDto
import bo.saludencasa.features.services.data.model.AttentionSummaryDto
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionPlace
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

// Raised by guard_service_transition.
private val TRANSITION_REFUSALS = listOf("invalid_service_transition", "service_status_is_terminal")

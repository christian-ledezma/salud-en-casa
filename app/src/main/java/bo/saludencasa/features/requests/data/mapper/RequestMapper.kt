package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.data.model.BookableStartDto
import bo.saludencasa.features.requests.data.model.InboxRequestDto
import bo.saludencasa.features.requests.data.model.ServiceRequestDto
import bo.saludencasa.features.requests.domain.model.BookableStart
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import kotlin.math.roundToInt

internal fun ServiceRequestDto.toServiceRequest(): ServiceRequest? {
    val mappedStatus = RequestStatus.entries.firstOrNull { it.name == status } ?: return null
    val mappedModality = RequestModality.entries.firstOrNull { it.name == modality } ?: return null
    val created = parseInstant(createdAt) ?: return null
    val expires = expiresAt?.let(::parseInstant)
    val scheduled = scheduledAt?.let(::parseInstant)
    if (mappedModality == RequestModality.SCHEDULED && scheduled == null) return null
    val budget = suggestedBudgetBob

    return ServiceRequest(
        id = id,
        serviceTypeName = serviceTypeName,
        addressText = addressText,
        city = city,
        status = mappedStatus,
        modality = mappedModality,
        description = description?.takeIf { it.isNotBlank() },
        suggestedBudgetBob = budget,
        expiresAt = expires,
        createdAt = created,
        scheduledAt = scheduled,
        professionalName = professionalName?.takeIf { it.isNotBlank() },
        professionalId = professionalId,
    )
}

internal fun InboxRequestDto.toInboxRequest(): InboxRequest? {
    val mappedStatus = RequestStatus.entries.firstOrNull { it.name == status } ?: return null
    val mappedModality = RequestModality.entries.firstOrNull { it.name == modality } ?: return null
    val created = parseInstant(createdAt) ?: return null
    val scheduled = scheduledAt?.let(::parseInstant)
    if (mappedModality == RequestModality.SCHEDULED && scheduled == null) return null

    return InboxRequest(
        id = id,
        serviceTypeName = serviceTypeName,
        city = city,
        description = description?.takeIf { it.isNotBlank() },
        suggestedBudgetBob = suggestedBudgetBob,
        distanceM = distanceM.roundToInt(),
        status = mappedStatus,
        createdAt = created,
        modality = mappedModality,
        scheduledAt = scheduled,
    )
}

internal fun BookableStartDto.toBookableStart(): BookableStart? {
    val start = parseInstant(startAt) ?: return null
    val date = runCatching { LocalDate.parse(localDate) }.getOrNull() ?: return null
    val time = runCatching { LocalTime.parse(localTime) }.getOrNull() ?: return null
    return BookableStart(startAt = start, date = date, time = time)
}

// PostgREST writes the offset as +00:00, which Instant.parse does not take.
private fun parseInstant(text: String): Instant? = runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()

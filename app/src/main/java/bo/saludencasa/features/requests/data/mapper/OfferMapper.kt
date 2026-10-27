package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.data.model.OfferableRequestDto
import bo.saludencasa.features.requests.data.model.RequestOfferDto
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferableRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestOffer
import bo.saludencasa.features.requests.domain.model.RequestStatus
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.roundToInt

internal fun RequestOfferDto.toRequestOffer(): RequestOffer? {
    val mappedIssuer = OfferIssuer.entries.firstOrNull { it.name == issuer } ?: return null
    val created = parseOfferInstant(createdAt) ?: return null

    return RequestOffer(
        id = id,
        professionalId = professionalId,
        professionalName = professionalName?.takeIf { it.isNotBlank() },
        professionalPhotoUrl = professionalPhotoUrl?.takeIf { it.isNotBlank() },
        professionalRating = professionalRating,
        amountBob = amountBob,
        message = message?.takeIf { it.isNotBlank() },
        issuer = mappedIssuer,
        createdAt = created,
    )
}

internal fun OfferableRequestDto.toOfferableRequest(): OfferableRequest? {
    val mappedStatus = RequestStatus.entries.firstOrNull { it.name == status } ?: return null
    val mappedModality = RequestModality.entries.firstOrNull { it.name == modality } ?: return null
    val scheduled = scheduledAt?.let(::parseOfferInstant)
    if (mappedModality == RequestModality.SCHEDULED && scheduled == null) return null

    return OfferableRequest(
        id = id,
        serviceTypeName = serviceTypeName,
        city = city,
        description = description?.takeIf { it.isNotBlank() },
        suggestedBudgetBob = suggestedBudgetBob,
        distanceM = distanceM.roundToInt(),
        status = mappedStatus,
        modality = mappedModality,
        scheduledAt = scheduled,
        alreadyOffered = alreadyOffered,
    )
}

// PostgREST writes the offset as +00:00, which Instant.parse does not take.
private fun parseOfferInstant(text: String): Instant? =
    runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()

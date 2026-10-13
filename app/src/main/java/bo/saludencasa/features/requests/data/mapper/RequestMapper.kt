package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.data.model.InboxRequestDto
import bo.saludencasa.features.requests.data.model.ServiceRequestDto
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import java.math.BigDecimal
import java.time.OffsetDateTime
import kotlin.math.roundToInt

internal fun ServiceRequestDto.toServiceRequest(): ServiceRequest? {
    val mappedStatus = RequestStatus.entries.firstOrNull { it.name == status } ?: return null
    val mappedModality = RequestModality.entries.firstOrNull { it.name == modality } ?: return null
    val created = runCatching { OffsetDateTime.parse(createdAt).toInstant() }.getOrNull() ?: return null
    val expires = expiresAt?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
    val budget = suggestedBudgetBob?.let { runCatching { BigDecimal(it) }.getOrNull() }

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
    )
}

internal fun InboxRequestDto.toInboxRequest(): InboxRequest? {
    val mappedStatus = RequestStatus.entries.firstOrNull { it.name == status } ?: return null
    val created = runCatching { OffsetDateTime.parse(createdAt).toInstant() }.getOrNull() ?: return null

    return InboxRequest(
        id = id,
        serviceTypeName = serviceTypeName,
        city = city,
        description = description?.takeIf { it.isNotBlank() },
        suggestedBudgetBob = suggestedBudgetBob,
        distanceM = distanceM.roundToInt(),
        status = mappedStatus,
        createdAt = created,
    )
}

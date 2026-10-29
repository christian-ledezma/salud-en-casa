package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.data.model.OfferableRequestDto
import bo.saludencasa.features.requests.data.model.RequestOfferDto
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferStatus
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.OfferableRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestOffer
import bo.saludencasa.features.requests.domain.model.RequestStatus
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.roundToInt

internal fun RequestOfferDto.toRequestOffer(): RequestOffer? {
    val mappedIssuer = OfferIssuer.entries.firstOrNull { it.name == issuer } ?: return null
    val mappedStatus = OfferStatus.entries.firstOrNull { it.name == status } ?: return null
    val created = parseOfferInstant(createdAt) ?: return null

    return RequestOffer(
        id = id,
        parentOfferId = parentOfferId,
        professionalId = professionalId,
        professionalName = professionalName?.takeIf { it.isNotBlank() },
        professionalPhotoUrl = professionalPhotoUrl?.takeIf { it.isNotBlank() },
        professionalRating = professionalRating,
        amountBob = amountBob,
        message = message?.takeIf { it.isNotBlank() },
        issuer = mappedIssuer,
        status = mappedStatus,
        createdAt = created,
    )
}

// One thread per professional, each in the order the rows were written. The
// server already orders them, and the chain is rebuilt from the parent reference
// only to put a row that shares its timestamp with another in its place: two
// offers written in the same transaction carry the same created_at, so the
// parent is what orders a thread (docs/decisions.md, 2026-10-27).
internal fun List<RequestOfferDto>.toOfferThreads(signedInUserId: String): List<OfferThread>? {
    val offers = map { it.toRequestOffer() ?: return null }

    val distances = associate { it.professionalId to it.distanceM?.roundToInt() }
    val budget = firstNotNullOfOrNull(RequestOfferDto::suggestedBudgetBob)

    return offers
        .groupBy(RequestOffer::professionalId)
        .map { (professionalId, rows) ->
            val chain = rows.inThreadOrder()
            OfferThread(
                professionalId = professionalId,
                professionalName = chain.firstNotNullOfOrNull(RequestOffer::professionalName),
                professionalPhotoUrl = chain.firstNotNullOfOrNull(RequestOffer::professionalPhotoUrl),
                professionalRating = chain.firstNotNullOfOrNull(RequestOffer::professionalRating),
                offers = chain,
                iAmTheProfessional = professionalId == signedInUserId,
                distanceM = distances[professionalId],
                suggestedBudgetBob = budget,
            )
        }.sortedByDescending { it.offers.last().createdAt }
}

private fun List<RequestOffer>.inThreadOrder(): List<RequestOffer> {
    val byParent = associateBy(RequestOffer::parentOfferId)
    val chain = mutableListOf<RequestOffer>()
    var next = byParent[null]
    while (next != null && chain.size < size) {
        chain += next
        next = byParent[next.id]
    }
    // A chain that does not come out whole is one with a row missing, and the
    // order the engine sent is what is left to go by: re-sorting here would put
    // rows that share a timestamp in an order of its own.
    return if (chain.size == size) chain else this
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

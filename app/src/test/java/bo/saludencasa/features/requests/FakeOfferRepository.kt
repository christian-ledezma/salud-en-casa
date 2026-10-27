package bo.saludencasa.features.requests

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferableRequest
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestOffer
import bo.saludencasa.features.requests.domain.model.RequestOffersResult
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.repository.IOfferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigDecimal
import java.time.Instant

class FakeOfferRepository : IOfferRepository {
    data class Submission(
        val requestId: String,
        val amount: AmountBob,
        val message: String,
    )

    val submitted = mutableListOf<Submission>()

    var submitResult: OfferSubmitResult = OfferSubmitResult.Success
    var offersResult: RequestOffersResult = RequestOffersResult.Loaded(emptyList())
    var offerableResult: OfferableRequestResult = OfferableRequestResult.NotFound

    // A hot flow the test drives: emitting a Unit stands in for the broadcast
    // that tells the patient to re-query the offers.
    val offerSignals = MutableSharedFlow<Unit>()

    override suspend fun submitOffer(
        requestId: String,
        amount: AmountBob,
        message: String,
    ): OfferSubmitResult {
        submitted += Submission(requestId, amount, message)
        return submitResult
    }

    override suspend fun getOffersForRequest(requestId: String): RequestOffersResult = offersResult

    override suspend fun getOfferableRequest(requestId: String): OfferableRequestResult = offerableResult

    override fun observeMyOfferSignals(): Flow<Unit> = offerSignals
}

fun requestOffer(
    id: String = "offer-1",
    professionalName: String? = "Ana Pérez",
    professionalRating: BigDecimal? = BigDecimal("4.80"),
    amountBob: BigDecimal = BigDecimal("160.00"),
    message: String? = "Llevo el material.",
    issuer: OfferIssuer = OfferIssuer.PROFESSIONAL,
): RequestOffer =
    RequestOffer(
        id = id,
        professionalId = "professional-1",
        professionalName = professionalName,
        professionalPhotoUrl = null,
        professionalRating = professionalRating,
        amountBob = amountBob,
        message = message,
        issuer = issuer,
        createdAt = Instant.parse("2026-10-23T18:00:00Z"),
    )

fun offerableRequest(
    id: String = "request-1",
    alreadyOffered: Boolean = false,
    status: RequestStatus = RequestStatus.PUBLISHED,
    modality: RequestModality = RequestModality.IMMEDIATE,
    scheduledAt: Instant? = null,
): OfferableRequest =
    OfferableRequest(
        id = id,
        serviceTypeName = "Vacunación",
        city = "Cochabamba",
        description = "Descripción de la solicitud.",
        suggestedBudgetBob = BigDecimal("150.00"),
        distanceM = 1200,
        status = status,
        modality = modality,
        scheduledAt = scheduledAt,
        alreadyOffered = alreadyOffered,
    )

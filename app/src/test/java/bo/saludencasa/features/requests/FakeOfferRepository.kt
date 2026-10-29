package bo.saludencasa.features.requests

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.OfferActionResult
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferStatus
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.OfferThreadResult
import bo.saludencasa.features.requests.domain.model.OfferThreadsResult
import bo.saludencasa.features.requests.domain.model.OfferableRequest
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestOffer
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.repository.IOfferRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigDecimal
import java.time.Instant

class FakeOfferRepository : IOfferRepository {
    data class Submission(
        val requestId: String,
        val professionalId: String?,
        val parentOfferId: String?,
        val amount: AmountBob,
        val message: String?,
    )

    val submitted = mutableListOf<Submission>()
    val rejected = mutableListOf<String>()
    val signalSides = mutableListOf<Boolean>()

    var submitResult: OfferSubmitResult = OfferSubmitResult.Success
    var threadsResult: OfferThreadsResult = OfferThreadsResult.Loaded(emptyList())
    var threadResult: OfferThreadResult = OfferThreadResult.NotFound
    var offerableResult: OfferableRequestResult = OfferableRequestResult.NotFound
    var rejectResult: OfferActionResult = OfferActionResult.Success

    // A hot flow the test drives: emitting a Unit stands in for the broadcast
    // that tells one side to re-query.
    val offerSignals = MutableSharedFlow<Unit>()

    // Holds a submission in flight, so a test can see what the screen does while
    // the network has not answered yet.
    var holdSubmit: CompletableDeferred<Unit>? = null

    override suspend fun submitOffer(
        requestId: String,
        amount: AmountBob,
        message: String?,
    ): OfferSubmitResult {
        submitted +=
            Submission(requestId, professionalId = null, parentOfferId = null, amount = amount, message = message)
        return submitResult
    }

    override suspend fun submitCounterOffer(
        requestId: String,
        professionalId: String,
        parentOfferId: String,
        amount: AmountBob,
        message: String?,
    ): OfferSubmitResult {
        holdSubmit?.await()
        submitted += Submission(requestId, professionalId, parentOfferId, amount, message)
        return submitResult
    }

    override suspend fun getOfferThreads(requestId: String): OfferThreadsResult = threadsResult

    override suspend fun getOfferThread(
        requestId: String,
        professionalId: String?,
    ): OfferThreadResult = threadResult

    override suspend fun getOfferableRequest(requestId: String): OfferableRequestResult = offerableResult

    override suspend fun rejectOffer(offerId: String): OfferActionResult {
        rejected += offerId
        return rejectResult
    }

    override fun observeOfferSignals(asProfessional: Boolean): Flow<Unit> {
        signalSides += asProfessional
        return offerSignals
    }
}

fun requestOffer(
    id: String = "offer-1",
    parentOfferId: String? = null,
    professionalName: String? = "Ana Pérez",
    professionalRating: BigDecimal? = BigDecimal("4.80"),
    amountBob: BigDecimal = BigDecimal("160.00"),
    message: String? = "Llevo el material.",
    issuer: OfferIssuer = OfferIssuer.PROFESSIONAL,
    status: OfferStatus = OfferStatus.PROPOSED,
): RequestOffer =
    RequestOffer(
        id = id,
        parentOfferId = parentOfferId,
        professionalId = "professional-1",
        professionalName = professionalName,
        professionalPhotoUrl = null,
        professionalRating = professionalRating,
        amountBob = amountBob,
        message = message,
        issuer = issuer,
        status = status,
        createdAt = Instant.parse("2026-10-27T18:00:00Z"),
    )

fun offerThread(
    professionalId: String = "professional-1",
    professionalName: String? = "Ana Pérez",
    professionalRating: BigDecimal? = BigDecimal("4.80"),
    offers: List<RequestOffer> = listOf(requestOffer()),
    iAmTheProfessional: Boolean = false,
): OfferThread =
    OfferThread(
        professionalId = professionalId,
        professionalName = professionalName,
        professionalPhotoUrl = null,
        professionalRating = professionalRating,
        offers = offers,
        iAmTheProfessional = iAmTheProfessional,
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

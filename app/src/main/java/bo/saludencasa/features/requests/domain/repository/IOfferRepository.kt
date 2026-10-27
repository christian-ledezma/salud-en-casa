package bo.saludencasa.features.requests.domain.repository

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.model.RequestOffersResult
import kotlinx.coroutines.flow.Flow

interface IOfferRepository {
    // The professional issues the first offer of a thread (RF-08.1).
    suspend fun submitOffer(
        requestId: String,
        amount: AmountBob,
        message: String,
    ): OfferSubmitResult

    // The offers the patient received on their own request, newest work first.
    suspend fun getOffersForRequest(requestId: String): RequestOffersResult

    // The request the professional is about to offer on, with already-offered.
    suspend fun getOfferableRequest(requestId: String): OfferableRequestResult

    // Emits once each time an offer on one of the caller's requests may have
    // changed, so the patient re-queries. It carries no data (docs/decisions.md,
    // 2026-10-23): the signal says "ask again", getOffersForRequest says what.
    fun observeMyOfferSignals(): Flow<Unit>
}

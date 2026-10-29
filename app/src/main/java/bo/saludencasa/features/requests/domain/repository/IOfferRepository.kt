package bo.saludencasa.features.requests.domain.repository

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.OfferActionResult
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferThreadResult
import bo.saludencasa.features.requests.domain.model.OfferThreadsResult
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import kotlinx.coroutines.flow.Flow

interface IOfferRepository {
    // The professional issues the first offer of a thread (RF-08.1). The message is
    // optional (docs/decisions.md, 2026-10-27, HT-24).
    suspend fun submitOffer(
        requestId: String,
        amount: AmountBob,
        message: String?,
    ): OfferSubmitResult

    // Either side answers the offer on the table with another amount (RF-08.2).
    // The issuer is decided by who the caller is on this thread, never passed in.
    suspend fun submitCounterOffer(
        requestId: String,
        professionalId: String,
        parentOfferId: String,
        amount: AmountBob,
        message: String?,
    ): OfferSubmitResult

    // The threads on the patient's own request, one per professional (RF-08.3).
    suspend fun getOfferThreads(requestId: String): OfferThreadsResult

    // One chain, read by whichever side opens it (RF-08.3). A null professionalId
    // means the caller is the professional of the thread, so their own identifier
    // never has to travel through navigation or a notification to come back.
    suspend fun getOfferThread(
        requestId: String,
        professionalId: String?,
    ): OfferThreadResult

    // The request the professional is about to offer on, with already-offered.
    suspend fun getOfferableRequest(requestId: String): OfferableRequestResult

    // Rejecting the offer on the table. The chain closes and the request stays
    // open to other professionals (RF-08.4 of HU-18's criterion 4).
    suspend fun rejectOffer(offerId: String): OfferActionResult

    // Accepting the offer on the table seals the agreement in one transaction
    // (RF-08.5): the service and its pending payment are created and the request
    // moves to accepted. A refusal means the thread moved on, so the caller
    // re-reads.
    suspend fun acceptOffer(offerId: String): OfferActionResult

    // Emits once each time an offer the caller takes part in may have changed, so
    // the screen re-queries. It carries no data (docs/decisions.md, 2026-10-23):
    // the signal says "ask again", the read says what. Each role has its own
    // topic, so the caller says which side it is listening as.
    fun observeOfferSignals(asProfessional: Boolean): Flow<Unit>
}

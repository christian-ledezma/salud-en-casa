package bo.saludencasa.features.requests.domain.repository

import bo.saludencasa.features.requests.domain.model.ActiveRequestResult
import bo.saludencasa.features.requests.domain.model.BookableTimesResult
import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.model.CreateImmediateRequestDraft
import bo.saludencasa.features.requests.domain.model.CreateScheduledRequestDraft
import bo.saludencasa.features.requests.domain.model.InboxResult
import bo.saludencasa.features.requests.domain.model.MyRequestsResult
import bo.saludencasa.features.requests.domain.model.RequestResult
import kotlinx.coroutines.flow.Flow

interface IRequestRepository {
    suspend fun createImmediate(draft: CreateImmediateRequestDraft): RequestResult

    suspend fun createScheduled(draft: CreateScheduledRequestDraft): RequestResult

    suspend fun getBookableTimes(
        professionalId: String,
        serviceTypeId: String,
    ): BookableTimesResult

    suspend fun getMyRequests(
        offset: Int,
        limit: Int,
    ): MyRequestsResult

    // The newest immediate request of the caller that is still open to offers.
    suspend fun getActiveImmediate(): ActiveRequestResult

    suspend fun cancel(requestId: String): CancelRequestResult

    // HU-14: the professional's open inbox, one page at a time.
    suspend fun getInbox(
        offset: Int,
        limit: Int,
    ): InboxResult

    // Emits once each time the inbox may have changed, so the caller re-queries.
    // It carries no data: the signal says "ask again", getInbox says what.
    fun inboxSignals(): Flow<Unit>
}

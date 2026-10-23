package bo.saludencasa.features.requests.data.repository

import bo.saludencasa.features.requests.data.datasource.SupabaseRequestDataSource
import bo.saludencasa.features.requests.data.mapper.toBookableStart
import bo.saludencasa.features.requests.data.mapper.toInboxRequest
import bo.saludencasa.features.requests.data.mapper.toRequestError
import bo.saludencasa.features.requests.data.mapper.toServiceRequest
import bo.saludencasa.features.requests.data.model.BookableTimesParams
import bo.saludencasa.features.requests.data.model.CreateImmediateRequestParams
import bo.saludencasa.features.requests.data.model.CreateScheduledRequestParams
import bo.saludencasa.features.requests.domain.model.ActiveRequestResult
import bo.saludencasa.features.requests.domain.model.BookableTimes
import bo.saludencasa.features.requests.domain.model.BookableTimesResult
import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.model.CreateImmediateRequestDraft
import bo.saludencasa.features.requests.domain.model.CreateScheduledRequestDraft
import bo.saludencasa.features.requests.domain.model.InboxResult
import bo.saludencasa.features.requests.domain.model.MyRequestsResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class RequestRepository(
    private val dataSource: SupabaseRequestDataSource,
) : IRequestRepository {
    override suspend fun createImmediate(draft: CreateImmediateRequestDraft): RequestResult {
        if (dataSource.currentUserId() == null) {
            return RequestResult.Failure(RequestError.NotSignedIn)
        }

        return try {
            val requestId =
                dataSource.createImmediate(
                    CreateImmediateRequestParams(
                        addressId = draft.addressId,
                        serviceTypeId = draft.serviceTypeId,
                        description = draft.description,
                        suggestedBudgetBob = draft.suggestedBudgetBob?.toPlainString(),
                    ),
                )
            RequestResult.Created(requestId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            RequestResult.Failure(failure.toRequestError())
        }
    }

    override suspend fun createScheduled(draft: CreateScheduledRequestDraft): RequestResult {
        if (dataSource.currentUserId() == null) {
            return RequestResult.Failure(RequestError.NotSignedIn)
        }

        return try {
            val requestId =
                dataSource.createScheduled(
                    CreateScheduledRequestParams(
                        professionalId = draft.professionalId,
                        addressId = draft.addressId,
                        serviceTypeId = draft.serviceTypeId,
                        scheduledAt = draft.scheduledAt.toString(),
                        description = draft.description,
                        suggestedBudgetBob = draft.suggestedBudgetBob?.toPlainString(),
                    ),
                )
            RequestResult.Created(requestId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            RequestResult.Failure(failure.toRequestError())
        }
    }

    override suspend fun getBookableTimes(
        professionalId: String,
        serviceTypeId: String,
    ): BookableTimesResult {
        if (dataSource.currentUserId() == null) {
            return BookableTimesResult.Failure(RequestError.NotSignedIn)
        }

        return try {
            val rows = dataSource.fetchBookableTimes(BookableTimesParams(professionalId, serviceTypeId))
            val starts = rows.map { it.toBookableStart() }

            if (starts.any { it == null }) {
                BookableTimesResult.Failure(RequestError.Unexpected)
            } else {
                BookableTimesResult.Loaded(BookableTimes(starts.filterNotNull()))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            BookableTimesResult.Failure(failure.toRequestError())
        }
    }

    override suspend fun getMyRequests(
        offset: Int,
        limit: Int,
    ): MyRequestsResult {
        if (dataSource.currentUserId() == null) {
            return MyRequestsResult.Failure(RequestError.NotSignedIn)
        }

        return try {
            val rows = dataSource.findMyRequests(offset = offset, limit = limit)
            val requests = rows.map { it.toServiceRequest() }

            if (requests.any { it == null }) {
                MyRequestsResult.Failure(RequestError.Unexpected)
            } else {
                MyRequestsResult.Loaded(requests.filterNotNull())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            MyRequestsResult.Failure(failure.toRequestError())
        }
    }

    override suspend fun getActiveImmediate(): ActiveRequestResult {
        if (dataSource.currentUserId() == null) {
            return ActiveRequestResult.Failure(RequestError.NotSignedIn)
        }

        return try {
            val row = dataSource.findActiveImmediate() ?: return ActiveRequestResult.None
            row.toServiceRequest()?.let(ActiveRequestResult::Found)
                ?: ActiveRequestResult.Failure(RequestError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ActiveRequestResult.Failure(failure.toRequestError())
        }
    }

    override suspend fun cancel(requestId: String): CancelRequestResult {
        if (dataSource.currentUserId() == null) {
            return CancelRequestResult.Failure(RequestError.NotSignedIn)
        }

        return try {
            dataSource.cancel(requestId)
            CancelRequestResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            CancelRequestResult.Failure(failure.toRequestError())
        }
    }

    override suspend fun getInbox(
        offset: Int,
        limit: Int,
    ): InboxResult {
        if (dataSource.currentUserId() == null) {
            return InboxResult.Failure(RequestError.NotSignedIn)
        }

        return try {
            val rows = dataSource.fetchInbox(offset = offset, limit = limit)
            val requests = rows.map { it.toInboxRequest() }

            if (requests.any { it == null }) {
                InboxResult.Failure(RequestError.Unexpected)
            } else {
                InboxResult.Loaded(requests.filterNotNull())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            InboxResult.Failure(failure.toRequestError())
        }
    }

    override fun inboxSignals(): Flow<Unit> = dataSource.inboxSignals()
}

package bo.saludencasa.features.requests.data.repository

import bo.saludencasa.features.requests.data.datasource.SupabaseRequestDataSource
import bo.saludencasa.features.requests.data.mapper.toRequestError
import bo.saludencasa.features.requests.data.mapper.toServiceRequest
import bo.saludencasa.features.requests.data.model.CreateImmediateRequestParams
import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.model.CreateImmediateRequestDraft
import bo.saludencasa.features.requests.domain.model.MyRequestsResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository
import kotlinx.coroutines.CancellationException

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
}

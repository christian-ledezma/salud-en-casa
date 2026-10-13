package bo.saludencasa.features.requests.domain.repository

import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.model.CreateImmediateRequestDraft
import bo.saludencasa.features.requests.domain.model.MyRequestsResult
import bo.saludencasa.features.requests.domain.model.RequestResult

interface IRequestRepository {
    suspend fun createImmediate(draft: CreateImmediateRequestDraft): RequestResult

    suspend fun getMyRequests(
        offset: Int,
        limit: Int,
    ): MyRequestsResult

    suspend fun cancel(requestId: String): CancelRequestResult
}

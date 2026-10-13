package bo.saludencasa.features.requests

import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.model.CreateImmediateRequestDraft
import bo.saludencasa.features.requests.domain.model.MyRequestsResult
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import bo.saludencasa.features.requests.domain.repository.IRequestRepository
import java.math.BigDecimal
import java.time.Instant

class FakeRequestRepository : IRequestRepository {
    val createdDrafts = mutableListOf<CreateImmediateRequestDraft>()
    val cancelledIds = mutableListOf<String>()
    val pageRequests = mutableListOf<Pair<Int, Int>>()

    var createResult: RequestResult = RequestResult.Created("request-1")
    var cancelResult: CancelRequestResult = CancelRequestResult.Success

    // A responder so a test can answer differently per page (pagination), and a
    // single default for the simple cases.
    var requestsResult: MyRequestsResult = MyRequestsResult.Loaded(emptyList())
    var requestsResponder: ((offset: Int, limit: Int) -> MyRequestsResult)? = null

    override suspend fun createImmediate(draft: CreateImmediateRequestDraft): RequestResult {
        createdDrafts += draft
        return createResult
    }

    override suspend fun getMyRequests(
        offset: Int,
        limit: Int,
    ): MyRequestsResult {
        pageRequests += offset to limit
        return requestsResponder?.invoke(offset, limit) ?: requestsResult
    }

    override suspend fun cancel(requestId: String): CancelRequestResult {
        cancelledIds += requestId
        return cancelResult
    }
}

fun serviceRequest(
    id: String = "request-1",
    serviceTypeName: String = "Vacunación",
    status: RequestStatus = RequestStatus.PUBLISHED,
    modality: RequestModality = RequestModality.IMMEDIATE,
    suggestedBudgetBob: BigDecimal? = BigDecimal("150.00"),
): ServiceRequest =
    ServiceRequest(
        id = id,
        serviceTypeName = serviceTypeName,
        addressText = "Avenida América 1234, Cochabamba",
        city = "Cochabamba",
        status = status,
        modality = modality,
        description = null,
        suggestedBudgetBob = suggestedBudgetBob,
        expiresAt = Instant.parse("2026-10-12T13:00:00Z"),
        createdAt = Instant.parse("2026-10-12T12:00:00Z"),
    )

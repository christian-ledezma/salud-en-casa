package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.MyRequestsPageResult
import bo.saludencasa.features.requests.domain.model.MyRequestsResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository

class GetMyRequestsUseCase(
    private val repository: IRequestRepository,
) {
    // RNF-08: no query returns an unbounded collection. endReached is derived
    // from a short page, the same way the nearby search decides it.
    suspend operator fun invoke(offset: Int): MyRequestsPageResult =
        when (val result = repository.getMyRequests(offset = offset, limit = PAGE_SIZE)) {
            is MyRequestsResult.Loaded -> {
                MyRequestsPageResult.Loaded(
                    requests = result.requests,
                    endReached = result.requests.size < PAGE_SIZE,
                )
            }

            is MyRequestsResult.Failure -> {
                MyRequestsPageResult.Failure(result.error)
            }
        }

    companion object {
        const val PAGE_SIZE = 20
    }
}

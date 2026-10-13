package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.InboxPageResult
import bo.saludencasa.features.requests.domain.model.InboxResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository

class GetInboxUseCase(
    private val repository: IRequestRepository,
) {
    // RNF-08: no query returns an unbounded collection. endReached is derived
    // from a short page, the same way the nearby search decides it.
    suspend operator fun invoke(offset: Int): InboxPageResult =
        when (val result = repository.getInbox(offset = offset, limit = PAGE_SIZE)) {
            is InboxResult.Loaded -> {
                InboxPageResult.Loaded(
                    requests = result.requests,
                    endReached = result.requests.size < PAGE_SIZE,
                )
            }

            is InboxResult.Failure -> {
                InboxPageResult.Failure(result.error)
            }
        }

    companion object {
        const val PAGE_SIZE = 20
    }
}

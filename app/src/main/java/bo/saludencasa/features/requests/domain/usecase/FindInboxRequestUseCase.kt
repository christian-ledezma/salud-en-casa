package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.InboxPageResult
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.InboxSearchResult

// docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 8.
class FindInboxRequestUseCase(
    private val getInbox: GetInboxUseCase,
) {
    suspend operator fun invoke(
        requestId: String,
        loaded: List<InboxRequest>,
        endReached: Boolean,
    ): InboxSearchResult {
        var requests = loaded
        var end = endReached

        while (requests.none { it.id == requestId }) {
            if (end || requests.size >= SEARCH_LIMIT) return InboxSearchResult.NotInInbox(requests, end)

            when (val next = getInbox(offset = requests.size)) {
                is InboxPageResult.Loaded -> {
                    requests = (requests + next.requests).distinctBy(InboxRequest::id)
                    end = next.endReached
                }

                is InboxPageResult.Failure -> {
                    return InboxSearchResult.Failure(next.error, requests, end)
                }
            }
        }
        return InboxSearchResult.Found(requests, end)
    }

    companion object {
        // RNF-08: the search reads a bounded number of pages.
        const val SEARCH_LIMIT = 5 * GetInboxUseCase.PAGE_SIZE
    }
}

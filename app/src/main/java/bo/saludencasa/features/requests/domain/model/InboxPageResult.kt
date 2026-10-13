package bo.saludencasa.features.requests.domain.model

// What the repository returns for one page of the inbox.
sealed interface InboxResult {
    data class Loaded(
        val requests: List<InboxRequest>,
    ) : InboxResult

    data class Failure(
        val error: RequestError,
    ) : InboxResult
}

// What the use case returns: the page plus whether the list has ended, derived
// from a short page the same way the nearby search and MyRequests decide it.
sealed interface InboxPageResult {
    data class Loaded(
        val requests: List<InboxRequest>,
        val endReached: Boolean,
    ) : InboxPageResult

    data class Failure(
        val error: RequestError,
    ) : InboxPageResult
}

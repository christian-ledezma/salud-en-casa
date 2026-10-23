package bo.saludencasa.features.requests.domain.model

sealed interface InboxSearchResult {
    val requests: List<InboxRequest>
    val endReached: Boolean

    data class Found(
        override val requests: List<InboxRequest>,
        override val endReached: Boolean,
    ) : InboxSearchResult

    data class NotInInbox(
        override val requests: List<InboxRequest>,
        override val endReached: Boolean,
    ) : InboxSearchResult

    data class Failure(
        val error: RequestError,
        override val requests: List<InboxRequest>,
        override val endReached: Boolean,
    ) : InboxSearchResult
}

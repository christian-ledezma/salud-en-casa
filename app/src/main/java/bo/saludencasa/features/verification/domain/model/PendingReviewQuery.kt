package bo.saludencasa.features.verification.domain.model

enum class ReviewRoleFilter {
    ALL,
    PATIENT,
    PROFESSIONAL,
}

enum class ReviewQueueOrder {
    OLDEST_FIRST,
    NEWEST_FIRST,
}

data class PendingReviewQuery(
    val search: String = "",
    val role: ReviewRoleFilter = ReviewRoleFilter.ALL,
    val order: ReviewQueueOrder = ReviewQueueOrder.OLDEST_FIRST,
) {
    val term: String? get() = search.trim().takeIf { it.isNotEmpty() }

    val isNarrowed: Boolean get() = term != null || role != ReviewRoleFilter.ALL
}

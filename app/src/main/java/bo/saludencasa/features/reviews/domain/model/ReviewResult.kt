package bo.saludencasa.features.reviews.domain.model

sealed interface ProfessionalReviewsResult {
    data class Found(
        val reviews: List<ProfessionalReview>,
    ) : ProfessionalReviewsResult

    data class Failure(
        val error: ReviewError,
    ) : ProfessionalReviewsResult
}

sealed interface ProfessionalReviewsPageResult {
    data class Loaded(
        val reviews: List<ProfessionalReview>,
        val endReached: Boolean,
    ) : ProfessionalReviewsPageResult

    data class Failure(
        val error: ReviewError,
    ) : ProfessionalReviewsPageResult
}

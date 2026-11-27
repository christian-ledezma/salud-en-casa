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

sealed interface SubmitReviewError {
    // No stars chosen: the screen keeps its button off, so this is the engine of
    // the rule and not a message anyone reads.
    data object RatingMissing : SubmitReviewError

    data object CommentTooLong : SubmitReviewError

    data class Review(
        val error: ReviewError,
    ) : SubmitReviewError
}

sealed interface SubmitReviewResult {
    data object Submitted : SubmitReviewResult

    data class Failure(
        val error: SubmitReviewError,
    ) : SubmitReviewResult
}

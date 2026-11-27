package bo.saludencasa.features.reviews.domain.usecase

import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewResult
import bo.saludencasa.features.reviews.domain.model.toReviewError
import bo.saludencasa.features.reviews.domain.repository.IReviewRepository
import bo.saludencasa.features.reviews.domain.vo.Rating
import bo.saludencasa.features.reviews.domain.vo.ReviewComment
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase

class SubmitReviewUseCase(
    private val repository: IReviewRepository,
    private val getAttention: GetAttentionUseCase,
) {
    suspend operator fun invoke(
        requestId: String,
        rawRating: Int,
        rawComment: String,
    ): SubmitReviewResult {
        val rating =
            Rating.create(rawRating).getOrElse {
                return failure(SubmitReviewError.RatingMissing)
            }
        // A blank comment is a rating with no words, not an error.
        val comment =
            if (rawComment.isBlank()) {
                null
            } else {
                ReviewComment.create(rawComment).getOrElse {
                    return failure(SubmitReviewError.CommentTooLong)
                }
            }

        // The service and the recipient come from the attention as the engine reads
        // it for this caller, never from the screen (docs/decisions.md, 2026-11-26, HU-29).
        val attention =
            when (val result = getAttention(requestId)) {
                is AttentionResult.Loaded -> result.attention
                AttentionResult.NotFound -> return review(ReviewError.NotAllowed)
                is AttentionResult.Failure -> return review(result.error.toReviewError())
            }
        if (attention.myReview != null) return review(ReviewError.AlreadyRated)
        if (!attention.canRate) return review(ReviewError.NotAllowed)

        return repository.submit(attention.serviceId, attention.counterpart.id, rating, comment)
    }

    private fun failure(error: SubmitReviewError) = SubmitReviewResult.Failure(error)

    private fun review(error: ReviewError) = failure(SubmitReviewError.Review(error))
}

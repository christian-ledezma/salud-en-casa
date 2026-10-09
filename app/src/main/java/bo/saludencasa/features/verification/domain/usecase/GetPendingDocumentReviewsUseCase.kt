package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.domain.model.PendingReviewPageResult
import bo.saludencasa.features.verification.domain.model.PendingReviewsResult
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository

class GetPendingDocumentReviewsUseCase(
    private val repository: IDocumentReviewRepository,
) {
    suspend operator fun invoke(offset: Int): PendingReviewPageResult =
        when (val result = repository.findPendingReviews(offset, PAGE_SIZE)) {
            is PendingReviewsResult.Loaded -> {
                PendingReviewPageResult.Loaded(result.reviews, endReached = result.reviews.size < PAGE_SIZE)
            }

            is PendingReviewsResult.Failure -> {
                PendingReviewPageResult.Failure(result.error)
            }
        }

    companion object {
        const val PAGE_SIZE: Int = 20
    }
}

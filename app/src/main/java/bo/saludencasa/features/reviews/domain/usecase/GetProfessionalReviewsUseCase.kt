package bo.saludencasa.features.reviews.domain.usecase

import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsPageResult
import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsResult
import bo.saludencasa.features.reviews.domain.repository.IReviewRepository

class GetProfessionalReviewsUseCase(
    private val repository: IReviewRepository,
) {
    suspend operator fun invoke(
        professionalId: String,
        offset: Int,
    ): ProfessionalReviewsPageResult =
        when (
            val result =
                repository.findReceivedAsProfessional(professionalId, offset = offset, limit = PAGE_SIZE)
        ) {
            is ProfessionalReviewsResult.Found -> {
                ProfessionalReviewsPageResult.Loaded(
                    reviews = result.reviews,
                    // A short page is the only signal that the list ended, which
                    // is why no row is ever dropped on the way here: one that
                    // cannot be read fails its whole page instead.
                    endReached = result.reviews.size < PAGE_SIZE,
                )
            }

            is ProfessionalReviewsResult.Failure -> {
                ProfessionalReviewsPageResult.Failure(result.error)
            }
        }

    companion object {
        const val PAGE_SIZE: Int = 10
    }
}

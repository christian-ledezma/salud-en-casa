package bo.saludencasa.features.reviews.data.repository

import bo.saludencasa.features.reviews.data.datasource.SupabaseReviewDataSource
import bo.saludencasa.features.reviews.data.mapper.toProfessionalReview
import bo.saludencasa.features.reviews.data.mapper.toReviewError
import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsResult
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.repository.IReviewRepository
import kotlinx.coroutines.CancellationException

class ReviewRepository(
    private val dataSource: SupabaseReviewDataSource,
) : IReviewRepository {
    override suspend fun findReceivedAsProfessional(
        professionalId: String,
        offset: Int,
        limit: Int,
    ): ProfessionalReviewsResult {
        if (dataSource.currentUserId() == null) {
            return ProfessionalReviewsResult.Failure(ReviewError.NotSignedIn)
        }

        return try {
            val rows = dataSource.findReviewsOf(professionalId, offset, limit)
            val reviews = rows.map { it.toProfessionalReview() }

            if (reviews.any { it == null }) {
                ProfessionalReviewsResult.Failure(ReviewError.UnreadableResult)
            } else {
                ProfessionalReviewsResult.Found(reviews.filterNotNull())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ProfessionalReviewsResult.Failure(failure.toReviewError())
        }
    }
}

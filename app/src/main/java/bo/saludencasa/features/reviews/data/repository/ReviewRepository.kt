package bo.saludencasa.features.reviews.data.repository

import bo.saludencasa.features.reviews.data.datasource.SupabaseReviewDataSource
import bo.saludencasa.features.reviews.data.mapper.toProfessionalReview
import bo.saludencasa.features.reviews.data.mapper.toReviewError
import bo.saludencasa.features.reviews.data.model.NewReviewRow
import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsResult
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewResult
import bo.saludencasa.features.reviews.domain.repository.IReviewRepository
import bo.saludencasa.features.reviews.domain.vo.Rating
import bo.saludencasa.features.reviews.domain.vo.ReviewComment
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

    override suspend fun submit(
        serviceId: String,
        recipientId: String,
        rating: Rating,
        comment: ReviewComment?,
    ): SubmitReviewResult {
        val authorId =
            dataSource.currentUserId()
                ?: return SubmitReviewResult.Failure(SubmitReviewError.Review(ReviewError.NotSignedIn))

        return try {
            dataSource.insertReview(
                NewReviewRow(
                    serviceId = serviceId,
                    authorId = authorId,
                    recipientId = recipientId,
                    rating = rating.value,
                    comment = comment?.value,
                ),
            )
            SubmitReviewResult.Submitted
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            SubmitReviewResult.Failure(SubmitReviewError.Review(failure.toReviewError()))
        }
    }
}

package bo.saludencasa.features.reviews

import bo.saludencasa.features.reviews.domain.model.ProfessionalReview
import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsResult
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewResult
import bo.saludencasa.features.reviews.domain.repository.IReviewRepository
import bo.saludencasa.features.reviews.domain.vo.Rating
import bo.saludencasa.features.reviews.domain.vo.ReviewComment
import kotlinx.coroutines.CompletableDeferred
import java.time.Instant

class FakeReviewRepository(
    var reviews: List<ProfessionalReview> = emptyList(),
) : IReviewRepository {
    data class Request(
        val professionalId: String,
        val offset: Int,
        val limit: Int,
    )

    var failure: ReviewError? = null

    // Holds the answer so a test can have a second call land while the first
    // is still in flight.
    var gate: CompletableDeferred<Unit>? = null

    val requests: MutableList<Request> = mutableListOf()

    data class Submission(
        val serviceId: String,
        val recipientId: String,
        val rating: Int,
        val comment: String?,
    )

    var submitFailure: ReviewError? = null

    // Holds the submission so a test can tap again while the first is in flight.
    var submitGate: CompletableDeferred<Unit>? = null

    val submissions: MutableList<Submission> = mutableListOf()

    override suspend fun findReceivedAsProfessional(
        professionalId: String,
        offset: Int,
        limit: Int,
    ): ProfessionalReviewsResult {
        requests += Request(professionalId, offset, limit)
        gate?.await()
        failure?.let { return ProfessionalReviewsResult.Failure(it) }

        return ProfessionalReviewsResult.Found(reviews.drop(offset).take(limit))
    }

    override suspend fun submit(
        serviceId: String,
        recipientId: String,
        rating: Rating,
        comment: ReviewComment?,
    ): SubmitReviewResult {
        submissions += Submission(serviceId, recipientId, rating.value, comment?.value)
        submitGate?.await()
        return submitFailure?.let { SubmitReviewResult.Failure(SubmitReviewError.Review(it)) }
            ?: SubmitReviewResult.Submitted
    }
}

fun professionalReview(
    id: String = "review-1",
    rating: Int = 5,
    comment: String? = "Muy puntual y explicó todo con paciencia.",
    createdAt: Instant = Instant.parse("2026-09-01T12:00:00Z"),
): ProfessionalReview = ProfessionalReview(id = id, rating = rating, comment = comment, createdAt = createdAt)

fun professionalReviews(count: Int): List<ProfessionalReview> = (1..count).map { professionalReview(id = "review-$it") }

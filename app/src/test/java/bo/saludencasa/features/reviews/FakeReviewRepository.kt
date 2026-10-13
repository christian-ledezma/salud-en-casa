package bo.saludencasa.features.reviews

import bo.saludencasa.features.reviews.domain.model.ProfessionalReview
import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsResult
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.repository.IReviewRepository
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
}

fun professionalReview(
    id: String = "review-1",
    rating: Int = 5,
    comment: String? = "Muy puntual y explicó todo con paciencia.",
    createdAt: Instant = Instant.parse("2026-09-01T12:00:00Z"),
): ProfessionalReview = ProfessionalReview(id = id, rating = rating, comment = comment, createdAt = createdAt)

fun professionalReviews(count: Int): List<ProfessionalReview> = (1..count).map { professionalReview(id = "review-$it") }

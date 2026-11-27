package bo.saludencasa.features.reviews.domain.repository

import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsResult
import bo.saludencasa.features.reviews.domain.model.SubmitReviewResult
import bo.saludencasa.features.reviews.domain.vo.Rating
import bo.saludencasa.features.reviews.domain.vo.ReviewComment

interface IReviewRepository {
    suspend fun findReceivedAsProfessional(
        professionalId: String,
        offset: Int,
        limit: Int,
    ): ProfessionalReviewsResult

    // The author is the signed in person; the engine seals the date and keeps the
    // rating visible (docs/decisions.md, 2026-11-26, HU-29).
    suspend fun submit(
        serviceId: String,
        recipientId: String,
        rating: Rating,
        comment: ReviewComment?,
    ): SubmitReviewResult
}

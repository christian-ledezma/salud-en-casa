package bo.saludencasa.features.reviews.domain.repository

import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsResult

interface IReviewRepository {
    suspend fun findReceivedAsProfessional(
        professionalId: String,
        offset: Int,
        limit: Int,
    ): ProfessionalReviewsResult
}

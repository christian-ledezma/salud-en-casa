package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository

class ApproveProfessionalVerificationUseCase(
    private val repository: IDocumentReviewRepository,
) {
    suspend operator fun invoke(profileId: String): ReviewActionResult = repository.approveProfessional(profileId)
}

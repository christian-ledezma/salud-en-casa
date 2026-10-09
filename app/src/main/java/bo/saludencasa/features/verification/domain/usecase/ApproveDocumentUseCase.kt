package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository

class ApproveDocumentUseCase(
    private val repository: IDocumentReviewRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        type: DocumentType,
    ): ReviewActionResult = repository.approveDocument(profileId, type)
}

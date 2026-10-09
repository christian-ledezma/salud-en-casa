package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository

class ApproveDocumentsUseCase(
    private val repository: IDocumentReviewRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        types: Set<DocumentType>,
    ): ReviewActionResult = repository.approveDocuments(profileId, types)
}

package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository

class GetSignedDocumentUrlUseCase(
    private val repository: IDocumentReviewRepository,
) {
    suspend operator fun invoke(storagePath: String): DocumentUrlResult = repository.getDocumentUrl(storagePath)
}

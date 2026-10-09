package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository
import bo.saludencasa.features.verification.domain.vo.RejectionReason

class RejectDocumentUseCase(
    private val repository: IDocumentReviewRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        type: DocumentType,
        rawReason: String,
    ): ReviewActionResult {
        val reason =
            RejectionReason.create(rawReason).getOrElse {
                return ReviewActionResult.Failure(VerificationError.InvalidRejectionReason)
            }
        return repository.rejectDocument(profileId, type, reason)
    }
}

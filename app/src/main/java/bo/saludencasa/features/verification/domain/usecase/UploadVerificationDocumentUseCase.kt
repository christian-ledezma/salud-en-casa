package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.UploadDocumentResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.repository.IVerificationRepository
import bo.saludencasa.features.verification.domain.vo.DocumentCaption
import bo.saludencasa.features.verification.domain.vo.DocumentImage

class UploadVerificationDocumentUseCase(
    private val repository: IVerificationRepository,
) {
    suspend operator fun invoke(
        type: DocumentType,
        bytes: ByteArray,
        rawCaption: String?,
    ): UploadDocumentResult {
        if (bytes.isEmpty()) return UploadDocumentResult.Failure(VerificationError.EmptyImage)
        val image =
            DocumentImage.create(bytes).getOrNull()
                ?: return UploadDocumentResult.Failure(VerificationError.ImageTooLarge)

        val caption =
            if (type == DocumentType.OTHER) {
                // The schema constraint verification_documents_caption_matches_type
                // mirrors this: the application names the field instead of
                // letting the SQL check reach the UI as a generic refusal.
                if (rawCaption.isNullOrBlank()) {
                    return UploadDocumentResult.Failure(VerificationError.CaptionRequired)
                }
                DocumentCaption.create(rawCaption).getOrNull()
                    ?: return UploadDocumentResult.Failure(VerificationError.InvalidCaption)
            } else {
                if (!rawCaption.isNullOrBlank()) {
                    return UploadDocumentResult.Failure(VerificationError.CaptionNotAllowed)
                }
                null
            }

        return repository.uploadDocument(type, image, caption)
    }
}

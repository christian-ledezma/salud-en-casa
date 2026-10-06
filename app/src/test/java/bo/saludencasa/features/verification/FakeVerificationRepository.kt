package bo.saludencasa.features.verification

import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.UploadDocumentResult
import bo.saludencasa.features.verification.domain.model.VerificationDocument
import bo.saludencasa.features.verification.domain.repository.IVerificationRepository
import bo.saludencasa.features.verification.domain.vo.DocumentCaption
import bo.saludencasa.features.verification.domain.vo.DocumentImage

class FakeVerificationRepository(
    var documentsResult: MyDocumentsResult = MyDocumentsResult.Loaded(emptyList()),
    var uploadResult: UploadDocumentResult = UploadDocumentResult.Success,
) : IVerificationRepository {
    data class UploadAttempt(
        val type: DocumentType,
        val caption: String?,
    )

    var documentReads: Int = 0
        private set
    val uploadAttempts: MutableList<UploadAttempt> = mutableListOf()

    override suspend fun getMyDocuments(): MyDocumentsResult {
        documentReads++
        return documentsResult
    }

    override suspend fun uploadDocument(
        type: DocumentType,
        image: DocumentImage,
        caption: DocumentCaption?,
    ): UploadDocumentResult {
        uploadAttempts += UploadAttempt(type = type, caption = caption?.value)
        return uploadResult
    }
}

fun verificationDocument(
    type: DocumentType = DocumentType.ID_FRONT,
    status: ReviewStatus = ReviewStatus.PENDING,
    storagePath: String = "x/${type.name}.jpg",
    caption: String? = null,
    rejectionReason: String? = null,
): VerificationDocument =
    VerificationDocument(
        type = type,
        status = status,
        storagePath = storagePath,
        caption = caption,
        rejectionReason = rejectionReason,
    )

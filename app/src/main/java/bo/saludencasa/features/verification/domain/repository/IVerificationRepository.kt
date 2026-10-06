package bo.saludencasa.features.verification.domain.repository

import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.UploadDocumentResult
import bo.saludencasa.features.verification.domain.vo.DocumentCaption
import bo.saludencasa.features.verification.domain.vo.DocumentImage

interface IVerificationRepository {
    suspend fun getMyDocuments(): MyDocumentsResult

    suspend fun uploadDocument(
        type: DocumentType,
        image: DocumentImage,
        caption: DocumentCaption?,
    ): UploadDocumentResult
}

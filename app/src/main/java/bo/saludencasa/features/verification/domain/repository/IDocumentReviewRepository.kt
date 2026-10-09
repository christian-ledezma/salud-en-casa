package bo.saludencasa.features.verification.domain.repository

import bo.saludencasa.features.verification.domain.model.DocumentReviewSubjectResult
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.PendingReviewsResult
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.vo.RejectionReason

interface IDocumentReviewRepository {
    suspend fun findPendingReviews(
        offset: Int,
        limit: Int,
    ): PendingReviewsResult

    suspend fun getSubject(profileId: String): DocumentReviewSubjectResult

    suspend fun getDocumentsOf(profileId: String): MyDocumentsResult

    suspend fun approveDocument(
        profileId: String,
        type: DocumentType,
    ): ReviewActionResult

    suspend fun rejectDocument(
        profileId: String,
        type: DocumentType,
        reason: RejectionReason,
    ): ReviewActionResult

    suspend fun approveProfessional(profileId: String): ReviewActionResult

    suspend fun getDocumentUrl(storagePath: String): DocumentUrlResult
}

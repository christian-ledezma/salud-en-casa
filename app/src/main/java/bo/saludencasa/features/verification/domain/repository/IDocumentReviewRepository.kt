package bo.saludencasa.features.verification.domain.repository

import bo.saludencasa.features.verification.domain.model.DocumentReviewSubjectResult
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.PendingReviewQuery
import bo.saludencasa.features.verification.domain.model.PendingReviewSubjectsResult
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.vo.RejectionReason

interface IDocumentReviewRepository {
    suspend fun findPendingSubjects(
        query: PendingReviewQuery,
        offset: Int,
        limit: Int,
    ): PendingReviewSubjectsResult

    suspend fun getSubject(profileId: String): DocumentReviewSubjectResult

    suspend fun getDocumentsOf(profileId: String): MyDocumentsResult

    suspend fun approveDocuments(
        profileId: String,
        types: Set<DocumentType>,
    ): ReviewActionResult

    suspend fun rejectDocuments(
        profileId: String,
        types: Set<DocumentType>,
        reason: RejectionReason,
    ): ReviewActionResult

    suspend fun approveProfessional(profileId: String): ReviewActionResult

    suspend fun getDocumentUrl(storagePath: String): DocumentUrlResult
}

package bo.saludencasa.features.verification.data.repository

import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.data.datasource.SupabaseVerificationDataSource
import bo.saludencasa.features.verification.data.mapper.toDocument
import bo.saludencasa.features.verification.data.mapper.toPendingSubject
import bo.saludencasa.features.verification.data.mapper.toSubject
import bo.saludencasa.features.verification.data.mapper.toVerificationError
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubjectResult
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.PendingReviewQuery
import bo.saludencasa.features.verification.domain.model.PendingReviewSubjectsResult
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.model.ReviewQueueOrder
import bo.saludencasa.features.verification.domain.model.ReviewRoleFilter
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository
import bo.saludencasa.features.verification.domain.vo.RejectionReason
import kotlinx.coroutines.CancellationException

class DocumentReviewRepository(
    private val dataSource: SupabaseVerificationDataSource,
) : IDocumentReviewRepository {
    override suspend fun findPendingSubjects(
        query: PendingReviewQuery,
        offset: Int,
        limit: Int,
    ): PendingReviewSubjectsResult =
        guarded(PendingReviewSubjectsResult::Failure) {
            val rows =
                dataSource.findPendingSubjects(
                    term = query.term,
                    role = query.role.heldRole(),
                    newestFirst = query.order == ReviewQueueOrder.NEWEST_FIRST,
                    offset = offset.toLong(),
                    limit = limit.toLong(),
                )
            val subjects =
                rows.map { dto ->
                    dto.toPendingSubject()
                        ?: return@guarded PendingReviewSubjectsResult.Failure(VerificationError.Unexpected)
                }
            PendingReviewSubjectsResult.Loaded(subjects)
        }

    override suspend fun getSubject(profileId: String): DocumentReviewSubjectResult =
        guarded(DocumentReviewSubjectResult::Failure) {
            val subject = dataSource.findReviewSubject(profileId)?.toSubject()
            if (subject == null) {
                DocumentReviewSubjectResult.Failure(VerificationError.Unexpected)
            } else {
                DocumentReviewSubjectResult.Loaded(subject)
            }
        }

    override suspend fun getDocumentsOf(profileId: String): MyDocumentsResult =
        guarded(MyDocumentsResult::Failure) {
            MyDocumentsResult.Loaded(dataSource.findDocumentsOf(profileId).mapNotNull { it.toDocument() })
        }

    override suspend fun approveDocuments(
        profileId: String,
        types: Set<DocumentType>,
    ): ReviewActionResult = writeVerdict(profileId, types, ReviewStatus.APPROVED, rejectionReason = null)

    override suspend fun rejectDocuments(
        profileId: String,
        types: Set<DocumentType>,
        reason: RejectionReason,
    ): ReviewActionResult = writeVerdict(profileId, types, ReviewStatus.REJECTED, rejectionReason = reason.value)

    override suspend fun approveProfessional(profileId: String): ReviewActionResult =
        guarded(ReviewActionResult::Failure) {
            dataSource.approveProfessional(profileId)
            ReviewActionResult.Success
        }

    override suspend fun getDocumentUrl(storagePath: String): DocumentUrlResult =
        guarded(DocumentUrlResult::Failure) {
            DocumentUrlResult.Loaded(dataSource.createSignedUrl(storagePath))
        }

    // One statement for the whole selection, so the engine either stamps every
    // document or none. A policy refusal updates zero rows without raising, and a
    // short count means the verdict only landed on part of what was asked.
    private suspend fun writeVerdict(
        profileId: String,
        types: Set<DocumentType>,
        status: ReviewStatus,
        rejectionReason: String?,
    ): ReviewActionResult =
        guarded(ReviewActionResult::Failure) {
            val written = dataSource.writeVerdict(profileId, types.map { it.name }, status.name, rejectionReason)
            if (written == types.size) {
                ReviewActionResult.Success
            } else {
                ReviewActionResult.Failure(VerificationError.NotAuthorized)
            }
        }

    private fun ReviewRoleFilter.heldRole(): String? =
        when (this) {
            ReviewRoleFilter.ALL -> null
            ReviewRoleFilter.PATIENT -> UserRole.PATIENT.name
            ReviewRoleFilter.PROFESSIONAL -> UserRole.PROFESSIONAL.name
        }

    private suspend fun <T> guarded(
        failure: (VerificationError) -> T,
        block: suspend () -> T,
    ): T =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            failure(error.toVerificationError())
        }
}

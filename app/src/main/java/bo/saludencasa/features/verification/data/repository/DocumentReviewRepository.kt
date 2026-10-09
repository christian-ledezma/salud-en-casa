package bo.saludencasa.features.verification.data.repository

import bo.saludencasa.features.verification.data.datasource.SupabaseVerificationDataSource
import bo.saludencasa.features.verification.data.mapper.toDocument
import bo.saludencasa.features.verification.data.mapper.toPendingReview
import bo.saludencasa.features.verification.data.mapper.toSubject
import bo.saludencasa.features.verification.data.mapper.toVerificationError
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubjectResult
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.PendingReviewsResult
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository
import bo.saludencasa.features.verification.domain.vo.RejectionReason
import kotlinx.coroutines.CancellationException

class DocumentReviewRepository(
    private val dataSource: SupabaseVerificationDataSource,
) : IDocumentReviewRepository {
    override suspend fun findPendingReviews(
        offset: Int,
        limit: Int,
    ): PendingReviewsResult =
        guarded(PendingReviewsResult::Failure) {
            val reviews =
                dataSource.findPendingReviews(offset.toLong(), limit.toLong()).map { dto ->
                    dto.toPendingReview() ?: return@guarded PendingReviewsResult.Failure(VerificationError.Unexpected)
                }
            PendingReviewsResult.Loaded(reviews)
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

    override suspend fun approveDocument(
        profileId: String,
        type: DocumentType,
    ): ReviewActionResult = writeVerdict(profileId, type, ReviewStatus.APPROVED, rejectionReason = null)

    override suspend fun rejectDocument(
        profileId: String,
        type: DocumentType,
        reason: RejectionReason,
    ): ReviewActionResult = writeVerdict(profileId, type, ReviewStatus.REJECTED, rejectionReason = reason.value)

    override suspend fun approveProfessional(profileId: String): ReviewActionResult =
        guarded(ReviewActionResult::Failure) {
            dataSource.approveProfessional(profileId)
            ReviewActionResult.Success
        }

    override suspend fun getDocumentUrl(storagePath: String): DocumentUrlResult =
        guarded(DocumentUrlResult::Failure) {
            DocumentUrlResult.Loaded(dataSource.createSignedUrl(storagePath))
        }

    private suspend fun writeVerdict(
        profileId: String,
        type: DocumentType,
        status: ReviewStatus,
        rejectionReason: String?,
    ): ReviewActionResult =
        guarded(ReviewActionResult::Failure) {
            val written = dataSource.writeVerdict(profileId, type.name, status.name, rejectionReason)
            if (written) ReviewActionResult.Success else ReviewActionResult.Failure(VerificationError.NotAuthorized)
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

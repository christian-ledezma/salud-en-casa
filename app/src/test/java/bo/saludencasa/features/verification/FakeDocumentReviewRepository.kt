package bo.saludencasa.features.verification

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubject
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubjectResult
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.PendingDocumentReview
import bo.saludencasa.features.verification.domain.model.PendingReviewsResult
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationDocument
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository
import bo.saludencasa.features.verification.domain.vo.RejectionReason
import java.time.Instant

class FakeDocumentReviewRepository(
    var subjectResult: DocumentReviewSubjectResult = DocumentReviewSubjectResult.Loaded(reviewSubject()),
    var documents: List<VerificationDocument> = emptyList(),
    var urlResult: DocumentUrlResult = DocumentUrlResult.Loaded("https://signed.example/document"),
    var actionResult: ReviewActionResult = ReviewActionResult.Success,
    // When false the write is accepted but the stored row does not change, which
    // is how a test shows that the screen reads the server back instead of
    // assuming the outcome.
    var applyWrites: Boolean = true,
    var documentsFailure: VerificationError? = null,
) : IDocumentReviewRepository {
    data class Verdict(
        val type: DocumentType,
        val status: ReviewStatus,
        val reason: String?,
    )

    var pending: suspend (offset: Int, limit: Int) -> PendingReviewsResult = { _, _ ->
        PendingReviewsResult.Loaded(
            emptyList(),
        )
    }
    val pendingRequests: MutableList<Pair<Int, Int>> = mutableListOf()
    val verdicts: MutableList<Verdict> = mutableListOf()
    val urlRequests: MutableList<String> = mutableListOf()

    override suspend fun findPendingReviews(
        offset: Int,
        limit: Int,
    ): PendingReviewsResult {
        pendingRequests += offset to limit
        return pending(offset, limit)
    }

    override suspend fun getSubject(profileId: String): DocumentReviewSubjectResult = subjectResult

    override suspend fun getDocumentsOf(profileId: String): MyDocumentsResult =
        documentsFailure?.let(MyDocumentsResult::Failure) ?: MyDocumentsResult.Loaded(documents)

    override suspend fun approveDocument(
        profileId: String,
        type: DocumentType,
    ): ReviewActionResult {
        verdicts += Verdict(type, ReviewStatus.APPROVED, null)
        if (actionResult is ReviewActionResult.Success && applyWrites) {
            documents =
                documents.map {
                    if (it.type ==
                        type
                    ) {
                        it.copy(status = ReviewStatus.APPROVED, rejectionReason = null)
                    } else {
                        it
                    }
                }
        }
        return actionResult
    }

    override suspend fun rejectDocument(
        profileId: String,
        type: DocumentType,
        reason: RejectionReason,
    ): ReviewActionResult {
        verdicts += Verdict(type, ReviewStatus.REJECTED, reason.value)
        if (actionResult is ReviewActionResult.Success && applyWrites) {
            documents =
                documents.map {
                    if (it.type == type) it.copy(status = ReviewStatus.REJECTED, rejectionReason = reason.value) else it
                }
        }
        return actionResult
    }

    override suspend fun approveProfessional(profileId: String): ReviewActionResult {
        val loaded = subjectResult as? DocumentReviewSubjectResult.Loaded
        if (actionResult is ReviewActionResult.Success && applyWrites && loaded != null) {
            subjectResult =
                DocumentReviewSubjectResult.Loaded(loaded.subject.copy(professionalStatus = ReviewStatus.APPROVED))
        }
        return actionResult
    }

    override suspend fun getDocumentUrl(storagePath: String): DocumentUrlResult {
        urlRequests += storagePath
        return urlResult
    }
}

fun reviewSubject(
    heldRoles: Set<UserRole> = setOf(UserRole.PROFESSIONAL),
    professionalType: ProfessionalType? = ProfessionalType.NURSE,
    professionalStatus: ReviewStatus? = ReviewStatus.PENDING,
): DocumentReviewSubject =
    DocumentReviewSubject(
        profileId = "subject-id",
        fullName = "Ernesto Arancibia",
        email = "ernesto@example.com",
        heldRoles = heldRoles,
        professionalType = professionalType,
        professionalStatus = professionalStatus,
    )

fun pendingReview(
    index: Int = 0,
    type: DocumentType = DocumentType.ID_FRONT,
    fullName: String = "Person $index",
): PendingDocumentReview =
    PendingDocumentReview(
        profileId = "profile-$index",
        type = type,
        caption = null,
        createdAt = Instant.parse("2026-10-01T15:30:00Z"),
        fullName = fullName,
        email = "person$index@example.com",
    )

val everyRequiredNurseDocument: List<DocumentType> =
    listOf(
        DocumentType.ID_FRONT,
        DocumentType.ID_BACK,
        DocumentType.SELFIE,
        DocumentType.DEGREE,
        DocumentType.LICENSE,
    )

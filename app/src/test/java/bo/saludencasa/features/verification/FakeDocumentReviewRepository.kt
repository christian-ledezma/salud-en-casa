package bo.saludencasa.features.verification

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubject
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubjectResult
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.PendingReviewQuery
import bo.saludencasa.features.verification.domain.model.PendingReviewSubject
import bo.saludencasa.features.verification.domain.model.PendingReviewSubjectsResult
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
        val types: Set<DocumentType>,
        val status: ReviewStatus,
        val reason: String?,
    )

    data class PageRequest(
        val query: PendingReviewQuery,
        val offset: Int,
        val limit: Int,
    )

    var pending: suspend (query: PendingReviewQuery, offset: Int) -> PendingReviewSubjectsResult = { _, _ ->
        PendingReviewSubjectsResult.Loaded(emptyList())
    }
    val pendingRequests: MutableList<PageRequest> = mutableListOf()
    val verdicts: MutableList<Verdict> = mutableListOf()
    val urlRequests: MutableList<String> = mutableListOf()

    override suspend fun findPendingSubjects(
        query: PendingReviewQuery,
        offset: Int,
        limit: Int,
    ): PendingReviewSubjectsResult {
        pendingRequests += PageRequest(query, offset, limit)
        return pending(query, offset)
    }

    override suspend fun getSubject(profileId: String): DocumentReviewSubjectResult = subjectResult

    override suspend fun getDocumentsOf(profileId: String): MyDocumentsResult =
        documentsFailure?.let(MyDocumentsResult::Failure) ?: MyDocumentsResult.Loaded(documents)

    override suspend fun approveDocuments(
        profileId: String,
        types: Set<DocumentType>,
    ): ReviewActionResult = write(types, ReviewStatus.APPROVED, reason = null)

    override suspend fun rejectDocuments(
        profileId: String,
        types: Set<DocumentType>,
        reason: RejectionReason,
    ): ReviewActionResult = write(types, ReviewStatus.REJECTED, reason.value)

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

    private fun write(
        types: Set<DocumentType>,
        status: ReviewStatus,
        reason: String?,
    ): ReviewActionResult {
        verdicts += Verdict(types, status, reason)
        if (actionResult is ReviewActionResult.Success && applyWrites) {
            documents =
                documents.map {
                    if (it.type in types) it.copy(status = status, rejectionReason = reason) else it
                }
        }
        return actionResult
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

fun pendingSubject(
    index: Int = 0,
    fullName: String = "Person $index",
    pendingCount: Int = 3,
    awaitingVerification: Boolean = false,
): PendingReviewSubject =
    PendingReviewSubject(
        profileId = "profile-$index",
        fullName = fullName,
        email = "person$index@example.com",
        pendingCount = pendingCount,
        awaitingVerification = awaitingVerification,
        waitingSince = Instant.parse("2026-10-01T15:30:00Z"),
    )

val everyRequiredNurseDocument: List<DocumentType> =
    listOf(
        DocumentType.ID_FRONT,
        DocumentType.ID_BACK,
        DocumentType.SELFIE,
        DocumentType.DEGREE,
        DocumentType.LICENSE,
    )

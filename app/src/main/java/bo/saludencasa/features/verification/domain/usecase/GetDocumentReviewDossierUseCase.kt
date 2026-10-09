package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.domain.model.DocumentReviewDossier
import bo.saludencasa.features.verification.domain.model.DocumentReviewDossierResult
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubjectResult
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.VerificationChecklist
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository

class GetDocumentReviewDossierUseCase(
    private val repository: IDocumentReviewRepository,
) {
    suspend operator fun invoke(profileId: String): DocumentReviewDossierResult {
        val subject =
            when (val result = repository.getSubject(profileId)) {
                is DocumentReviewSubjectResult.Loaded -> result.subject
                is DocumentReviewSubjectResult.Failure -> return DocumentReviewDossierResult.Failure(result.error)
            }

        val documents =
            when (val result = repository.getDocumentsOf(profileId)) {
                is MyDocumentsResult.Loaded -> result.documents
                is MyDocumentsResult.Failure -> return DocumentReviewDossierResult.Failure(result.error)
            }

        // docs/decisions.md, 2026-10-01, held roles and not the active one.
        val checklist = VerificationChecklist.create(subject.heldRoles, subject.professionalType, documents)
        return DocumentReviewDossierResult.Loaded(DocumentReviewDossier(subject, checklist))
    }
}

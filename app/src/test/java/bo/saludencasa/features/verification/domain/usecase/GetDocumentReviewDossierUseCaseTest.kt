package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.FakeDocumentReviewRepository
import bo.saludencasa.features.verification.domain.model.DocumentReviewDossierResult
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubjectResult
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.reviewSubject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDocumentReviewDossierUseCaseTest {
    private val repository = FakeDocumentReviewRepository()
    private val getDossier = GetDocumentReviewDossierUseCase(repository)

    // The reviewed person owes the documents of every role they hold, not only
    // of the one they happen to be using; reviewing against the latter would let
    // a dual-role professional be approved without their identity documents.
    @Test
    fun theChecklistComesFromTheHeldRoles() =
        runTest {
            repository.subjectResult =
                DocumentReviewSubjectResult.Loaded(
                    reviewSubject(
                        heldRoles = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL),
                        professionalType = ProfessionalType.NURSE,
                    ),
                )

            val dossier = (getDossier("subject-id") as DocumentReviewDossierResult.Loaded).dossier

            assertEquals(
                listOf(
                    DocumentType.ID_FRONT,
                    DocumentType.ID_BACK,
                    DocumentType.SELFIE,
                    DocumentType.DEGREE,
                    DocumentType.LICENSE,
                ),
                dossier.checklist.required,
            )
        }

    @Test
    fun aFailedSubjectReadFailsTheWholeDossier() =
        runTest {
            repository.subjectResult = DocumentReviewSubjectResult.Failure(VerificationError.NetworkUnavailable)

            val result = getDossier("subject-id")

            assertEquals(DocumentReviewDossierResult.Failure(VerificationError.NetworkUnavailable), result)
        }
}

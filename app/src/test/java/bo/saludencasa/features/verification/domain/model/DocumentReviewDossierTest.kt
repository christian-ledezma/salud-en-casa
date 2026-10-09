package bo.saludencasa.features.verification.domain.model

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.everyRequiredNurseDocument
import bo.saludencasa.features.verification.reviewSubject
import bo.saludencasa.features.verification.verificationDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentReviewDossierTest {
    private fun dossier(
        subject: DocumentReviewSubject = reviewSubject(),
        approved: Collection<DocumentType> = everyRequiredNurseDocument,
        pending: Collection<DocumentType> = emptyList(),
    ): DocumentReviewDossier {
        val documents =
            approved.map { verificationDocument(type = it, status = ReviewStatus.APPROVED) } +
                pending.map { verificationDocument(type = it, status = ReviewStatus.PENDING) }
        return DocumentReviewDossier(
            subject = subject,
            checklist = VerificationChecklist.create(subject.heldRoles, subject.professionalType, documents),
        )
    }

    @Test
    fun aProfessionalIsApprovableOnlyWhenEveryRequiredDocumentIsApproved() {
        assertTrue(dossier().canApproveProfessional)
        assertFalse(
            dossier(
                approved = everyRequiredNurseDocument - DocumentType.LICENSE,
                pending = listOf(DocumentType.LICENSE),
            ).canApproveProfessional,
        )
        assertFalse(dossier(approved = everyRequiredNurseDocument - DocumentType.LICENSE).canApproveProfessional)
    }

    @Test
    fun anOptionalAttachmentStillPendingDoesNotBlockTheApproval() {
        assertTrue(dossier(pending = listOf(DocumentType.OTHER)).canApproveProfessional)
    }

    @Test
    fun aPatientOnlyProfileIsNeverOfferedTheProfessionalApproval() {
        val patient =
            reviewSubject(heldRoles = setOf(UserRole.PATIENT), professionalType = null, professionalStatus = null)

        assertFalse(
            dossier(
                subject = patient,
                approved = listOf(DocumentType.ID_FRONT, DocumentType.ID_BACK, DocumentType.SELFIE),
            ).canApproveProfessional,
        )
    }

    // The engine refuses it anyway; offering a button that cannot work is the
    // failure this prediction exists to avoid.
    @Test
    fun aProfessionalWithNoDeclaredTypeIsNotOfferedTheApproval() {
        val undeclared = reviewSubject(professionalType = null)

        assertFalse(
            dossier(subject = undeclared, approved = everyRequiredNurseDocument + DocumentType.STUDENT_CARD)
                .canApproveProfessional,
        )
    }

    @Test
    fun anAlreadyApprovedProfessionalIsNotOfferedTheButtonAgain() {
        val verified = reviewSubject(professionalStatus = ReviewStatus.APPROVED)

        assertFalse(dossier(subject = verified).canApproveProfessional)
    }

    @Test
    fun aStudentNeedsTheStudentCardAndNotTheLicense() {
        val student = reviewSubject(professionalType = ProfessionalType.STUDENT)
        val withCard =
            listOf(
                DocumentType.ID_FRONT,
                DocumentType.ID_BACK,
                DocumentType.SELFIE,
                DocumentType.DEGREE,
                DocumentType.STUDENT_CARD,
            )

        assertTrue(dossier(subject = student, approved = withCard).canApproveProfessional)
        assertFalse(dossier(subject = student, approved = everyRequiredNurseDocument).canApproveProfessional)
    }

    @Test
    fun theOptionalAttachmentIsReviewableOnlyOnceUploaded() {
        val without = dossier()
        val with = dossier(pending = listOf(DocumentType.OTHER))

        assertFalse(DocumentType.OTHER in without.reviewableTypes)
        assertTrue(DocumentType.OTHER in with.reviewableTypes)
    }

    @Test
    fun everyRequiredDocumentIsReviewableEvenBeforeItIsUploaded() {
        val nothingUploaded = dossier(approved = emptyList())

        assertEquals(nothingUploaded.checklist.required, nothingUploaded.reviewableTypes)
    }
}

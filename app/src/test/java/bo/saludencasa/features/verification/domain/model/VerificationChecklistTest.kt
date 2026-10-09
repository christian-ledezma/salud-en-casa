package bo.saludencasa.features.verification.domain.model

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.verificationDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationChecklistTest {
    @Test
    fun `a patient alone is asked for the three identity documents`() {
        val checklist = VerificationChecklist.create(roles(UserRole.PATIENT), null, emptyList())

        assertEquals(
            listOf(DocumentType.ID_FRONT, DocumentType.ID_BACK, DocumentType.SELFIE),
            checklist.required,
        )
    }

    @Test
    fun `a student professional is asked for the student card instead of a license`() {
        val checklist =
            VerificationChecklist.create(roles(UserRole.PROFESSIONAL), ProfessionalType.STUDENT, emptyList())

        assertTrue(DocumentType.STUDENT_CARD in checklist.required)
        assertTrue(DocumentType.LICENSE !in checklist.required)
    }

    @Test
    fun `a titled professional is asked for the license and not the student card`() {
        val checklist =
            VerificationChecklist.create(roles(UserRole.PROFESSIONAL), ProfessionalType.DOCTOR, emptyList())

        assertTrue(DocumentType.LICENSE in checklist.required)
        assertTrue(DocumentType.STUDENT_CARD !in checklist.required)
    }

    @Test
    fun `a professional without a declared type is asked for both license and student card`() {
        val checklist = VerificationChecklist.create(roles(UserRole.PROFESSIONAL), null, emptyList())

        assertTrue(DocumentType.LICENSE in checklist.required)
        assertTrue(DocumentType.STUDENT_CARD in checklist.required)
    }

    @Test
    fun `a person with both roles sees identity documents once and the professional ones added`() {
        val roles = roles(UserRole.PATIENT, UserRole.PROFESSIONAL)

        val checklist = VerificationChecklist.create(roles, ProfessionalType.NURSE, emptyList())

        assertEquals(
            listOf(
                DocumentType.ID_FRONT,
                DocumentType.ID_BACK,
                DocumentType.SELFIE,
                DocumentType.DEGREE,
                DocumentType.LICENSE,
            ),
            checklist.required,
        )
    }

    @Test
    fun `a person with no roles has no required documents`() {
        val checklist = VerificationChecklist.create(emptySet(), null, emptyList())

        assertTrue(checklist.required.isEmpty())
    }

    @Test
    fun `documents already uploaded are indexed by their type`() {
        val id = verificationDocument(type = DocumentType.ID_FRONT)
        val selfie = verificationDocument(type = DocumentType.SELFIE)

        val checklist = VerificationChecklist.create(roles(UserRole.PATIENT), null, listOf(id, selfie))

        assertEquals(id, checklist.documentFor(DocumentType.ID_FRONT))
        assertEquals(selfie, checklist.documentFor(DocumentType.SELFIE))
        assertEquals(null, checklist.documentFor(DocumentType.ID_BACK))
    }

    private fun roles(vararg r: UserRole): Set<UserRole> = r.toSet()
}

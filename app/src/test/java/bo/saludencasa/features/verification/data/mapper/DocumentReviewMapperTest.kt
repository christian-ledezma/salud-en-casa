package bo.saludencasa.features.verification.data.mapper

import bo.saludencasa.features.verification.data.model.DocumentReviewSubjectDto
import bo.saludencasa.features.verification.data.model.PendingDocumentReviewDto
import bo.saludencasa.features.verification.domain.model.DocumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DocumentReviewMapperTest {
    private fun queueRow(
        type: String = "ID_FRONT",
        createdAt: String = "2026-10-01T15:30:00+00:00",
    ) = PendingDocumentReviewDto(
        profileId = "p",
        documentType = type,
        caption = null,
        createdAt = createdAt,
        fullName = "Ana",
        email = "ana@example.com",
    )

    private fun subjectRow(
        roles: List<String> = listOf("PROFESSIONAL"),
        type: String? = "NURSE",
        status: String? = "PENDING",
    ) = DocumentReviewSubjectDto(
        id = "p",
        fullName = "Ana",
        email = "ana@example.com",
        heldRoles = roles,
        professionalType = type,
        professionalStatus = status,
    )

    // PostgREST writes the offset as +00:00 and keeps microseconds, a shape
    // Instant.parse refuses; reading it wrongly would blank the whole queue.
    @Test
    fun parsesTheTimestampOffsetPostgrestSends() {
        val review = queueRow(createdAt = "2026-10-01T15:30:00.123456+00:00").toPendingReview()

        assertEquals(Instant.parse("2026-10-01T15:30:00.123456Z"), review?.createdAt)
    }

    // Null, not a dropped row: the repository turns it into a failure so that a
    // full page is never mistaken for the last one.
    @Test
    fun anUnknownDocumentTypeIsReportedAsUnreadable() {
        assertNull(queueRow(type = "PASSPORT").toPendingReview())
    }

    @Test
    fun anUnparsableTimestampIsReportedAsUnreadable() {
        assertNull(queueRow(createdAt = "yesterday").toPendingReview())
    }

    @Test
    fun aKnownRowKeepsItsType() {
        assertEquals(DocumentType.DEGREE, queueRow(type = "DEGREE").toPendingReview()?.type)
    }

    @Test
    fun anEmptyHeldRolesArrayMapsToAnEmptySet() {
        val subject = subjectRow(roles = emptyList(), type = null, status = null).toSubject()

        assertNotNull(subject)
        assertTrue(subject!!.heldRoles.isEmpty())
    }

    @Test
    fun anUnknownRoleMakesTheSubjectUnreadable() {
        assertNull(subjectRow(roles = listOf("PROFESSIONAL", "AUDITOR")).toSubject())
    }

    @Test
    fun anUnknownProfessionalStatusMakesTheSubjectUnreadable() {
        assertNull(subjectRow(status = "SUSPENDED").toSubject())
    }
}

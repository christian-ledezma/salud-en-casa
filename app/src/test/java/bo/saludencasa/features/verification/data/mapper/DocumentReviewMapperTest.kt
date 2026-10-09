package bo.saludencasa.features.verification.data.mapper

import bo.saludencasa.features.verification.data.model.DocumentReviewSubjectDto
import bo.saludencasa.features.verification.data.model.PendingReviewSubjectDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DocumentReviewMapperTest {
    private fun queueRow(
        pendingCount: Int = 3,
        awaitingVerification: Boolean = false,
        waitingSince: String = "2026-10-01T15:30:00+00:00",
    ) = PendingReviewSubjectDto(
        profileId = "p",
        fullName = "Ana",
        email = "ana@example.com",
        pendingCount = pendingCount,
        awaitingVerification = awaitingVerification,
        waitingSince = waitingSince,
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
        val subject = queueRow(waitingSince = "2026-10-01T15:30:00.123456+00:00").toPendingSubject()

        assertEquals(Instant.parse("2026-10-01T15:30:00.123456Z"), subject?.waitingSince)
    }

    // Null, not a dropped row: the repository turns it into a failure so that a
    // full page is never mistaken for the last one.
    @Test
    fun anUnparsableTimestampIsReportedAsUnreadable() {
        assertNull(queueRow(waitingSince = "yesterday").toPendingSubject())
    }

    @Test
    fun aReadableRowKeepsItsPendingCount() {
        assertEquals(5, queueRow(pendingCount = 5).toPendingSubject()?.pendingCount)
    }

    // The card asks for a different act depending on this flag, and a person
    // with no pending document is there only because of it.
    @Test
    fun aRowWithoutPendingDocumentsKeepsTheVerifyingWork() {
        val subject = queueRow(pendingCount = 0, awaitingVerification = true).toPendingSubject()

        assertEquals(0, subject?.pendingCount)
        assertEquals(true, subject?.awaitingVerification)
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

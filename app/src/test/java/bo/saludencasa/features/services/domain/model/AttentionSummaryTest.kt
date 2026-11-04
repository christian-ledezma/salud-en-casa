package bo.saludencasa.features.services.domain.model

import bo.saludencasa.features.services.attentionSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AttentionSummaryTest {
    private val appointment = Instant.parse("2026-11-05T19:00:00Z")
    private val start = Instant.parse("2026-11-04T14:02:00Z")

    // A person looks for an appointment by the day it was set for, also once it began.
    @Test
    fun anAppointmentIsDatedByItsAppointmentEvenAfterItStarted() {
        assertEquals(
            AttentionCardDate(AttentionDateKind.APPOINTMENT, appointment),
            attentionSummary(scheduledAt = appointment, startedAt = start).cardDate,
        )
    }

    @Test
    fun anImmediateAttentionIsDatedByItsStart() {
        assertEquals(
            AttentionCardDate(AttentionDateKind.STARTED, start),
            attentionSummary(startedAt = start).cardDate,
        )
    }

    @Test
    fun anAttentionThatHasNotStartedIsDatedByTheAgreement() {
        val attention = attentionSummary()

        assertEquals(AttentionCardDate(AttentionDateKind.AGREED, attention.createdAt), attention.cardDate)
    }
}

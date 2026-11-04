package bo.saludencasa.features.services.data.mapper

import bo.saludencasa.features.services.data.model.AttentionDto
import bo.saludencasa.features.services.data.model.AttentionSummaryDto
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class AttentionMapperTest {
    private fun dto(
        status: String = "ASSIGNED",
        createdAt: String = "2026-10-29T12:00:00Z",
        iAmPatient: Boolean = true,
    ) = AttentionDto(
        serviceId = "s1",
        requestId = "r1",
        status = status,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        createdAt = createdAt,
        counterpartId = "p1",
        counterpartName = "María Quispe",
        counterpartPhotoUrl = null,
        iAmPatient = iAmPatient,
        city = "La Paz",
    )

    @Test
    fun mapsTheRowToAttentionWithTheCounterpart() {
        val attention = dto().toAttention()!!

        assertEquals("s1", attention.serviceId)
        assertEquals(ServiceStatus.ASSIGNED, attention.status)
        assertEquals(BigDecimal("150.00"), attention.finalAmountBob)
        assertEquals("p1", attention.counterpart.id)
        assertEquals("María Quispe", attention.counterpart.name)
        assertEquals(true, attention.iAmThePatient)
    }

    // A status the client does not know is a contract mismatch, not a value to
    // guess at: the row is dropped so the repository reports Unexpected.
    @Test
    fun rejectsAnUnknownStatus() {
        assertNull(dto(status = "ARCHIVED").toAttention())
    }

    @Test
    fun rejectsAnUnparseableCreatedAt() {
        assertNull(dto(createdAt = "not-a-timestamp").toAttention())
    }

    // What PostgREST really writes: microseconds and +00:00, not a Z.
    @Test
    fun readsTheOffsetPostgrestWrites() {
        val attention = dto(createdAt = "2026-10-29T12:00:00.123456+00:00").toAttention()

        assertEquals(Instant.parse("2026-10-29T12:00:00.123456Z"), attention?.createdAt)
    }

    private fun summaryDto(
        status: String = "COMPLETED",
        scheduledAt: String? = null,
        startedAt: String? = null,
    ) = AttentionSummaryDto(
        requestId = "r1",
        status = status,
        finalAmountBob = BigDecimal("45.00"),
        serviceTypeName = "Inyectables",
        counterpartName = "María Quispe",
        createdAt = "2026-10-29T12:00:00.123456+00:00",
        scheduledAt = scheduledAt,
        startedAt = startedAt,
    )

    // The start decides the date of the card: read wrong, the card says «Acordada»
    // for an attention under way.
    @Test
    fun theStartOfTheAttentionReachesTheSummary() {
        assertEquals(
            Instant.parse("2026-11-04T14:02:00Z"),
            summaryDto(startedAt = "2026-11-04T14:02:00+00:00").toAttentionSummary()?.startedAt,
        )
    }

    @Test
    fun anUnreadableStartIsAContractMismatch() {
        assertNull(summaryDto(startedAt = "ayer").toAttentionSummary())
    }

    @Test
    fun anImmediateAttentionHasNoAppointment() {
        assertNull(summaryDto().toAttentionSummary()?.scheduledAt)
    }

    @Test
    fun aScheduledAttentionKeepsItsAppointment() {
        val summary = summaryDto(scheduledAt = "2026-11-05T14:30:00+00:00").toAttentionSummary()

        assertEquals(Instant.parse("2026-11-05T14:30:00Z"), summary?.scheduledAt)
    }

    // One unreadable row fails the page instead of showing a card with a hole.
    @Test
    fun aSummaryWithAnUnknownStatusOrADateItCannotReadIsRejected() {
        assertNull(summaryDto(status = "ARCHIVED").toAttentionSummary())
        assertNull(summaryDto(scheduledAt = "not-a-timestamp").toAttentionSummary())
    }

    // docs/decisions.md, 2026-11-04, HU-23: the engine leaves these out for a
    // professional once the attention ended, and that is a row like any other.
    @Test
    fun readsARowWithoutTheAddress() {
        val place = dto().toAttention()!!.place

        assertEquals("La Paz", place.city)
        assertNull(place.addressText)
        assertNull(place.latitude)
    }

    @Test
    fun readsTheDatesOfTheProgress() {
        val attention =
            dto()
                .copy(startedAt = "2026-11-04T14:02:00+00:00", finishedAt = "2026-11-04T15:40:00+00:00")
                .toAttention()!!

        assertEquals(Instant.parse("2026-11-04T14:02:00Z"), attention.startedAt)
        assertEquals(Instant.parse("2026-11-04T15:40:00Z"), attention.finishedAt)
    }

    @Test
    fun rejectsAnUnparseableStartDate() {
        assertNull(dto().copy(startedAt = "ayer").toAttention())
    }

    // The engine refused the jump: the screen rereads instead of showing an error.
    @Test
    fun aRefusedTransitionMeansTheAttentionChanged() {
        val terminal = IllegalStateException("service_status_is_terminal")
        val invalid = IllegalStateException("invalid_service_transition")

        assertEquals(ServiceError.AttentionChanged, terminal.toServiceError())
        assertEquals(ServiceError.AttentionChanged, invalid.toServiceError())
        assertEquals(ServiceError.Unexpected, IllegalStateException("boom").toServiceError())
    }
}

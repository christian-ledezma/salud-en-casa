package bo.saludencasa.features.services.presentation

import bo.saludencasa.R
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.AttentionCancellation
import bo.saludencasa.features.services.domain.model.ServiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AttentionTimelineTest {
    private val started = Instant.parse("2026-11-04T14:02:00Z")
    private val finished = Instant.parse("2026-11-04T15:40:00Z")

    private fun states(
        status: ServiceStatus,
        startedAt: Instant? = null,
        finishedAt: Instant? = null,
    ) = attention(status = status).copy(startedAt = startedAt, finishedAt = finishedAt).timeline().map { it.state }

    @Test
    fun beforeArrivingTheArrivalIsWhatComesNext() {
        assertEquals(
            listOf(TimelineStepState.DONE, TimelineStepState.CURRENT, TimelineStepState.PENDING),
            states(ServiceStatus.ASSIGNED),
        )
    }

    @Test
    fun onceArrivedTheEndIsWhatComesNext() {
        assertEquals(
            listOf(TimelineStepState.DONE, TimelineStepState.DONE, TimelineStepState.CURRENT),
            states(ServiceStatus.IN_PROGRESS, started),
        )
    }

    // HU-23, criterion 4: a completed attention shows when it started and ended.
    @Test
    fun aCompletedAttentionCarriesBothTimes() {
        val steps =
            attention(status = ServiceStatus.COMPLETED)
                .copy(startedAt = started, finishedAt = finished)
                .timeline()

        assertEquals(List(3) { TimelineStepState.DONE }, steps.map { it.state })
        assertEquals(started, steps[1].at)
        assertEquals(finished, steps[2].at)
    }

    // No arrival to show, and nothing left to say "pending" about.
    @Test
    fun aCancelledAttentionEndsInItsCancellation() {
        val beforeArriving = attention(status = ServiceStatus.CANCELLED).timeline()
        val afterArriving = attention(status = ServiceStatus.CANCELLED).copy(startedAt = started).timeline()

        assertEquals(
            listOf(R.string.attention_step_agreed, R.string.attention_step_cancelled),
            beforeArriving.map { it.titleRes },
        )
        assertEquals(
            listOf(R.string.attention_step_agreed, R.string.attention_step_arrival, R.string.attention_step_cancelled),
            afterArriving.map { it.titleRes },
        )
    }

    @Test
    fun theCancellationCarriesItsTime() {
        val at = Instant.parse("2026-11-04T20:20:00Z")
        val cancelled =
            attention(
                status = ServiceStatus.CANCELLED,
                cancellation = AttentionCancellation("x", byMe = false, at = at),
            )

        assertEquals(at, cancelled.timeline().last().at)
    }

    // The geo scheme takes latitude first; swapped, it opens the wrong place in
    // silence, the mistake CLAUDE.md warns about for PostGIS the other way round.
    @Test
    fun theDirectionsTakeLatitudeFirst() {
        assertEquals("geo:-16.5103,-68.1218?q=-16.5103,-68.1218", directionsUri(-16.5103, -68.1218))
    }
}

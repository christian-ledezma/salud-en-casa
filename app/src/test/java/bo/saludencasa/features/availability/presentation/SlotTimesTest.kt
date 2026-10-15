package bo.saludencasa.features.availability.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class SlotTimesTest {
    // A start of 23:30 would have no end after it: the column requires
    // end_time > start_time and the grid stops at 23:30.
    @Test
    fun everyStartOnOfferHasAtLeastOneEndAfterIt() {
        assertTrue(SlotTimes.starts.all { SlotTimes.ends(it).isNotEmpty() })
    }

    @Test
    fun theGridCoversTheWholeDayEveryHalfHour() {
        assertEquals(LocalTime.MIDNIGHT, SlotTimes.starts.first())
        assertEquals(LocalTime.of(23, 0), SlotTimes.starts.last())
        assertEquals(47, SlotTimes.starts.size)
        assertEquals(LocalTime.of(23, 30), SlotTimes.ends(LocalTime.of(23, 0)).single())
    }

    // The engine rejects an end that is not after the start, so the grid never
    // offers one.
    @Test
    fun noEndOnOfferIsAtOrBeforeItsStart() {
        val start = LocalTime.of(14, 0)

        val ends = SlotTimes.ends(start)

        assertEquals(LocalTime.of(14, 30), ends.first())
        assertFalse(ends.any { it <= start })
    }

    @Test
    fun anEndAlreadyAfterTheStartIsKept() {
        assertEquals(
            LocalTime.of(18, 0),
            SlotTimes.endAfter(start = LocalTime.of(14, 0), currentEnd = LocalTime.of(18, 0)),
        )
    }

    @Test
    fun anEndAtOrBeforeTheStartIsPushedOneStepPastIt() {
        assertEquals(
            LocalTime.of(14, 30),
            SlotTimes.endAfter(start = LocalTime.of(14, 0), currentEnd = LocalTime.of(10, 0)),
        )
        assertEquals(
            LocalTime.of(14, 30),
            SlotTimes.endAfter(start = LocalTime.of(14, 0), currentEnd = LocalTime.of(14, 0)),
        )
    }
}

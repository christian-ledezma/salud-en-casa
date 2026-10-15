package bo.saludencasa.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeChipRowTest {
    private val times =
        listOf(
            LocalTime.of(9, 0),
            LocalTime.of(11, 30),
            LocalTime.of(14, 0),
            LocalTime.of(18, 30),
        )

    // The row inserts a label before each part of the day, so scrolling to the
    // position of the hour in the list would land on another hour.
    @Test
    fun theIndexCountsTheLabelThatPrecedesEachPartOfTheDay() {
        assertEquals(1, timeChipIndex(times, LocalTime.of(9, 0)))
        assertEquals(2, timeChipIndex(times, LocalTime.of(11, 30)))
        assertEquals(4, timeChipIndex(times, LocalTime.of(14, 0)))
        assertEquals(6, timeChipIndex(times, LocalTime.of(18, 30)))
    }

    @Test
    fun anHourThatIsNotOnOfferLeavesTheRowAtItsStart() {
        assertEquals(0, timeChipIndex(times, LocalTime.of(7, 15)))
    }
}

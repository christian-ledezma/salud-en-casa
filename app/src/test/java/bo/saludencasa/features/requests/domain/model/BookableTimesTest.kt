package bo.saludencasa.features.requests.domain.model

import bo.saludencasa.core.vo.PartOfDay
import bo.saludencasa.features.requests.bookableStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class BookableTimesTest {
    private val thursday = LocalDate.of(2026, 10, 15)
    private val monday = LocalDate.of(2026, 10, 19)

    // A professional who works Thursday and Monday: the carousel must still
    // show Friday to Sunday between them, dimmed, or the patient would read
    // Monday as the day after Thursday.
    @Test
    fun theDaysBetweenTwoWorkingDaysAreKeptAsDaysWithoutTimes() {
        val times = BookableTimes(listOf(bookableStart(monday, 9), bookableStart(thursday, 15)))

        assertEquals((15..19).map { LocalDate.of(2026, 10, it) }, times.days)
        assertFalse(times.hasTimesOn(LocalDate.of(2026, 10, 17)))
        assertTrue(times.hasTimesOn(monday))
    }

    @Test
    fun theFirstDayWithTimesIsTheEarliestNotTheFirstListed() {
        val times = BookableTimes(listOf(bookableStart(monday, 9), bookableStart(thursday, 15)))

        assertEquals(thursday, times.firstDayWithTimes())
    }

    @Test
    fun noStartsMeansNoDaysAndNoFirstDay() {
        val times = BookableTimes(emptyList())

        assertTrue(times.isEmpty)
        assertTrue(times.days.isEmpty())
        assertNull(times.firstDayWithTimes())
    }

    @Test
    fun aDayIsSplitIntoItsPartsInOrderAndEachPartIsSortedByTime() {
        val times =
            BookableTimes(
                listOf(
                    bookableStart(monday, 19),
                    bookableStart(monday, 10),
                    bookableStart(monday, 9, 30),
                    bookableStart(monday, 14),
                ),
            )

        val parts = times.on(monday)

        assertEquals(listOf(PartOfDay.MORNING, PartOfDay.AFTERNOON, PartOfDay.EVENING), parts.keys.toList())
        assertEquals(
            listOf(LocalTime.of(9, 30), LocalTime.of(10, 0)),
            parts.getValue(PartOfDay.MORNING).map(BookableStart::time),
        )
    }

    @Test
    fun aPartWithNoStartsIsLeftOutRatherThanShownEmpty() {
        val times = BookableTimes(listOf(bookableStart(monday, 15)))

        assertEquals(setOf(PartOfDay.AFTERNOON), times.on(monday).keys)
    }
}

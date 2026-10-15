package bo.saludencasa.core.vo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class PartOfDayTest {
    @Test
    fun thePartsChangeExactlyAtNoonAndAtSixInTheEvening() {
        assertEquals(PartOfDay.MORNING, PartOfDay.of(LocalTime.of(11, 59)))
        assertEquals(PartOfDay.AFTERNOON, PartOfDay.of(LocalTime.NOON))
        assertEquals(PartOfDay.AFTERNOON, PartOfDay.of(LocalTime.of(17, 59)))
        assertEquals(PartOfDay.EVENING, PartOfDay.of(LocalTime.of(18, 0)))
    }
}

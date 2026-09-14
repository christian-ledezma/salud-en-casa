package bo.saludencasa.features.profile.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BirthDateTest {
    private val today = LocalDate.of(2026, 9, 13)

    @Test
    fun acceptsADateInThePast() {
        val birthDate = BirthDate.create(LocalDate.of(1990, 5, 14), today).getOrNull()

        assertEquals(LocalDate.of(1990, 5, 14), birthDate?.value)
    }

    // The column check is `birth_date < current_date`, so today itself is the
    // exact boundary the database refuses. Accepting it here would turn a
    // rejected save into an unexplained failure from the server.
    @Test
    fun rejectsToday() {
        assertNull(BirthDate.create(today, today).getOrNull())
    }

    @Test
    fun rejectsADateInTheFuture() {
        assertNull(BirthDate.create(today.plusDays(1), today).getOrNull())
    }

    @Test
    fun acceptsTheOldestDateAllowed() {
        val oldest = today.minusYears(120)

        assertEquals(oldest, BirthDate.create(oldest, today).getOrNull()?.value)
    }

    @Test
    fun rejectsADateOlderThanTheOldestAllowed() {
        assertNull(BirthDate.create(today.minusYears(120).minusDays(1), today).getOrNull())
    }
}

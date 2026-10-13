package bo.saludencasa.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RelativeTimeTest {
    // The exact borders of the five buckets. One minute on either side is what
    // tells a comment from this afternoon apart from one from yesterday.
    @Test
    fun `under an hour the count is not shown at all`() {
        assertEquals(AgeUnit.MINUTES, age(Duration.ofMinutes(59)).unit)
    }

    @Test
    fun `an hour exactly is already measured in hours`() {
        assertEquals(Age(AgeUnit.HOURS, 1), age(Duration.ofMinutes(60)))
    }

    @Test
    fun `the last minute of the first day is still measured in hours`() {
        assertEquals(Age(AgeUnit.HOURS, 23), age(Duration.ofHours(24).minusMinutes(1)))
    }

    @Test
    fun `a day exactly is measured in days`() {
        assertEquals(Age(AgeUnit.DAYS, 1), age(Duration.ofDays(1)))
    }

    @Test
    fun `the day before the month boundary is still measured in days`() {
        assertEquals(Age(AgeUnit.DAYS, 29), age(Duration.ofDays(29)))
    }

    @Test
    fun `thirty days become one month`() {
        assertEquals(Age(AgeUnit.MONTHS, 1), age(Duration.ofDays(30)))
    }

    @Test
    fun `the day before the year boundary is still measured in months`() {
        assertEquals(Age(AgeUnit.MONTHS, 12), age(Duration.ofDays(364)))
    }

    @Test
    fun `a year exactly is measured in years`() {
        assertEquals(Age(AgeUnit.YEARS, 1), age(Duration.ofDays(365)))
    }

    // A clock that ran backwards, or a row stamped a second into the future,
    // must not produce a negative age that the plural would read as "Hace -1".
    @Test
    fun `an instant in the future is not negative time`() {
        assertEquals(Age(AgeUnit.MINUTES, 0), age(Duration.ofHours(-5)))
    }
}

private val NOW = Instant.parse("2026-10-11T12:00:00Z")

private fun age(elapsed: Duration): Age = ageOf(NOW.minus(elapsed), NOW)

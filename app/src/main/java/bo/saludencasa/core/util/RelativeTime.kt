package bo.saludencasa.core.util

import java.time.Duration
import java.time.Instant

enum class AgeUnit {
    MINUTES,
    HOURS,
    DAYS,
    MONTHS,
    YEARS,
}

data class Age(
    val unit: AgeUnit,
    val count: Int,
)

// docs/decisions.md, 2026-10-11, a comment is dated by its age.
fun ageOf(
    instant: Instant,
    now: Instant,
): Age {
    val minutes = Duration.between(instant, now).toMinutes().coerceAtLeast(0)

    return when {
        minutes < MINUTES_IN_AN_HOUR -> Age(AgeUnit.MINUTES, minutes.toInt())
        minutes < MINUTES_IN_A_DAY -> Age(AgeUnit.HOURS, (minutes / MINUTES_IN_AN_HOUR).toInt())
        minutes < MINUTES_IN_A_MONTH -> Age(AgeUnit.DAYS, (minutes / MINUTES_IN_A_DAY).toInt())
        minutes < MINUTES_IN_A_YEAR -> Age(AgeUnit.MONTHS, (minutes / MINUTES_IN_A_MONTH).toInt())
        else -> Age(AgeUnit.YEARS, (minutes / MINUTES_IN_A_YEAR).toInt())
    }
}

private const val MINUTES_IN_AN_HOUR = 60L
private const val MINUTES_IN_A_DAY = 60L * 24
private const val MINUTES_IN_A_MONTH = MINUTES_IN_A_DAY * 30
private const val MINUTES_IN_A_YEAR = MINUTES_IN_A_DAY * 365

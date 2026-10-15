package bo.saludencasa.features.availability.presentation

import java.time.LocalTime

// The half-hour grid the screen offers. It is the step bookable_start_times
// walks a slot with, so a declared slot never yields a start time the patient
// cannot pick.
internal object SlotTimes {
    private const val STEP_MINUTES = 30L
    private const val SECONDS_PER_STEP = STEP_MINUTES * 60
    private val LAST_END: LocalTime = LocalTime.of(23, 30)

    val starts: List<LocalTime> = grid(LocalTime.MIDNIGHT, LAST_END.minusMinutes(STEP_MINUTES))

    fun ends(start: LocalTime): List<LocalTime> = grid(start.plusMinutes(STEP_MINUTES), LAST_END)

    // Moving the start past the end would leave a range the engine rejects, so
    // the end follows instead of waiting for the rejection.
    fun endAfter(
        start: LocalTime,
        currentEnd: LocalTime,
    ): LocalTime = if (currentEnd > start) currentEnd else start.plusMinutes(STEP_MINUTES)

    private fun grid(
        from: LocalTime,
        to: LocalTime,
    ): List<LocalTime> {
        if (from > to) return emptyList()
        val steps = (to.toSecondOfDay() - from.toSecondOfDay()) / SECONDS_PER_STEP
        return (0..steps).map { from.plusMinutes(it * STEP_MINUTES) }
    }
}

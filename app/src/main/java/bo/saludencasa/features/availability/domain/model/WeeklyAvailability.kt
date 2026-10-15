package bo.saludencasa.features.availability.domain.model

import java.time.DayOfWeek

// The seven days always carry a list, even when empty, so the screen does not
// have to decide what to show on a day the professional has not touched.
data class WeeklyAvailability(
    val slotsByDay: Map<DayOfWeek, List<AvailabilitySlot>>,
) {
    companion object {
        fun from(slots: List<AvailabilitySlot>): WeeklyAvailability {
            val grouped = slots.groupBy { it.dayOfWeek }
            val byDay =
                DayOfWeek.entries.associateWith { day ->
                    grouped[day]?.sortedBy { it.startTime } ?: emptyList()
                }
            return WeeklyAvailability(byDay)
        }

        val Empty: WeeklyAvailability = from(emptyList())
    }
}

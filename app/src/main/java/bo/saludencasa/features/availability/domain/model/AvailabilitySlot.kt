package bo.saludencasa.features.availability.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class AvailabilitySlot(
    val id: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
)

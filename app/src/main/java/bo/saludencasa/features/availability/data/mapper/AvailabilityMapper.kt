package bo.saludencasa.features.availability.data.mapper

import bo.saludencasa.features.availability.data.model.AvailabilitySlotDto
import bo.saludencasa.features.availability.domain.model.AvailabilitySlot
import java.time.DayOfWeek
import java.time.LocalTime

internal fun AvailabilitySlotDto.toDomain(): AvailabilitySlot =
    AvailabilitySlot(
        id = id,
        // The schema stores ISO weekdays (1 = Monday, 7 = Sunday), matching
        // java.time.DayOfWeek.of exactly.
        dayOfWeek = DayOfWeek.of(dayOfWeek),
        startTime = LocalTime.parse(startTime),
        endTime = LocalTime.parse(endTime),
    )

internal fun LocalTime.toColumnText(): String = toString().let { if (it.length == 5) "$it:00" else it }

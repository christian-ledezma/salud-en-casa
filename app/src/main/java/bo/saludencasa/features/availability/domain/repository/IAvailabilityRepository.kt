package bo.saludencasa.features.availability.domain.repository

import bo.saludencasa.features.availability.domain.model.AvailabilityResult
import bo.saludencasa.features.availability.domain.model.SlotWriteResult
import java.time.DayOfWeek
import java.time.LocalTime

interface IAvailabilityRepository {
    suspend fun getMySlots(): AvailabilityResult

    suspend fun addSlot(
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
    ): SlotWriteResult

    suspend fun removeSlot(slotId: String): SlotWriteResult
}

package bo.saludencasa.features.availability.data.repository

import bo.saludencasa.features.availability.data.datasource.SupabaseAvailabilityDataSource
import bo.saludencasa.features.availability.data.mapper.toAvailabilityError
import bo.saludencasa.features.availability.data.mapper.toColumnText
import bo.saludencasa.features.availability.data.mapper.toDomain
import bo.saludencasa.features.availability.data.model.AvailabilitySlotRow
import bo.saludencasa.features.availability.domain.model.AvailabilityError
import bo.saludencasa.features.availability.domain.model.AvailabilityResult
import bo.saludencasa.features.availability.domain.model.SlotWriteResult
import bo.saludencasa.features.availability.domain.model.WeeklyAvailability
import bo.saludencasa.features.availability.domain.repository.IAvailabilityRepository
import java.time.DayOfWeek
import java.time.LocalTime

class AvailabilityRepository(
    private val dataSource: SupabaseAvailabilityDataSource,
) : IAvailabilityRepository {
    override suspend fun getMySlots(): AvailabilityResult {
        val userId = dataSource.currentUserId() ?: return AvailabilityResult.Failure(AvailabilityError.Unexpected)
        return runCatching {
            dataSource.findSlotsOf(userId).map { it.toDomain() }
        }.fold(
            onSuccess = { AvailabilityResult.Loaded(WeeklyAvailability.from(it)) },
            onFailure = { AvailabilityResult.Failure(it.toAvailabilityError()) },
        )
    }

    override suspend fun addSlot(
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
    ): SlotWriteResult {
        val userId = dataSource.currentUserId() ?: return SlotWriteResult.Failure(AvailabilityError.Unexpected)
        return runCatching {
            dataSource.insertSlot(
                AvailabilitySlotRow(
                    professionalId = userId,
                    dayOfWeek = dayOfWeek.value,
                    startTime = startTime.toColumnText(),
                    endTime = endTime.toColumnText(),
                ),
            )
        }.fold(
            onSuccess = { SlotWriteResult.Success },
            onFailure = { SlotWriteResult.Failure(it.toAvailabilityError()) },
        )
    }

    override suspend fun removeSlot(slotId: String): SlotWriteResult =
        runCatching { dataSource.deleteSlot(slotId) }.fold(
            onSuccess = { SlotWriteResult.Success },
            onFailure = { SlotWriteResult.Failure(it.toAvailabilityError()) },
        )
}

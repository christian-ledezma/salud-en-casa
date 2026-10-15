package bo.saludencasa.features.availability

import bo.saludencasa.features.availability.domain.model.AvailabilityError
import bo.saludencasa.features.availability.domain.model.AvailabilityResult
import bo.saludencasa.features.availability.domain.model.AvailabilitySlot
import bo.saludencasa.features.availability.domain.model.SlotWriteResult
import bo.saludencasa.features.availability.domain.model.WeeklyAvailability
import bo.saludencasa.features.availability.domain.repository.IAvailabilityRepository
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class FakeAvailabilityRepository : IAvailabilityRepository {
    private val slots = mutableListOf<AvailabilitySlot>()

    val addedSlots = mutableListOf<Triple<DayOfWeek, LocalTime, LocalTime>>()
    val removedIds = mutableListOf<String>()

    var getResult: AvailabilityResult? = null
    var addResult: SlotWriteResult = SlotWriteResult.Success
    var removeResult: SlotWriteResult = SlotWriteResult.Success

    fun seed(vararg initial: AvailabilitySlot) {
        slots.clear()
        slots.addAll(initial)
    }

    override suspend fun getMySlots(): AvailabilityResult =
        getResult ?: AvailabilityResult.Loaded(WeeklyAvailability.from(slots))

    override suspend fun addSlot(
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
    ): SlotWriteResult {
        addedSlots += Triple(dayOfWeek, startTime, endTime)
        if (addResult is SlotWriteResult.Success) {
            slots +=
                AvailabilitySlot(
                    id = UUID.randomUUID().toString(),
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                )
        }
        return addResult
    }

    override suspend fun removeSlot(slotId: String): SlotWriteResult {
        removedIds += slotId
        if (removeResult is SlotWriteResult.Success) {
            slots.removeAll { it.id == slotId }
        }
        return removeResult
    }
}

internal fun slot(
    id: String = "s-1",
    dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    startTime: LocalTime = LocalTime.of(9, 0),
    endTime: LocalTime = LocalTime.of(10, 0),
): AvailabilitySlot = AvailabilitySlot(id, dayOfWeek, startTime, endTime)

internal fun failure(error: AvailabilityError): SlotWriteResult = SlotWriteResult.Failure(error)

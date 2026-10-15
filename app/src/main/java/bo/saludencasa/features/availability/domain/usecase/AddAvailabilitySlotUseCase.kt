package bo.saludencasa.features.availability.domain.usecase

import bo.saludencasa.features.availability.domain.model.AvailabilityError
import bo.saludencasa.features.availability.domain.model.SlotWriteResult
import bo.saludencasa.features.availability.domain.repository.IAvailabilityRepository
import java.time.DayOfWeek
import java.time.LocalTime

class AddAvailabilitySlotUseCase(
    private val repository: IAvailabilityRepository,
) {
    // The end-after-start rule is enforced by the schema too (constraint
    // slot_ends_after_it_starts); doing it here first keeps the client from
    // reaching the network with a value it can already tell is wrong, so the
    // user sees the error in the dialog instantly.
    suspend operator fun invoke(
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
    ): SlotWriteResult {
        if (!endTime.isAfter(startTime)) {
            return SlotWriteResult.Failure(AvailabilityError.EndNotAfterStart)
        }
        return repository.addSlot(dayOfWeek, startTime, endTime)
    }
}

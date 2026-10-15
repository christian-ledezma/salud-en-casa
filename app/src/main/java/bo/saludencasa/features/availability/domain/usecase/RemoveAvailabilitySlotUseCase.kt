package bo.saludencasa.features.availability.domain.usecase

import bo.saludencasa.features.availability.domain.model.SlotWriteResult
import bo.saludencasa.features.availability.domain.repository.IAvailabilityRepository

class RemoveAvailabilitySlotUseCase(
    private val repository: IAvailabilityRepository,
) {
    suspend operator fun invoke(slotId: String): SlotWriteResult = repository.removeSlot(slotId)
}

package bo.saludencasa.features.availability.domain.usecase

import bo.saludencasa.features.availability.domain.model.AvailabilityResult
import bo.saludencasa.features.availability.domain.repository.IAvailabilityRepository

class GetMyAvailabilityUseCase(
    private val repository: IAvailabilityRepository,
) {
    suspend operator fun invoke(): AvailabilityResult = repository.getMySlots()
}

package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.PositionResult
import bo.saludencasa.features.location.domain.repository.IDeviceLocationRepository

class GetCurrentPositionUseCase(
    private val repository: IDeviceLocationRepository,
) {
    suspend operator fun invoke(): PositionResult = repository.currentPosition()
}

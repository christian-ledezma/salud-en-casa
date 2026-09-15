package bo.saludencasa.features.location.domain.repository

import bo.saludencasa.features.location.domain.model.PositionResult

interface IDeviceLocationRepository {
    suspend fun currentPosition(): PositionResult
}

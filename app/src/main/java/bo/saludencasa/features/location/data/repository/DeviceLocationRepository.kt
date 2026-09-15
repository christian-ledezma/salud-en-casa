package bo.saludencasa.features.location.data.repository

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.data.datasource.DeviceLocationDataSource
import bo.saludencasa.features.location.data.mapper.toAddressError
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.PositionResult
import bo.saludencasa.features.location.domain.repository.IDeviceLocationRepository
import kotlinx.coroutines.CancellationException

class DeviceLocationRepository(
    private val dataSource: DeviceLocationDataSource,
) : IDeviceLocationRepository {
    override suspend fun currentPosition(): PositionResult {
        if (!dataSource.hasPermission()) return PositionResult.Failure(AddressError.LocationPermissionDenied)

        return try {
            // A device that has never had a fix answers with nothing rather
            // than with an error, and indoors that is the common case.
            val location =
                dataSource.currentLocation()
                    ?: return PositionResult.Failure(AddressError.LocationUnavailable)

            Coordinate
                .create(location.latitude, location.longitude)
                .getOrNull()
                ?.let(PositionResult::Located)
                ?: PositionResult.Failure(AddressError.LocationUnavailable)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            PositionResult.Failure(failure.toAddressError())
        }
    }
}

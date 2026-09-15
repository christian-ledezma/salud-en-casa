package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.domain.model.GeocodingResult
import bo.saludencasa.features.location.domain.repository.IGeocodingRepository

class DescribePointUseCase(
    private val repository: IGeocodingRepository,
) {
    suspend operator fun invoke(coordinate: Coordinate): GeocodingResult = repository.describe(coordinate)
}

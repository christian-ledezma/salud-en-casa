package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.GeocodingResult
import bo.saludencasa.features.location.domain.repository.IGeocodingRepository

class FindPlaceUseCase(
    private val repository: IGeocodingRepository,
) {
    suspend operator fun invoke(query: String): GeocodingResult =
        if (query.isBlank()) GeocodingResult.NotFound else repository.find(query.trim())
}

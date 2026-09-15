package bo.saludencasa.features.location.domain.repository

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.domain.model.GeocodingResult

interface IGeocodingRepository {
    suspend fun describe(coordinate: Coordinate): GeocodingResult

    suspend fun find(query: String): GeocodingResult
}

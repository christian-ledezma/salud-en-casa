package bo.saludencasa.features.search.domain.repository

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.search.domain.model.NearbyProfessionalsResult
import bo.saludencasa.features.search.domain.model.SearchCriteria

interface IProfessionalSearchRepository {
    suspend fun findNearby(
        origin: Coordinate,
        criteria: SearchCriteria,
        offset: Int,
        limit: Int,
    ): NearbyProfessionalsResult
}

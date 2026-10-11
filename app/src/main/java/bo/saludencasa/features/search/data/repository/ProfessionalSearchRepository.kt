package bo.saludencasa.features.search.data.repository

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.search.data.datasource.SupabaseProfessionalSearchDataSource
import bo.saludencasa.features.search.data.mapper.toNearbyProfessional
import bo.saludencasa.features.search.data.mapper.toParams
import bo.saludencasa.features.search.data.mapper.toSearchError
import bo.saludencasa.features.search.domain.model.NearbyProfessionalsResult
import bo.saludencasa.features.search.domain.model.SearchCriteria
import bo.saludencasa.features.search.domain.model.SearchError
import bo.saludencasa.features.search.domain.repository.IProfessionalSearchRepository
import kotlinx.coroutines.CancellationException

class ProfessionalSearchRepository(
    private val dataSource: SupabaseProfessionalSearchDataSource,
) : IProfessionalSearchRepository {
    override suspend fun findNearby(
        origin: Coordinate,
        criteria: SearchCriteria,
        offset: Int,
        limit: Int,
    ): NearbyProfessionalsResult {
        if (dataSource.currentUserId() == null) {
            return NearbyProfessionalsResult.Failure(SearchError.NotSignedIn)
        }

        return try {
            val rows = dataSource.searchNearby(criteria.toParams(origin, offset, limit))
            val professionals = rows.map { it.toNearbyProfessional() }

            if (professionals.any { it == null }) {
                NearbyProfessionalsResult.Failure(SearchError.UnreadableResult)
            } else {
                NearbyProfessionalsResult.Found(professionals.filterNotNull())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            NearbyProfessionalsResult.Failure(failure.toSearchError())
        }
    }
}

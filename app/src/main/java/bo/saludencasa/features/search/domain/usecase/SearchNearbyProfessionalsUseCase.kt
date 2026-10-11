package bo.saludencasa.features.search.domain.usecase

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.search.domain.model.NearbyProfessionalsPageResult
import bo.saludencasa.features.search.domain.model.NearbyProfessionalsResult
import bo.saludencasa.features.search.domain.model.SearchCriteria
import bo.saludencasa.features.search.domain.repository.IProfessionalSearchRepository

class SearchNearbyProfessionalsUseCase(
    private val repository: IProfessionalSearchRepository,
) {
    suspend operator fun invoke(
        origin: Coordinate,
        criteria: SearchCriteria,
        offset: Int,
    ): NearbyProfessionalsPageResult =
        when (
            val result = repository.findNearby(origin, criteria, offset = offset, limit = PAGE_SIZE)
        ) {
            is NearbyProfessionalsResult.Found -> {
                NearbyProfessionalsPageResult.Loaded(
                    professionals = result.professionals,
                    // A short page is the only signal that the list ended, which
                    // is why no row is ever dropped on the way here: one that
                    // cannot be read fails its whole page instead.
                    endReached = result.professionals.size < PAGE_SIZE,
                )
            }

            is NearbyProfessionalsResult.Failure -> {
                NearbyProfessionalsPageResult.Failure(result.error)
            }
        }

    companion object {
        const val PAGE_SIZE: Int = 20
    }
}

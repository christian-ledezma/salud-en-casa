package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.search.domain.model.NearbyProfessionalsPageResult
import bo.saludencasa.features.search.domain.model.SearchCriteria
import bo.saludencasa.features.search.domain.model.SearchRadius
import bo.saludencasa.features.search.domain.usecase.SearchNearbyProfessionalsUseCase

// docs/decisions.md, 2026-10-23, HT-23: who «Atención ahora» shows on the map.
class FindProfessionalsAvailableNowUseCase(
    private val searchNearbyProfessionals: SearchNearbyProfessionalsUseCase,
) {
    suspend operator fun invoke(
        around: Coordinate,
        serviceTypeId: String?,
    ): NearbyProfessionalsPageResult =
        searchNearbyProfessionals(
            origin = around,
            criteria =
                SearchCriteria(
                    radius = SearchRadius.entries.last(),
                    serviceTypeId = serviceTypeId,
                    availableNowOnly = true,
                ),
            offset = 0,
        )
}

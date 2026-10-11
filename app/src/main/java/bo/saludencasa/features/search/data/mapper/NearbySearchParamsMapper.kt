package bo.saludencasa.features.search.data.mapper

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.search.data.model.NearbySearchParams
import bo.saludencasa.features.search.domain.model.SearchCriteria

internal fun SearchCriteria.toParams(
    origin: Coordinate,
    offset: Int,
    limit: Int,
): NearbySearchParams =
    NearbySearchParams(
        latitude = origin.latitude,
        longitude = origin.longitude,
        radiusKm = radius.km.toDouble(),
        serviceTypeId = serviceTypeId,
        // Three states on the engine's side: null does not filter at all, and
        // false filters for the unavailable. An off filter travelling as false
        // would show exactly the professionals nobody asked to see.
        availableNow = if (availableNowOnly) true else null,
        limit = limit,
        offset = offset,
    )

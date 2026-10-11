package bo.saludencasa.features.search.data.mapper

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.search.data.model.NearbyProfessionalDto
import bo.saludencasa.features.search.domain.model.NearbyProfessional
import kotlin.math.roundToInt

// Null when the row carries no rate: docs/decisions.md, 2026-10-09, an
// unreadable row fails its page.
internal fun NearbyProfessionalDto.toNearbyProfessional(): NearbyProfessional? {
    val rate = baseRateBob ?: return null

    return NearbyProfessional(
        professionalId = id,
        fullName = fullName,
        photoUrl = photoUrl,
        professionalType = ProfessionalType.entries.firstOrNull { it.name == professionalType },
        specialty = specialty,
        baseRateBob = rate,
        averageRating = averageRating,
        totalReviews = totalReviews,
        availableNow = availableNow,
        distanceM = distanceM.roundToInt(),
        // Latitude first here, longitude second: the engine projects st_y as
        // the latitude and st_x as the longitude, and swapping them validates
        // just as well while putting the pin on another continent.
        basePoint =
            if (baseLatitude == null || baseLongitude == null) {
                null
            } else {
                Coordinate.create(baseLatitude, baseLongitude).getOrNull()
            },
    )
}

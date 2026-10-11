package bo.saludencasa.features.search.domain.model

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import java.math.BigDecimal

data class NearbyProfessional(
    val professionalId: String,
    val fullName: String,
    val photoUrl: String?,
    // Null when this version does not know the value the engine sent. A label
    // is not a reason to drop a verified professional off the map, and the
    // specialty takes its place on the card.
    val professionalType: ProfessionalType?,
    val specialty: String?,
    val baseRateBob: BigDecimal,
    val averageRating: Double,
    val totalReviews: Int,
    val availableNow: Boolean,
    // Already rounded to the hundred metres by the engine, so zero is a value
    // that happens (docs/decisions.md, 2026-10-09).
    val distanceM: Int,
    // Null when the point cannot be read: the card stays in the list with its
    // distance and its rate, and the map simply has no pin for it.
    val basePoint: Coordinate?,
)

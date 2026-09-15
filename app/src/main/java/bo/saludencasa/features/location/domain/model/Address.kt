package bo.saludencasa.features.location.domain.model

import bo.saludencasa.core.vo.Coordinate

data class Address(
    val id: String,
    val alias: String,
    val addressText: String,
    val reference: String?,
    val city: String,
    val coordinate: Coordinate,
    val isPrimary: Boolean,
)

// What the geocoder can say about a point, and what a search for a written
// address gives back. Both directions produce the same three pieces, so they
// share one model instead of one per direction.
data class Place(
    val coordinate: Coordinate,
    val addressText: String,
    val city: String,
)

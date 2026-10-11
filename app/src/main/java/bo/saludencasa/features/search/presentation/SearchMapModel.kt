package bo.saludencasa.features.search.presentation

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.search.domain.model.NearbyProfessional
import bo.saludencasa.features.search.domain.model.SearchRadius

// What the map needs, with no type from the Maps SDK in it. That is what lets
// the screen take the map as a slot and keeps the preview and the Robolectric
// test away from a library that needs a key and Play Services to draw anything
// (the AddressScreen precedent, HU-05).
internal data class SearchMapModel(
    val origin: Coordinate,
    val radius: SearchRadius,
    val professionals: List<NearbyProfessional>,
    val searching: Boolean,
    val selectedId: String?,
)

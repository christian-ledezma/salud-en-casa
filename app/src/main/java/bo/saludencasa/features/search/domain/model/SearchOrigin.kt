package bo.saludencasa.features.search.domain.model

import bo.saludencasa.core.vo.Coordinate

// RN-02 and RF-06.1: the patient searches around a registered address of their
// own. The alias travels so the screen can say which one it is measuring from.
data class SearchOrigin(
    val addressId: String,
    val alias: String,
    val addressText: String,
    val coordinate: Coordinate,
)

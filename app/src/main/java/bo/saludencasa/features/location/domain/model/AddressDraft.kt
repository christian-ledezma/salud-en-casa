package bo.saludencasa.features.location.domain.model

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.domain.vo.AddressAlias
import bo.saludencasa.features.location.domain.vo.AddressReference
import bo.saludencasa.features.location.domain.vo.AddressText
import bo.saludencasa.features.location.domain.vo.CityName

// What the screen holds: text as the person typed it, and the point the marker
// is sitting on, which is absent until they place it.
data class AddressDraft(
    val id: String?,
    val alias: String,
    val addressText: String,
    val reference: String,
    val city: String,
    val coordinate: Coordinate?,
)

// What survives validation and reaches the repository.
data class AddressUpdate(
    val id: String?,
    val alias: AddressAlias,
    val addressText: AddressText,
    val reference: AddressReference?,
    val city: CityName,
    val coordinate: Coordinate,
)

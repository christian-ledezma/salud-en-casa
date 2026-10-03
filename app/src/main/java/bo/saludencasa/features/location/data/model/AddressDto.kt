package bo.saludencasa.features.location.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// my_addresses, the view that projects the stored point as two numbers. Reading
// public.addresses directly would hand the client the hexadecimal encoding
// PostGIS keeps on disk.
@Serializable
data class AddressDto(
    val id: String,
    val alias: String,
    @SerialName("address_text") val addressText: String,
    val reference: String? = null,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("is_primary") val isPrimary: Boolean = false,
    @SerialName("is_professional_base") val isProfessionalBase: Boolean = false,
)

// What travels towards public.addresses. The point leaves as the text PostGIS
// parses, because there is no JSON shape for a geography value.
@Serializable
data class AddressRow(
    @SerialName("profile_id") val profileId: String,
    val alias: String,
    @SerialName("address_text") val addressText: String,
    // No default value, so an emptied reference leaves as an explicit null and
    // clears the column instead of being omitted from the request.
    val reference: String?,
    val city: String,
    val location: String,
)

// The insert asks the server for the identifier alone. Asking for the whole row
// would bring back location as the hexadecimal encoding, which this side has no
// reason to decode.
@Serializable
data class InsertedAddressDto(
    val id: String,
)

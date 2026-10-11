package bo.saludencasa.features.search.data.model

import bo.saludencasa.core.network.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

// No default values: docs/decisions.md, 2026-09-16, a field equal to its
// default never leaves the client.
@Serializable
data class NearbySearchParams(
    @SerialName("p_latitude") val latitude: Double,
    @SerialName("p_longitude") val longitude: Double,
    @SerialName("p_radius_km") val radiusKm: Double,
    @SerialName("p_service_type_id") val serviceTypeId: String?,
    @SerialName("p_available_now") val availableNow: Boolean?,
    @SerialName("p_limit") val limit: Int,
    @SerialName("p_offset") val offset: Int,
)

@Serializable
data class NearbyProfessionalDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("professional_type") val professionalType: String? = null,
    val specialty: String? = null,
    @SerialName("base_rate_bob")
    @Serializable(with = BigDecimalSerializer::class)
    val baseRateBob: BigDecimal? = null,
    @SerialName("average_rating") val averageRating: Double = 0.0,
    @SerialName("total_reviews") val totalReviews: Int = 0,
    @SerialName("available_now") val availableNow: Boolean = false,
    @SerialName("distance_m") val distanceM: Double = 0.0,
    @SerialName("base_latitude") val baseLatitude: Double? = null,
    @SerialName("base_longitude") val baseLongitude: Double? = null,
)

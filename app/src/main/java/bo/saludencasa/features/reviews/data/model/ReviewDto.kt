package bo.saludencasa.features.reviews.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: String,
    val rating: Int = 0,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String,
)

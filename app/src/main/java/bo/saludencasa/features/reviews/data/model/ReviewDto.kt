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

// A direct insert under reviews_insert_participant: the engine seals created_at and
// keeps the rating visible, so neither travels (docs/decisions.md, 2026-11-26, HU-29).
@Serializable
data class NewReviewRow(
    @SerialName("service_id") val serviceId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("recipient_id") val recipientId: String,
    val rating: Int,
    val comment: String? = null,
)

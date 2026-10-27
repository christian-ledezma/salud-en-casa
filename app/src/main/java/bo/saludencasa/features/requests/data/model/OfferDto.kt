package bo.saludencasa.features.requests.data.model

import bo.saludencasa.core.network.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

// The row inserted into request_offers. The amount rides through
// BigDecimalSerializer, which writes the numeric as its plain text so the stored
// scale is not lost through a double. status, expires_at and created_at keep
// their defaults, so they are not sent; parent_offer_id is null for a first
// offer.
@Serializable
data class NewOfferRow(
    @SerialName("request_id") val requestId: String,
    @SerialName("professional_id") val professionalId: String,
    val issuer: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("amount_bob") val amountBob: BigDecimal,
    val message: String,
)

// The single named argument of my_request_offers and inbox_request.
@Serializable
data class RequestIdParam(
    @SerialName("p_request_id") val requestId: String,
)

// A row of my_request_offers. The professional's public fields come from
// professional_directory; the amount and rating ride through BigDecimalSerializer.
@Serializable
data class RequestOfferDto(
    val id: String,
    @SerialName("professional_id") val professionalId: String,
    @SerialName("professional_name") val professionalName: String? = null,
    @SerialName("professional_photo_url") val professionalPhotoUrl: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("professional_rating") val professionalRating: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("amount_bob") val amountBob: BigDecimal,
    val message: String? = null,
    val issuer: String,
    @SerialName("created_at") val createdAt: String,
)

// A row of inbox_request: the inbox projection for one request, plus whether the
// caller already offered. The exact location, address and patient id are not here.
@Serializable
data class OfferableRequestDto(
    val id: String,
    @SerialName("service_type_name") val serviceTypeName: String,
    val city: String,
    val description: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("suggested_budget_bob") val suggestedBudgetBob: BigDecimal? = null,
    @SerialName("distance_m") val distanceM: Double = 0.0,
    val status: String,
    val modality: String,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("already_offered") val alreadyOffered: Boolean = false,
)

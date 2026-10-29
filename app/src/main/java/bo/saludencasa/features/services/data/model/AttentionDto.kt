package bo.saludencasa.features.services.data.model

import bo.saludencasa.core.network.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

// The single named argument of my_attention.
@Serializable
data class AttentionRequestIdParam(
    @SerialName("p_request_id") val requestId: String,
)

// A row of my_attention: the service, the frozen amount and the counterpart
// relative to the caller. The amount rides through BigDecimalSerializer so the
// stored scale is not lost through a double.
@Serializable
data class AttentionDto(
    @SerialName("service_id") val serviceId: String,
    @SerialName("request_id") val requestId: String,
    val status: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("final_amount_bob") val finalAmountBob: BigDecimal,
    @SerialName("service_type_name") val serviceTypeName: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("counterpart_id") val counterpartId: String,
    @SerialName("counterpart_name") val counterpartName: String,
    @SerialName("counterpart_photo_url") val counterpartPhotoUrl: String? = null,
    @SerialName("i_am_patient") val iAmPatient: Boolean,
)

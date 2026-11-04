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

@Serializable
data class MyAttentionsParams(
    @SerialName("p_role") val role: String,
    @SerialName("p_limit") val limit: Int,
    @SerialName("p_offset") val offset: Int,
)

@Serializable
data class ActiveAttentionParams(
    @SerialName("p_role") val role: String,
)

// A row of my_attentions and of my_active_attention.
@Serializable
data class AttentionSummaryDto(
    @SerialName("request_id") val requestId: String,
    val status: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("final_amount_bob") val finalAmountBob: BigDecimal,
    @SerialName("service_type_name") val serviceTypeName: String,
    @SerialName("counterpart_name") val counterpartName: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
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
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    val description: String? = null,
    val city: String,
    @SerialName("address_text") val addressText: String? = null,
    val reference: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class ServiceStatusRow(
    val status: String,
)

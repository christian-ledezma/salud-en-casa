package bo.saludencasa.features.requests.data.model

import bo.saludencasa.core.network.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

// The named arguments of create_immediate_request. The budget travels as text
// for the reason the function documents: a numeric argument would be rounded
// through a double on the way in. A field equal to its default never leaves the
// client, so these carry no defaults.
@Serializable
data class CreateImmediateRequestParams(
    @SerialName("p_address_id") val addressId: String,
    @SerialName("p_service_type_id") val serviceTypeId: String,
    @SerialName("p_description") val description: String?,
    @SerialName("p_suggested_budget_bob") val suggestedBudgetBob: String?,
)

// The budget travels as text for the same reason as above.
@Serializable
data class CreateScheduledRequestParams(
    @SerialName("p_professional_id") val professionalId: String,
    @SerialName("p_address_id") val addressId: String,
    @SerialName("p_service_type_id") val serviceTypeId: String,
    @SerialName("p_scheduled_at") val scheduledAt: String,
    @SerialName("p_description") val description: String?,
    @SerialName("p_suggested_budget_bob") val suggestedBudgetBob: String?,
)

@Serializable
data class BookableTimesParams(
    @SerialName("p_professional_id") val professionalId: String,
    @SerialName("p_service_type_id") val serviceTypeId: String,
)

@Serializable
data class BookableStartDto(
    @SerialName("start_at") val startAt: String,
    @SerialName("local_date") val localDate: String,
    @SerialName("local_time") val localTime: String,
)

// A row of my_service_requests. The budget and the timestamps arrive as text and
// are parsed in the mapper: PostGIS hands numbers back as plain strings here,
// and PostgREST writes the offset as +00:00, which Instant.parse does not take.
@Serializable
data class ServiceRequestDto(
    val id: String,
    @SerialName("service_type_name") val serviceTypeName: String,
    @SerialName("address_text") val addressText: String,
    val city: String,
    val modality: String,
    val status: String,
    val description: String? = null,
    // PostgREST returns a numeric column as a JSON number (80.00, unquoted), so
    // this rides through BigDecimalSerializer like InboxRequestDto, not as a
    // String: a plain String? fails to decode a request that carries a budget.
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("suggested_budget_bob") val suggestedBudgetBob: BigDecimal? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("professional_name") val professionalName: String? = null,
    @SerialName("professional_id") val professionalId: String? = null,
)

// The named arguments of request_inbox. A field equal to its default never
// leaves the client, so these carry no defaults.
@Serializable
data class RequestInboxParams(
    @SerialName("p_limit") val limit: Int,
    @SerialName("p_offset") val offset: Int,
)

// A row of request_inbox. distance_m is a double, as the engine rounds and
// returns it; the budget rides through BigDecimalSerializer, which reads the
// numeric literal whether PostgREST quotes it or not. The exact location, the
// address text and the patient's id are not here: the function never returns
// them.
@Serializable
data class InboxRequestDto(
    val id: String,
    @SerialName("service_type_name") val serviceTypeName: String,
    val city: String,
    val description: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("suggested_budget_bob") val suggestedBudgetBob: BigDecimal? = null,
    @SerialName("distance_m") val distanceM: Double = 0.0,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    val modality: String,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
)

package bo.saludencasa.features.requests.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("suggested_budget_bob") val suggestedBudgetBob: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

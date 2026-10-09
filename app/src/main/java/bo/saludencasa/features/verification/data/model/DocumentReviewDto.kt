package bo.saludencasa.features.verification.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PendingReviewSubjectDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    @SerialName("pending_count") val pendingCount: Int,
    @SerialName("oldest_pending_at") val oldestPendingAt: String,
)

@Serializable
data class DocumentReviewSubjectDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    @SerialName("held_roles") val heldRoles: List<String>,
    @SerialName("professional_type") val professionalType: String? = null,
    @SerialName("professional_verification_status") val professionalStatus: String? = null,
)

// Parameter names carry the p_ prefix the function declares. No defaults: this
// travels through install(Postgrest), which does not receive encodeDefaults.
@Serializable
data class ApproveProfessionalParams(
    @SerialName("p_profile_id") val profileId: String,
)

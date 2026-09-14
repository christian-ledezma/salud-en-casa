package bo.saludencasa.features.profile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRoleDto(
    val role: String? = null,
)

@Serializable
data class AssignRoleParams(
    @SerialName("p_role") val role: String,
)

package bo.saludencasa.features.profile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The shape of the my_roles view: the active role and the held ones in one row.
@Serializable
data class ProfileRolesDto(
    @SerialName("active_role") val activeRole: String? = null,
    @SerialName("held_roles") val heldRoles: List<String> = emptyList(),
)

@Serializable
data class AddRoleParams(
    @SerialName("p_role") val role: String,
)

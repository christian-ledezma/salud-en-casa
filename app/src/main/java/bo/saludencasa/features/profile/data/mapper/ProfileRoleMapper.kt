package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.features.profile.data.model.ProfileRoleDto
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole

internal fun ProfileRoleDto.toRoleResult(): RoleResult =
    when (role) {
        null -> RoleResult.Unassigned
        else -> role.toUserRole()?.let(RoleResult::Assigned) ?: RoleResult.Failure(ProfileError.Unexpected)
    }

internal fun String.toUserRole(): UserRole? = UserRole.entries.firstOrNull { it.name == this }

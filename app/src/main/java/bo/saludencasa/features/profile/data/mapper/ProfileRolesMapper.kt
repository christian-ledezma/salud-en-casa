package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.features.profile.data.model.ProfileRolesDto
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileRoles
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole

internal fun ProfileRolesDto.toRoleResult(): RoleResult {
    // A value this build does not know is a failure, never a role that is
    // missing. Dropping it would put someone who already chose in front of the
    // question again (docs/decisions.md, 2026-09-13).
    val held = heldRoles.map { it.toUserRole() ?: return unreadable() }.toSet()
    val active = activeRole?.let { it.toUserRole() ?: return unreadable() }

    return ProfileRoles.create(held, active).fold(
        onSuccess = RoleResult::Loaded,
        onFailure = { unreadable() },
    )
}

// The composite foreign key makes an active role the person does not hold
// impossible to store, so reading one back means the reply is not a role state
// this build can trust.
private fun unreadable(): RoleResult = RoleResult.Failure(ProfileError.Unexpected)

internal fun String.toUserRole(): UserRole? = UserRole.entries.firstOrNull { it.name == this }

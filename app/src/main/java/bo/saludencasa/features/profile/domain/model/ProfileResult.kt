package bo.saludencasa.features.profile.domain.model

sealed interface RoleResult {
    data class Assigned(
        val role: UserRole,
    ) : RoleResult

    data object Unassigned : RoleResult

    data class Failure(
        val error: ProfileError,
    ) : RoleResult
}

sealed interface ChooseRoleResult {
    data class Success(
        val role: UserRole,
    ) : ChooseRoleResult

    data class Failure(
        val error: ProfileError,
    ) : ChooseRoleResult
}

sealed interface ProfileResult {
    data class Success(
        val profile: UserProfile,
    ) : ProfileResult

    data class Failure(
        val error: ProfileError,
    ) : ProfileResult
}

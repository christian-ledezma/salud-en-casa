package bo.saludencasa.features.profile.domain.model

// There is no Unassigned case: a person with no roles is Loaded with an empty
// set. Carrying both would be two ways to say one thing, and a mapper could
// then produce either for the same reply.
sealed interface RoleResult {
    data class Loaded(
        val roles: ProfileRoles,
    ) : RoleResult

    data class Failure(
        val error: ProfileError,
    ) : RoleResult
}

// Success carries what the database recorded, not what the client asked for
// (docs/decisions.md, 2026-09-13). add_my_role leaves the new role active, so
// the role it returns is also the active one from here on.
sealed interface AddRoleResult {
    data class Success(
        val role: UserRole,
    ) : AddRoleResult

    data class Failure(
        val error: ProfileError,
    ) : AddRoleResult
}

sealed interface SwitchRoleResult {
    data class Success(
        val activeRole: UserRole,
    ) : SwitchRoleResult

    data class Failure(
        val error: ProfileError,
    ) : SwitchRoleResult
}

sealed interface ProfileResult {
    data class Success(
        val profile: UserProfile,
    ) : ProfileResult

    data class Failure(
        val error: ProfileError,
    ) : ProfileResult
}

sealed interface AvailabilityResult {
    data class Success(
        val availableNow: Boolean,
    ) : AvailabilityResult

    data class Failure(
        val error: ProfileError,
    ) : AvailabilityResult
}

// NotPublished is not an error and not an empty read either: it is INV-07
// holding. professional_directory only carries professionals whose
// verification is approved and whose profile is active, so a professional who
// is neither is simply not there for anyone to see.
sealed interface PublicProfileResult {
    data class Success(
        val profile: PublicProfile,
    ) : PublicProfileResult

    data object NotPublished : PublicProfileResult

    data class Failure(
        val error: ProfileError,
    ) : PublicProfileResult
}

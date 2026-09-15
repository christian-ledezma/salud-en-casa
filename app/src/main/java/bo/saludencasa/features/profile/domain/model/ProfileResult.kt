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

package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.domain.model.AddRoleResult
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

// Replaces ChooseRoleUseCase. The rule it enforces is no longer "there is never
// a second role" but "there is never the same role twice" (docs/decisions.md,
// 2026-10-01). Reading before writing skips a request the database was going to
// reject; the guarantee stays with profile_roles' primary key.
class AddRoleUseCase(
    private val profileRepository: IProfileRepository,
) {
    suspend operator fun invoke(role: AssignableRole): AddRoleResult =
        when (val current = profileRepository.getRoles()) {
            is RoleResult.Loaded -> {
                if (current.roles.has(role.role)) {
                    AddRoleResult.Failure(ProfileError.RoleAlreadyHeld)
                } else {
                    profileRepository.addRole(role)
                }
            }

            is RoleResult.Failure -> {
                AddRoleResult.Failure(current.error)
            }
        }
}

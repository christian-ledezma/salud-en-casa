package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

// The read is what turns "that role is not yours" into an error the screen can
// explain. Without it the composite foreign key still refuses the write, but it
// arrives as a constraint violation the client can only report as unexpected.
class SwitchActiveRoleUseCase(
    private val profileRepository: IProfileRepository,
) {
    suspend operator fun invoke(role: UserRole): SwitchRoleResult =
        when (val current = profileRepository.getRoles()) {
            is RoleResult.Loaded -> {
                if (current.roles.has(role)) {
                    profileRepository.setActiveRole(role)
                } else {
                    SwitchRoleResult.Failure(ProfileError.RoleNotHeld)
                }
            }

            is RoleResult.Failure -> {
                SwitchRoleResult.Failure(current.error)
            }
        }
}

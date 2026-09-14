package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ChooseRoleResult
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

class ChooseRoleUseCase(
    private val profileRepository: IProfileRepository,
) {
    suspend operator fun invoke(role: AssignableRole): ChooseRoleResult =
        when (val assigned = profileRepository.getRole()) {
            is RoleResult.Assigned -> ChooseRoleResult.Failure(ProfileError.RoleAlreadyAssigned)
            RoleResult.Unassigned -> profileRepository.assignRole(role)
            is RoleResult.Failure -> ChooseRoleResult.Failure(assigned.error)
        }
}

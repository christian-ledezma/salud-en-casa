package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

class GetRolesUseCase(
    private val profileRepository: IProfileRepository,
) {
    suspend operator fun invoke(): RoleResult = profileRepository.getRoles()
}

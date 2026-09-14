package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

class GetProfileUseCase(
    private val profileRepository: IProfileRepository,
) {
    suspend operator fun invoke(): ProfileResult = profileRepository.getProfile()
}

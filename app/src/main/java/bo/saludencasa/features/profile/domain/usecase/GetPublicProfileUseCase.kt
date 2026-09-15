package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

class GetPublicProfileUseCase(
    private val profileRepository: IProfileRepository,
) {
    suspend operator fun invoke(professionalId: String): PublicProfileResult =
        profileRepository.getPublicProfile(professionalId)
}

package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.domain.model.AvailabilityResult
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

class SetAvailabilityUseCase(
    private val profileRepository: IProfileRepository,
) {
    suspend operator fun invoke(availableNow: Boolean): AvailabilityResult =
        profileRepository.setAvailableNow(availableNow)
}

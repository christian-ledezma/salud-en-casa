package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.SetProfessionalBaseResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository

class SetProfessionalBaseAddressUseCase(
    private val repository: IAddressRepository,
) {
    suspend operator fun invoke(id: String): SetProfessionalBaseResult = repository.setProfessionalBaseAddress(id)
}

package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.SetPrimaryAddressResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository

class SetPrimaryAddressUseCase(
    private val repository: IAddressRepository,
) {
    suspend operator fun invoke(id: String): SetPrimaryAddressResult = repository.setPrimaryAddress(id)
}

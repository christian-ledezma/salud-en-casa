package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.DeleteAddressResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository

class DeleteAddressUseCase(
    private val repository: IAddressRepository,
) {
    suspend operator fun invoke(id: String): DeleteAddressResult = repository.deleteAddress(id)
}

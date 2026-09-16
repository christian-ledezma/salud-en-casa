package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.MyAddressResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository

class GetAddressUseCase(
    private val repository: IAddressRepository,
) {
    suspend operator fun invoke(id: String): MyAddressResult = repository.getAddress(id)
}

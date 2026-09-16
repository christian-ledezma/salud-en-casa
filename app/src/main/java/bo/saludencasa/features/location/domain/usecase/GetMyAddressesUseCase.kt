package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository

class GetMyAddressesUseCase(
    private val repository: IAddressRepository,
) {
    suspend operator fun invoke(): AddressListResult = repository.getMyAddresses()
}

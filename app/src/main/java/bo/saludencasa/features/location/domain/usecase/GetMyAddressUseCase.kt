package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.MyAddressResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository

class GetMyAddressUseCase(
    private val repository: IAddressRepository,
) {
    suspend operator fun invoke(): MyAddressResult = repository.getMyPrimaryAddress()
}

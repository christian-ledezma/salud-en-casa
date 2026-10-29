package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.OfferThreadResult
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

class GetOfferThreadUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(
        requestId: String,
        professionalId: String?,
    ): OfferThreadResult = repository.getOfferThread(requestId, professionalId)
}

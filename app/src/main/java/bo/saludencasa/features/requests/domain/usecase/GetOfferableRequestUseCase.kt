package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

class GetOfferableRequestUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(requestId: String): OfferableRequestResult = repository.getOfferableRequest(requestId)
}

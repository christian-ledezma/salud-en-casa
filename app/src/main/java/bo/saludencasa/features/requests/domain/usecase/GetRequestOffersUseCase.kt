package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.RequestOffersResult
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

class GetRequestOffersUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(requestId: String): RequestOffersResult = repository.getOffersForRequest(requestId)
}

package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.OfferThreadsResult
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

class GetOfferThreadsUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(requestId: String): OfferThreadsResult = repository.getOfferThreads(requestId)
}

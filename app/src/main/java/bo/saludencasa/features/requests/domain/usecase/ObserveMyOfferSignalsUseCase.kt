package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.repository.IOfferRepository
import kotlinx.coroutines.flow.Flow

class ObserveMyOfferSignalsUseCase(
    private val repository: IOfferRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.observeMyOfferSignals()
}

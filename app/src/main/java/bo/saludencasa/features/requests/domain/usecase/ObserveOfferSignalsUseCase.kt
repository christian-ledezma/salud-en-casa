package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.repository.IOfferRepository
import kotlinx.coroutines.flow.Flow

class ObserveOfferSignalsUseCase(
    private val repository: IOfferRepository,
) {
    // No default: which side is listening is a decision of the caller, and each
    // side has its own topic.
    operator fun invoke(asProfessional: Boolean): Flow<Unit> = repository.observeOfferSignals(asProfessional)
}

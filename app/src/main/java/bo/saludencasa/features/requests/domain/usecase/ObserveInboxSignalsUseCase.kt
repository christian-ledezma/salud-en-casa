package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.repository.IRequestRepository
import kotlinx.coroutines.flow.Flow

class ObserveInboxSignalsUseCase(
    private val repository: IRequestRepository,
) {
    // A stream, so no suspend: it emits once per "the inbox may have changed"
    // signal for as long as it is collected.
    operator fun invoke(): Flow<Unit> = repository.inboxSignals()
}

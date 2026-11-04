package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.repository.IServiceRepository
import kotlinx.coroutines.flow.Flow

class ObserveAttentionUseCase(
    private val repository: IServiceRepository,
) {
    operator fun invoke(requestId: String): Flow<Unit> = repository.attentionSignals(requestId)
}

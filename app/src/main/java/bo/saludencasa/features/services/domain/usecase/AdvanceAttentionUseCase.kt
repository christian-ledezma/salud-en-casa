package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.model.AdvanceAttentionResult
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.repository.IServiceRepository

class AdvanceAttentionUseCase(
    private val repository: IServiceRepository,
) {
    suspend operator fun invoke(
        requestId: String,
        step: AttentionStep,
    ): AdvanceAttentionResult = repository.advance(requestId, step)
}

package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.model.ActiveAttentionResult
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.repository.IServiceRepository

class GetActiveAttentionUseCase(
    private val repository: IServiceRepository,
) {
    suspend operator fun invoke(side: AttentionSide): ActiveAttentionResult = repository.getActiveAttention(side)
}

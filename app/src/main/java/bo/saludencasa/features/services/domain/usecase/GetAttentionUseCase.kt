package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.repository.IServiceRepository

class GetAttentionUseCase(
    private val repository: IServiceRepository,
) {
    suspend operator fun invoke(requestId: String): AttentionResult = repository.getAttention(requestId)
}

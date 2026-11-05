package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.CancelAttentionResult
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import bo.saludencasa.features.services.domain.vo.CancellationReason

class CancelAttentionUseCase(
    private val repository: IServiceRepository,
) {
    suspend operator fun invoke(
        requestId: String,
        rawReason: String,
    ): CancelAttentionResult {
        val reason =
            CancellationReason.create(rawReason).getOrElse {
                return CancelAttentionResult.Failure(
                    if (rawReason.isBlank()) CancelAttentionError.ReasonEmpty else CancelAttentionError.ReasonTooLong,
                )
            }
        return repository.cancel(requestId, reason)
    }
}

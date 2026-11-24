package bo.saludencasa.features.disputes.domain.usecase

import bo.saludencasa.features.disputes.domain.model.DisputeResolution
import bo.saludencasa.features.disputes.domain.model.ResolveDisputeResult
import bo.saludencasa.features.disputes.domain.repository.IDisputeRepository

class ResolveDisputeUseCase(
    private val repository: IDisputeRepository,
) {
    suspend operator fun invoke(
        paymentId: String,
        resolution: DisputeResolution,
    ): ResolveDisputeResult = repository.resolve(paymentId, resolution)
}

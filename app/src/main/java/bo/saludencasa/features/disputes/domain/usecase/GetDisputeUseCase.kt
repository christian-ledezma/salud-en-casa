package bo.saludencasa.features.disputes.domain.usecase

import bo.saludencasa.features.disputes.domain.model.DisputeResult
import bo.saludencasa.features.disputes.domain.repository.IDisputeRepository

class GetDisputeUseCase(
    private val repository: IDisputeRepository,
) {
    suspend operator fun invoke(paymentId: String): DisputeResult = repository.getDispute(paymentId)
}

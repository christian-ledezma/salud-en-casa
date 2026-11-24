package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.model.AnswerPaymentResult
import bo.saludencasa.features.services.domain.repository.IServiceRepository

class AcceptReceiptUseCase(
    private val repository: IServiceRepository,
) {
    suspend operator fun invoke(serviceId: String): AnswerPaymentResult = repository.acceptReceipt(serviceId)
}

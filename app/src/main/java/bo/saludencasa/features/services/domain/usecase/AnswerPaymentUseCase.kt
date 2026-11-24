package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.model.AnswerPaymentResult
import bo.saludencasa.features.services.domain.model.PaymentAnswer
import bo.saludencasa.features.services.domain.repository.IServiceRepository

class AnswerPaymentUseCase(
    private val repository: IServiceRepository,
) {
    suspend operator fun invoke(
        serviceId: String,
        answer: PaymentAnswer,
    ): AnswerPaymentResult = repository.answerPayment(serviceId, answer)
}

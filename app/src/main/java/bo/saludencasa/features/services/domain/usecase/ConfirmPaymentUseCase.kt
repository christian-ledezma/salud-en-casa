package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.model.ConfirmPaymentError
import bo.saludencasa.features.services.domain.model.ConfirmPaymentResult
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.services.domain.repository.IServiceRepository

class ConfirmPaymentUseCase(
    private val repository: IServiceRepository,
) {
    suspend operator fun invoke(
        serviceId: String,
        method: PaymentMethod,
    ): ConfirmPaymentResult {
        if (method !in PaymentMethod.patientChoices) {
            return ConfirmPaymentResult.Failure(ConfirmPaymentError.MethodNotAllowed)
        }
        return repository.confirmPayment(serviceId, method)
    }
}

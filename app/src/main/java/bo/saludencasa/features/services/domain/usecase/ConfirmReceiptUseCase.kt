package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.model.ConfirmPaymentError
import bo.saludencasa.features.services.domain.model.ConfirmPaymentResult
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.services.domain.repository.IServiceRepository

class ConfirmReceiptUseCase(
    private val repository: IServiceRepository,
) {
    suspend operator fun invoke(
        serviceId: String,
        method: PaymentMethod,
    ): ConfirmPaymentResult {
        if (method !in PaymentMethod.declarable) {
            return ConfirmPaymentResult.Failure(ConfirmPaymentError.MethodNotAllowed)
        }
        return repository.confirmReceipt(serviceId, method)
    }
}

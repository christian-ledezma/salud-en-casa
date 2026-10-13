package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository

class CancelRequestUseCase(
    private val repository: IRequestRepository,
) {
    suspend operator fun invoke(requestId: String): CancelRequestResult = repository.cancel(requestId)
}

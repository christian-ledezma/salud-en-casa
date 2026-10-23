package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.ActiveRequestResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository

class GetActiveImmediateRequestUseCase(
    private val repository: IRequestRepository,
) {
    suspend operator fun invoke(): ActiveRequestResult = repository.getActiveImmediate()
}

package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.BookableTimesResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository

class GetBookableTimesUseCase(
    private val repository: IRequestRepository,
) {
    suspend operator fun invoke(
        professionalId: String,
        serviceTypeId: String,
    ): BookableTimesResult = repository.getBookableTimes(professionalId, serviceTypeId)
}

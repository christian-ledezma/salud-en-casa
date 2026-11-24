package bo.saludencasa.features.disputes.domain.usecase

import bo.saludencasa.features.disputes.domain.model.DisputesPageResult
import bo.saludencasa.features.disputes.domain.model.DisputesResult
import bo.saludencasa.features.disputes.domain.repository.IDisputeRepository

class GetDisputesUseCase(
    private val repository: IDisputeRepository,
) {
    // RNF-08: one bounded page; a short one is the end of the list.
    suspend operator fun invoke(offset: Int): DisputesPageResult =
        when (val result = repository.getDisputes(offset = offset, limit = PAGE_SIZE)) {
            is DisputesResult.Loaded -> {
                DisputesPageResult.Loaded(result.disputes, endReached = result.disputes.size < PAGE_SIZE)
            }

            is DisputesResult.Failure -> {
                DisputesPageResult.Failure(result.error)
            }
        }

    companion object {
        const val PAGE_SIZE = 20
    }
}

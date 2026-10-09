package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.domain.model.PendingReviewPageResult
import bo.saludencasa.features.verification.domain.model.PendingReviewQuery
import bo.saludencasa.features.verification.domain.model.PendingReviewSubjectsResult
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository

class GetPendingReviewSubjectsUseCase(
    private val repository: IDocumentReviewRepository,
) {
    suspend operator fun invoke(
        query: PendingReviewQuery,
        offset: Int,
    ): PendingReviewPageResult =
        when (val result = repository.findPendingSubjects(query, offset, PAGE_SIZE)) {
            is PendingReviewSubjectsResult.Loaded -> {
                PendingReviewPageResult.Loaded(result.subjects, endReached = result.subjects.size < PAGE_SIZE)
            }

            is PendingReviewSubjectsResult.Failure -> {
                PendingReviewPageResult.Failure(result.error)
            }
        }

    companion object {
        const val PAGE_SIZE: Int = 20
    }
}

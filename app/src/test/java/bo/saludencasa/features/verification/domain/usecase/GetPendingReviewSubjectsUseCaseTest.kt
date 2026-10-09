package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.FakeDocumentReviewRepository
import bo.saludencasa.features.verification.domain.model.PendingReviewPageResult
import bo.saludencasa.features.verification.domain.model.PendingReviewQuery
import bo.saludencasa.features.verification.domain.model.PendingReviewSubjectsResult
import bo.saludencasa.features.verification.pendingSubject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPendingReviewSubjectsUseCaseTest {
    private val repository = FakeDocumentReviewRepository()
    private val getPending = GetPendingReviewSubjectsUseCase(repository)

    private fun pageOf(size: Int) = PendingReviewSubjectsResult.Loaded(List(size) { pendingSubject(it) })

    // A short page is the only signal the queue has that nothing follows.
    @Test
    fun aShortPageMarksTheEndOfTheQueue() =
        runTest {
            repository.pending = { _, _ -> pageOf(GetPendingReviewSubjectsUseCase.PAGE_SIZE - 1) }

            val result = getPending(PendingReviewQuery(), offset = 0) as PendingReviewPageResult.Loaded

            assertEquals(true, result.endReached)
        }

    // The exact boundary: a full page may be followed by another, so the screen
    // has to ask once more instead of stopping on a queue of exactly one page.
    @Test
    fun aFullPageLeavesTheQueueOpen() =
        runTest {
            repository.pending = { _, _ -> pageOf(GetPendingReviewSubjectsUseCase.PAGE_SIZE) }

            val result = getPending(PendingReviewQuery(), offset = 0) as PendingReviewPageResult.Loaded

            assertEquals(false, result.endReached)
        }
}

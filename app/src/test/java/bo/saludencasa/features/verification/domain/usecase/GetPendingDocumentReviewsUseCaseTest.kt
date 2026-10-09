package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.FakeDocumentReviewRepository
import bo.saludencasa.features.verification.domain.model.PendingReviewPageResult
import bo.saludencasa.features.verification.domain.model.PendingReviewsResult
import bo.saludencasa.features.verification.pendingReview
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPendingDocumentReviewsUseCaseTest {
    private val repository = FakeDocumentReviewRepository()
    private val getPending = GetPendingDocumentReviewsUseCase(repository)

    private fun pageOf(size: Int) = PendingReviewsResult.Loaded(List(size) { pendingReview(it) })

    // A short page is the only signal the queue has that nothing follows.
    @Test
    fun aShortPageMarksTheEndOfTheQueue() =
        runTest {
            repository.pending = { _, _ -> pageOf(GetPendingDocumentReviewsUseCase.PAGE_SIZE - 1) }

            val result = getPending(offset = 0) as PendingReviewPageResult.Loaded

            assertEquals(true, result.endReached)
        }

    // The exact boundary: a full page may be followed by another, so the screen
    // has to ask once more instead of stopping on a queue of exactly one page.
    @Test
    fun aFullPageLeavesTheQueueOpen() =
        runTest {
            repository.pending = { _, _ -> pageOf(GetPendingDocumentReviewsUseCase.PAGE_SIZE) }

            val result = getPending(offset = 0) as PendingReviewPageResult.Loaded

            assertEquals(false, result.endReached)
        }
}

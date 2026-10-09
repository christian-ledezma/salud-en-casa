@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.verification.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.verification.FakeDocumentReviewRepository
import bo.saludencasa.features.verification.domain.model.PendingReviewsResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.GetPendingDocumentReviewsUseCase
import bo.saludencasa.features.verification.pendingReview
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DocumentReviewQueueViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDocumentReviewRepository()
    private val pageSize = GetPendingDocumentReviewsUseCase.PAGE_SIZE

    private fun viewModel() = DocumentReviewQueueViewModel(GetPendingDocumentReviewsUseCase(repository))

    private fun fullPage(from: Int) = PendingReviewsResult.Loaded(List(pageSize) { pendingReview(from + it) })

    @Test
    fun anEmptyQueueRendersTheEmptyState() =
        runTest {
            val viewModel = viewModel()

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(DocumentReviewQueueUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun aFailedNextPageKeepsThePagesAlreadyLoaded() =
        runTest {
            repository.pending = { offset, _ ->
                if (offset == 0) fullPage(0) else PendingReviewsResult.Failure(VerificationError.NetworkUnavailable)
            }
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            val content = viewModel.uiState.value as DocumentReviewQueueUiState.Content
            assertEquals(pageSize, content.reviews.size)
            assertEquals(VerificationError.NetworkUnavailable, content.notice)
            assertEquals(false, content.loadingMore)
            assertEquals(false, content.endReached)
        }

    @Test
    fun doesNotRequestAFurtherPageOnceTheEndIsReached() =
        runTest {
            repository.pending = { _, _ -> PendingReviewsResult.Loaded(listOf(pendingReview(0))) }
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            assertEquals(1, repository.pendingRequests.size)
        }

    // A reopened document keeps its old creation date and slides toward the
    // front, so the next offset can hand over a row already listed. Two rows
    // with the same key crash a LazyColumn.
    @Test
    fun aDocumentSeenOnTwoPagesIsListedOnce() =
        runTest {
            repository.pending = { offset, _ ->
                if (offset ==
                    0
                ) {
                    fullPage(0)
                } else {
                    PendingReviewsResult.Loaded(listOf(pendingReview(pageSize - 1), pendingReview(99)))
                }
            }
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            val keys = (viewModel.uiState.value as DocumentReviewQueueUiState.Content).reviews.map { it.key }
            assertEquals(keys.distinct(), keys)
            assertEquals(pageSize + 1, keys.size)
        }

    @Test
    fun aFailedRefreshKeepsTheListOnScreen() =
        runTest {
            repository.pending = { _, _ -> fullPage(0) }
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()
            repository.pending = { _, _ -> PendingReviewsResult.Failure(VerificationError.NetworkUnavailable) }

            viewModel.refresh()
            advanceUntilIdle()

            val content = viewModel.uiState.value as DocumentReviewQueueUiState.Content
            assertEquals(pageSize, content.reviews.size)
            assertEquals(VerificationError.NetworkUnavailable, content.notice)
        }

    @Test
    fun aFailedFirstLoadShowsTheErrorAndNoList() =
        runTest {
            repository.pending = { _, _ -> PendingReviewsResult.Failure(VerificationError.NetworkUnavailable) }
            val viewModel = viewModel()

            viewModel.refresh()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is DocumentReviewQueueUiState.Failed)
        }

    // Coming back from the detail fires a refresh and, almost at once, the footer
    // asks for the next page. A late next page must not be stitched onto the
    // fresh first page, or the rows between them vanish from the list.
    @Test
    fun aRefreshDiscardsTheNextPageStillOnItsWay() =
        runTest {
            val nextPageArrives = CompletableDeferred<Unit>()
            repository.pending = { offset, _ ->
                if (offset > 0) nextPageArrives.await()
                fullPage(offset)
            }
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onReachedEnd()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()
            nextPageArrives.complete(Unit)
            advanceUntilIdle()

            val content = viewModel.uiState.value as DocumentReviewQueueUiState.Content
            assertEquals(pageSize, content.reviews.size)
            assertEquals(false, content.loadingMore)
        }
}

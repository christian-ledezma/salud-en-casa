@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.verification.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.verification.FakeDocumentReviewRepository
import bo.saludencasa.features.verification.domain.model.PendingReviewSubjectsResult
import bo.saludencasa.features.verification.domain.model.ReviewQueueOrder
import bo.saludencasa.features.verification.domain.model.ReviewRoleFilter
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.GetPendingReviewSubjectsUseCase
import bo.saludencasa.features.verification.pendingSubject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
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
    private val pageSize = GetPendingReviewSubjectsUseCase.PAGE_SIZE

    private fun viewModel() = DocumentReviewQueueViewModel(GetPendingReviewSubjectsUseCase(repository))

    private fun fullPage(from: Int) = PendingReviewSubjectsResult.Loaded(List(pageSize) { pendingSubject(from + it) })

    private fun DocumentReviewQueueViewModel.content() = uiState.value.list as QueueListState.Content

    @Test
    fun anEmptyQueueRendersTheEmptyState() =
        runTest {
            val viewModel = viewModel()

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(QueueListState.Empty, viewModel.uiState.value.list)
        }

    @Test
    fun aFailedNextPageKeepsThePagesAlreadyLoaded() =
        runTest {
            repository.pending = { _, offset ->
                if (offset ==
                    0
                ) {
                    fullPage(0)
                } else {
                    PendingReviewSubjectsResult.Failure(VerificationError.NetworkUnavailable)
                }
            }
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            val list = viewModel.content()
            assertEquals(pageSize, list.subjects.size)
            assertEquals(VerificationError.NetworkUnavailable, list.notice)
            assertEquals(false, list.loadingMore)
            assertEquals(false, list.endReached)
        }

    @Test
    fun doesNotRequestAFurtherPageOnceTheEndIsReached() =
        runTest {
            repository.pending = { _, _ -> PendingReviewSubjectsResult.Loaded(listOf(pendingSubject(0))) }
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            assertEquals(1, repository.pendingRequests.size)
        }

    // A person whose oldest pending document is reopened keeps its date and
    // slides toward the front, so the next offset can hand over a row already
    // listed. Two rows with the same key crash a LazyColumn.
    @Test
    fun aPersonSeenOnTwoPagesIsListedOnce() =
        runTest {
            repository.pending = { _, offset ->
                if (offset == 0) {
                    fullPage(0)
                } else {
                    PendingReviewSubjectsResult.Loaded(
                        listOf(pendingSubject(pageSize - 1), pendingSubject(99)),
                    )
                }
            }
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            val keys = viewModel.content().subjects.map { it.profileId }
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
            repository.pending = { _, _ -> PendingReviewSubjectsResult.Failure(VerificationError.NetworkUnavailable) }

            viewModel.refresh()
            advanceUntilIdle()

            val list = viewModel.content()
            assertEquals(pageSize, list.subjects.size)
            assertEquals(VerificationError.NetworkUnavailable, list.notice)
        }

    @Test
    fun aFailedFirstLoadShowsTheErrorAndNoList() =
        runTest {
            repository.pending = { _, _ -> PendingReviewSubjectsResult.Failure(VerificationError.NetworkUnavailable) }
            val viewModel = viewModel()

            viewModel.refresh()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.list is QueueListState.Failed)
        }

    // Coming back from the detail fires a refresh and, almost at once, the footer
    // asks for the next page. A late next page must not be stitched onto the
    // fresh first page, or the rows between them vanish from the list.
    @Test
    fun aRefreshDiscardsTheNextPageStillOnItsWay() =
        runTest {
            val nextPageArrives = CompletableDeferred<Unit>()
            repository.pending = { _, offset ->
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

            val list = viewModel.content()
            assertEquals(pageSize, list.subjects.size)
            assertEquals(false, list.loadingMore)
        }

    // One request per keystroke would spend a round trip on every prefix of the
    // word and show the answer to the last one anyway.
    @Test
    fun typingDoesNotQueryUntilTheKeystrokesStop() =
        runTest {
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()
            repository.pendingRequests.clear()

            viewModel.onSearchChanged("a")
            advanceTimeBy(DocumentReviewQueueViewModel.SEARCH_DEBOUNCE_MILLIS / 2)
            viewModel.onSearchChanged("an")
            advanceTimeBy(DocumentReviewQueueViewModel.SEARCH_DEBOUNCE_MILLIS / 2)
            viewModel.onSearchChanged("ana")
            assertTrue(repository.pendingRequests.isEmpty())

            advanceUntilIdle()

            assertEquals(1, repository.pendingRequests.size)
            assertEquals(
                "ana",
                repository.pendingRequests
                    .single()
                    .query.search,
            )
        }

    // The filter is applied by the engine, so a page asked without it comes back
    // with people the chips say are not there.
    @Test
    fun theNextPageCarriesTheActiveQuery() =
        runTest {
            repository.pending = { _, offset -> fullPage(offset) }
            val viewModel = viewModel()
            viewModel.onRoleChanged(ReviewRoleFilter.PROFESSIONAL)
            advanceUntilIdle()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            val lastRequest = repository.pendingRequests.last()
            assertEquals(pageSize, lastRequest.offset)
            assertEquals(ReviewRoleFilter.PROFESSIONAL, lastRequest.query.role)
        }

    // Keeping the offset after narrowing the criteria would skip the first page
    // of the new result and show its second one as if it were the first.
    @Test
    fun changingTheFilterStartsOverFromTheFirstPage() =
        runTest {
            repository.pending = { _, offset -> fullPage(offset) }
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onReachedEnd()
            advanceUntilIdle()

            viewModel.onOrderChanged(ReviewQueueOrder.NEWEST_FIRST)
            advanceUntilIdle()

            assertEquals(0, repository.pendingRequests.last().offset)
            assertEquals(pageSize, viewModel.content().subjects.size)
        }

    // Two taps in a row on the chips leave two requests in flight. If the first
    // one answers last the list fills with people the chips no longer describe.
    @Test
    fun aLateReplyToTheFormerCriteriaNeverLandsOnTheList() =
        runTest {
            val slowReply = CompletableDeferred<Unit>()
            repository.pending = { query, _ ->
                if (query.role == ReviewRoleFilter.PATIENT) {
                    slowReply.await()
                    PendingReviewSubjectsResult.Loaded(listOf(pendingSubject(1, fullName = "Paciente")))
                } else {
                    PendingReviewSubjectsResult.Loaded(listOf(pendingSubject(2, fullName = "Profesional")))
                }
            }
            val viewModel = viewModel()

            viewModel.onRoleChanged(ReviewRoleFilter.PATIENT)
            advanceUntilIdle()
            viewModel.onRoleChanged(ReviewRoleFilter.PROFESSIONAL)
            advanceUntilIdle()
            slowReply.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf("Profesional"), viewModel.content().subjects.map { it.fullName })
        }

    // The order is not a filter: it does not hide anybody, so clearing the
    // search and the role has no business resetting it.
    @Test
    fun clearingTheFiltersKeepsTheChosenOrder() =
        runTest {
            repository.pending = { _, _ -> fullPage(0) }
            val viewModel = viewModel()
            viewModel.onOrderChanged(ReviewQueueOrder.NEWEST_FIRST)
            advanceUntilIdle()
            viewModel.onRoleChanged(ReviewRoleFilter.PATIENT)
            advanceUntilIdle()

            viewModel.onClearFilters()
            advanceUntilIdle()

            val query = viewModel.uiState.value.query
            assertEquals(ReviewQueueOrder.NEWEST_FIRST, query.order)
            assertEquals(ReviewRoleFilter.ALL, query.role)
            assertEquals("", query.search)
        }
}

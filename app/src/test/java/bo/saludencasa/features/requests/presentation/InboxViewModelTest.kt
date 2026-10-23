@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.InboxResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.usecase.FindInboxRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetInboxUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveInboxSignalsUseCase
import bo.saludencasa.features.requests.inboxRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InboxViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeRequestRepository()

    private fun viewModel(highlightRequestId: String? = null): InboxViewModel =
        InboxViewModel(
            highlightRequestId,
            GetInboxUseCase(repository),
            FindInboxRequestUseCase(GetInboxUseCase(repository)),
            ObserveInboxSignalsUseCase(repository),
        )

    @Test
    fun anEmptyInboxIsTheEmptyState() =
        runTest {
            repository.inboxResult = InboxResult.Loaded(emptyList())

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(InboxUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun aFailedLoadIsReported() =
        runTest {
            repository.inboxResult = InboxResult.Failure(RequestError.NetworkUnavailable)

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(InboxUiState.Failed(RequestError.NetworkUnavailable), viewModel.uiState.value)
        }

    // The criterion: a request published in range appears without the
    // professional reloading. The broadcast signal stands in for that, and the
    // screen re-reads the first page and shows the new row.
    @Test
    fun aSignalReloadsTheInboxAndShowsTheNewRequest() =
        runTest {
            repository.inboxResult = InboxResult.Loaded(emptyList())

            val viewModel = viewModel()
            advanceUntilIdle()
            assertEquals(InboxUiState.Empty, viewModel.uiState.value)

            repository.inboxResult = InboxResult.Loaded(listOf(inboxRequest(id = "fresh")))
            repository.inboxSignals.emit(Unit)
            advanceUntilIdle()

            val content = viewModel.uiState.value as InboxUiState.Content
            assertEquals("fresh", content.requests.single().id)
            // One read on open, one read driven by the signal: no manual reload.
            assertEquals(2, repository.inboxPageRequests.count { it.first == 0 })
        }

    // A dropped background refetch must not blank a list the professional is
    // reading: the signal-driven failure is swallowed and the content stays.
    @Test
    fun aFailedSignalRefreshKeepsTheCurrentList() =
        runTest {
            repository.inboxResult = InboxResult.Loaded(listOf(inboxRequest(id = "r1")))

            val viewModel = viewModel()
            advanceUntilIdle()

            repository.inboxResult = InboxResult.Failure(RequestError.NetworkUnavailable)
            repository.inboxSignals.emit(Unit)
            advanceUntilIdle()

            val content = viewModel.uiState.value as InboxUiState.Content
            assertEquals("r1", content.requests.single().id)
        }

    // The second page appends without the first page's rows coming back, and a
    // short page ends the list.
    @Test
    fun aSecondPageAppendsAndEndsOnAShortPage() =
        runTest {
            val firstPage = List(GetInboxUseCase.PAGE_SIZE) { inboxRequest(id = "r$it") }
            repository.inboxResponder = { offset, _ ->
                if (offset == 0) {
                    InboxResult.Loaded(firstPage)
                } else {
                    InboxResult.Loaded(listOf(inboxRequest(id = "tail")))
                }
            }

            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onReachedEnd()
            advanceUntilIdle()

            val content = viewModel.uiState.value as InboxUiState.Content
            assertEquals(GetInboxUseCase.PAGE_SIZE + 1, content.requests.size)
            assertTrue(content.endReached)
        }

    // HU-20: tapping a notification opens the inbox on its request.
    @Test
    fun highlightsTheRowTheNotificationPointsAt() =
        runTest {
            repository.inboxResult = InboxResult.Loaded(listOf(inboxRequest(id = "a"), inboxRequest(id = "b")))

            val viewModel = viewModel(highlightRequestId = "b")
            advanceUntilIdle()

            assertEquals(InboxHighlight.Row("b"), viewModel.highlight.value)
        }

    // A late tap can point past the first page; concluding "no longer
    // available" after one page would tell the professional something false.
    @Test
    fun findsTheRowOnALaterPage() =
        runTest {
            repository.inboxResponder = { offset, _ ->
                val page = offset / GetInboxUseCase.PAGE_SIZE
                InboxResult.Loaded(List(GetInboxUseCase.PAGE_SIZE) { inboxRequest(id = "p$page-$it") })
            }

            val viewModel = viewModel(highlightRequestId = "p2-7")
            advanceUntilIdle()

            assertEquals(InboxHighlight.Row("p2-7"), viewModel.highlight.value)
            val content = viewModel.uiState.value as InboxUiState.Content
            assertEquals(3 * GetInboxUseCase.PAGE_SIZE, content.requests.size)
        }

    @Test
    fun reportsUnavailableWhenTheRequestLeftTheInbox() =
        runTest {
            repository.inboxResult = InboxResult.Loaded(listOf(inboxRequest(id = "a")))

            val viewModel = viewModel(highlightRequestId = "gone")
            advanceUntilIdle()

            assertEquals(InboxHighlight.Unavailable, viewModel.highlight.value)
        }

    @Test
    fun anEmptyInboxReportsTheRequestUnavailable() =
        runTest {
            repository.inboxResult = InboxResult.Loaded(emptyList())

            val viewModel = viewModel(highlightRequestId = "gone")
            advanceUntilIdle()

            assertEquals(InboxHighlight.Unavailable, viewModel.highlight.value)
        }

    // The search stops after a bounded number of pages instead of walking an
    // inbox that never ends (RNF-08).
    @Test
    fun stopsSearchingAfterTheLastPageItReads() =
        runTest {
            var reads = 0
            repository.inboxResponder = { offset, _ ->
                reads++
                InboxResult.Loaded(List(GetInboxUseCase.PAGE_SIZE) { inboxRequest(id = "r$offset-$it") })
            }

            val viewModel = viewModel(highlightRequestId = "never")
            advanceUntilIdle()

            assertEquals(InboxHighlight.Unavailable, viewModel.highlight.value)
            assertEquals(5, reads)
        }

    // A failed first page is not a request that left: the retry looks again.
    @Test
    fun aFailedLoadKeepsLookingOnRetry() =
        runTest {
            repository.inboxResult = InboxResult.Failure(RequestError.NetworkUnavailable)
            val viewModel = viewModel(highlightRequestId = "b")
            advanceUntilIdle()
            assertEquals(InboxHighlight.None, viewModel.highlight.value)

            repository.inboxResult = InboxResult.Loaded(listOf(inboxRequest(id = "b")))
            viewModel.onRetry()
            advanceUntilIdle()

            assertEquals(InboxHighlight.Row("b"), viewModel.highlight.value)
        }

    @Test
    fun noHighlightWasAskedForWithoutANotification() =
        runTest {
            repository.inboxResult = InboxResult.Loaded(listOf(inboxRequest(id = "a")))

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(InboxHighlight.None, viewModel.highlight.value)
        }

    @Test
    fun dismissingTheNoticeClearsIt() =
        runTest {
            repository.inboxResult = InboxResult.Loaded(emptyList())
            val viewModel = viewModel(highlightRequestId = "gone")
            advanceUntilIdle()

            viewModel.onDismissUnavailable()

            assertEquals(InboxHighlight.None, viewModel.highlight.value)
        }

    // A later page that fails says nothing about the request: claiming it left
    // would send the professional away from a request that is still open.
    @Test
    fun aFailedLaterPageDoesNotClaimTheRequestLeft() =
        runTest {
            repository.inboxResponder = { offset, _ ->
                if (offset == 0) {
                    InboxResult.Loaded(List(GetInboxUseCase.PAGE_SIZE) { inboxRequest(id = "r$it") })
                } else {
                    InboxResult.Failure(RequestError.NetworkUnavailable)
                }
            }

            val viewModel = viewModel(highlightRequestId = "deeper")
            advanceUntilIdle()

            assertEquals(InboxHighlight.None, viewModel.highlight.value)
        }
}

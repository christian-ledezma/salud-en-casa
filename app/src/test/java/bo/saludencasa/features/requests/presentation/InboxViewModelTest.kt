@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.InboxResult
import bo.saludencasa.features.requests.domain.model.RequestError
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

    private fun viewModel(): InboxViewModel =
        InboxViewModel(
            GetInboxUseCase(repository),
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
}

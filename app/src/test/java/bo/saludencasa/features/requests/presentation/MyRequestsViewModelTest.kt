@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.model.MyRequestsResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.usecase.CancelRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetMyRequestsUseCase
import bo.saludencasa.features.requests.serviceRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyRequestsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeRequestRepository()

    private fun viewModel(): MyRequestsViewModel =
        MyRequestsViewModel(GetMyRequestsUseCase(repository), CancelRequestUseCase(repository))

    @Test
    fun noRequestsIsTheEmptyState() =
        runTest {
            repository.requestsResult = MyRequestsResult.Loaded(emptyList())

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(MyRequestsUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun aFailedLoadIsReported() =
        runTest {
            repository.requestsResult = MyRequestsResult.Failure(RequestError.NetworkUnavailable)

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(MyRequestsUiState.Failed(RequestError.NetworkUnavailable), viewModel.uiState.value)
        }

    // Cancelling reads the list again rather than patching the one row, because
    // the sweep or the other party may have moved more since it was read.
    @Test
    fun aSuccessfulCancellationReloadsTheList() =
        runTest {
            repository.requestsResult = MyRequestsResult.Loaded(listOf(serviceRequest(id = "r1")))

            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onCancel("r1")
            advanceUntilIdle()

            assertEquals("r1", repository.cancelledIds.single())
            // One read on open, one read after the cancellation.
            assertEquals(2, repository.pageRequests.count { it.first == 0 })
        }

    // A refused cancellation leaves the request where it is and surfaces the
    // reason beside the list, not a blank screen.
    @Test
    fun aFailedCancellationKeepsTheListAndShowsTheReason() =
        runTest {
            repository.requestsResult = MyRequestsResult.Loaded(listOf(serviceRequest(id = "r1")))
            repository.cancelResult = CancelRequestResult.Failure(RequestError.NetworkUnavailable)

            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onCancel("r1")
            advanceUntilIdle()

            val content = viewModel.uiState.value as MyRequestsUiState.Content
            assertEquals(RequestError.NetworkUnavailable, content.notice)
            assertEquals(1, content.requests.size)
        }

    // The second page appends without the first page's rows coming back, and a
    // short page ends the list.
    @Test
    fun aSecondPageAppendsAndEndsOnAShortPage() =
        runTest {
            val firstPage = List(GetMyRequestsUseCase.PAGE_SIZE) { serviceRequest(id = "r$it") }
            repository.requestsResponder = { offset, _ ->
                if (offset == 0) {
                    MyRequestsResult.Loaded(firstPage)
                } else {
                    MyRequestsResult.Loaded(listOf(serviceRequest(id = "tail")))
                }
            }

            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onReachedEnd()
            advanceUntilIdle()

            val content = viewModel.uiState.value as MyRequestsUiState.Content
            assertEquals(GetMyRequestsUseCase.PAGE_SIZE + 1, content.requests.size)
            assertTrue(content.endReached)
        }

    // A tab of the home screen lives as long as the shell, so the list is read
    // again on every resume and has to show what was published since.
    @Test
    fun aRefreshShowsARequestPublishedSinceTheListWasRead() =
        runTest {
            repository.requestsResult = MyRequestsResult.Loaded(listOf(serviceRequest(id = "r1")))
            val viewModel = viewModel()
            advanceUntilIdle()

            repository.requestsResult =
                MyRequestsResult.Loaded(listOf(serviceRequest(id = "r2"), serviceRequest(id = "r1")))
            viewModel.refresh()
            advanceUntilIdle()

            val content = viewModel.uiState.value as MyRequestsUiState.Content
            assertEquals(listOf("r2", "r1"), content.requests.map { it.id })
        }

    // The inbox rule: a background read that fails never blanks the list.
    @Test
    fun aRefreshThatFailsKeepsTheListOnScreen() =
        runTest {
            repository.requestsResult = MyRequestsResult.Loaded(listOf(serviceRequest(id = "r1")))
            val viewModel = viewModel()
            advanceUntilIdle()

            repository.requestsResult = MyRequestsResult.Failure(RequestError.NetworkUnavailable)
            viewModel.refresh()
            advanceUntilIdle()

            val content = viewModel.uiState.value as MyRequestsUiState.Content
            assertEquals(listOf("r1"), content.requests.map { it.id })
        }
}

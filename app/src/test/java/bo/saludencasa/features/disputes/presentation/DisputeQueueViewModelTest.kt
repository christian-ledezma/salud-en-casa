@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.disputes.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.disputes.FakeDisputeRepository
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.usecase.GetDisputesUseCase
import bo.saludencasa.features.disputes.paymentDispute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DisputeQueueViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDisputeRepository()

    private fun viewModel() = DisputeQueueViewModel(GetDisputesUseCase(repository))

    private fun DisputeQueueViewModel.content() = uiState.value as DisputeQueueUiState.Content

    @Test
    fun noDisputeIsTheEmptyStateNotAnEmptyList() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(DisputeQueueUiState.Empty, viewModel.uiState.value)
        }

    // RNF-08: a full page leaves more to load; the next page follows it.
    @Test
    fun aFullPageOffersMoreAndTheNextPageFollowsIt() =
        runTest {
            repository.disputes = (1..GetDisputesUseCase.PAGE_SIZE + 3).map { paymentDispute(paymentId = "p$it") }
            val viewModel = viewModel()
            advanceUntilIdle()
            assertFalse(viewModel.content().endReached)

            viewModel.onLoadMore()
            advanceUntilIdle()

            assertEquals(GetDisputesUseCase.PAGE_SIZE + 3, viewModel.content().disputes.size)
            assertTrue(viewModel.content().endReached)
        }

    // Back from resolving one: the list reads itself again and the one resolved is gone.
    @Test
    fun aResolvedDisputeLeavesTheListOnRefresh() =
        runTest {
            repository.disputes = listOf(paymentDispute("p1"), paymentDispute("p2"))
            val viewModel = viewModel()
            advanceUntilIdle()

            repository.disputes = listOf(paymentDispute("p2"))
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(listOf("p2"), viewModel.content().disputes.map { it.paymentId })
        }

    @Test
    fun aFailedRefreshLeavesTheListAsItWas() =
        runTest {
            repository.disputes = listOf(paymentDispute("p1"))
            val viewModel = viewModel()
            advanceUntilIdle()

            repository.listError = DisputeError.NetworkUnavailable
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(listOf("p1"), viewModel.content().disputes.map { it.paymentId })
        }

    @Test
    fun aFailedFirstReadSaysWhy() =
        runTest {
            repository.listError = DisputeError.NetworkUnavailable
            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(DisputeQueueUiState.Failed(DisputeError.NetworkUnavailable), viewModel.uiState.value)
        }
}

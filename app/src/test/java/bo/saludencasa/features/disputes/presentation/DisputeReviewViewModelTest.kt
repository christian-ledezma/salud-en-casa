@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.disputes.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.disputes.FakeDisputeRepository
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.model.DisputeResolution
import bo.saludencasa.features.disputes.domain.usecase.GetDisputeUseCase
import bo.saludencasa.features.disputes.domain.usecase.ResolveDisputeUseCase
import bo.saludencasa.features.disputes.paymentDispute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DisputeReviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDisputeRepository().apply { disputes = listOf(paymentDispute("p1")) }

    private fun TestScope.opened(): DisputeReviewViewModel {
        val viewModel = DisputeReviewViewModel("p1", GetDisputeUseCase(repository), ResolveDisputeUseCase(repository))
        advanceUntilIdle()
        return viewModel
    }

    private fun DisputeReviewViewModel.content() = uiState.value as DisputeReviewUiState.Content

    // The button chose the resolution; dismissing the dialog sends nothing.
    @Test
    fun dismissingTheDialogResolvesNothing() =
        runTest {
            val viewModel = opened()

            viewModel.onResolveRequested(DisputeResolution.NOT_PAID)
            viewModel.onResolveDismissed()
            advanceUntilIdle()

            assertTrue(repository.resolutions.isEmpty())
            assertNull(viewModel.content().draft)
        }

    @Test
    fun confirmingSendsTheChosenResolutionAndLeavesTheScreen() =
        runTest {
            val viewModel = opened()

            viewModel.onResolveRequested(DisputeResolution.NOT_PAID)
            viewModel.onResolveConfirmed()
            advanceUntilIdle()

            assertEquals(listOf("p1" to DisputeResolution.NOT_PAID), repository.resolutions)
            assertTrue(viewModel.content().resolved)
            assertNull(viewModel.content().draft)
        }

    @Test
    fun aFailedResolutionKeepsTheDialogWithItsSense() =
        runTest {
            repository.resolveError = DisputeError.NetworkUnavailable
            val viewModel = opened()

            viewModel.onResolveRequested(DisputeResolution.PAID)
            viewModel.onResolveConfirmed()
            advanceUntilIdle()

            val draft = viewModel.content().draft!!
            assertEquals(DisputeResolution.PAID, draft.resolution)
            assertEquals(DisputeError.NetworkUnavailable, draft.error)
            assertFalse(draft.sending)
            assertFalse(viewModel.content().resolved)
        }

    // Resolved first from another phone: the screen says so instead of an error
    // to retry, because a resolution is final.
    @Test
    fun aDisputeResolvedMeanwhileSaysItIsGone() =
        runTest {
            repository.resolveError = DisputeError.AlreadyResolved
            val viewModel = opened()

            viewModel.onResolveRequested(DisputeResolution.PAID)
            viewModel.onResolveConfirmed()
            advanceUntilIdle()

            assertEquals(DisputeReviewUiState.Gone, viewModel.uiState.value)
        }

    @Test
    fun aDisputeNoLongerInTheQueueOpensAsGone() =
        runTest {
            repository.disputes = emptyList()

            assertEquals(DisputeReviewUiState.Gone, opened().uiState.value)
        }

    @Test
    fun aResolutionOnItsWayCannotBeDismissedOrSentTwice() =
        runTest {
            repository.holdResolve = CompletableDeferred()
            val viewModel = opened()
            viewModel.onResolveRequested(DisputeResolution.PAID)

            viewModel.onResolveConfirmed()
            advanceUntilIdle()
            viewModel.onResolveDismissed()
            viewModel.onResolveConfirmed()
            advanceUntilIdle()

            assertTrue(viewModel.content().draft!!.sending)
            assertEquals(1, repository.resolutions.size)
            repository.holdResolve!!.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.content().resolved)
        }
}

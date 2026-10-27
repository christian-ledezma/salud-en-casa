@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.requests.FakeOfferRepository
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.RequestOffersResult
import bo.saludencasa.features.requests.domain.usecase.GetRequestOffersUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveMyOfferSignalsUseCase
import bo.saludencasa.features.requests.requestOffer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NegotiationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val offers = FakeOfferRepository()

    private fun viewModel(): NegotiationViewModel =
        NegotiationViewModel(
            requestId = "r1",
            getRequestOffers = GetRequestOffersUseCase(offers),
            observeMyOfferSignals = ObserveMyOfferSignalsUseCase(offers),
        )

    @Test
    fun loadsTheOffersIntoContent() =
        runTest {
            offers.offersResult = RequestOffersResult.Loaded(listOf(requestOffer(id = "o1")))

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                NegotiationUiState.Content(listOf(requestOffer(id = "o1"))),
                viewModel.uiState.value,
            )
        }

    @Test
    fun noOffersShowsEmpty() =
        runTest {
            offers.offersResult = RequestOffersResult.Loaded(emptyList())

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(NegotiationUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun aFailureIsReported() =
        runTest {
            offers.offersResult = RequestOffersResult.Failure(OfferError.NetworkUnavailable)

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(NegotiationUiState.Failed(OfferError.NetworkUnavailable), viewModel.uiState.value)
        }

    // Criterion 2: an offer landing re-queries without showing the spinner again.
    // The emission takes the state straight from Empty to Content, never back to
    // Loading.
    @Test
    fun aSignalRefreshesTheThreadWithoutReloading() =
        runTest {
            offers.offersResult = RequestOffersResult.Loaded(emptyList())
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                assertEquals(NegotiationUiState.Empty, awaitItem())

                offers.offersResult = RequestOffersResult.Loaded(listOf(requestOffer(id = "o1")))
                offers.offerSignals.emit(Unit)

                assertEquals(NegotiationUiState.Content(listOf(requestOffer(id = "o1"))), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}

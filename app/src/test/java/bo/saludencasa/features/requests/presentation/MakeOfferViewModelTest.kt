@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.requests.FakeOfferRepository
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.usecase.GetOfferableRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.SubmitOfferUseCase
import bo.saludencasa.features.requests.offerableRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MakeOfferViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val offers = FakeOfferRepository()

    private fun viewModel(): MakeOfferViewModel =
        MakeOfferViewModel(
            requestId = "r1",
            getOfferableRequest = GetOfferableRequestUseCase(offers),
            submitOffer = SubmitOfferUseCase(offers),
        )

    private fun TestScope.opened(): MakeOfferViewModel {
        val viewModel = viewModel()
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun loadsTheRequestIntoTheForm() =
        runTest {
            offers.offerableResult = OfferableRequestResult.Loaded(offerableRequest())

            val viewModel = opened()

            val content = viewModel.uiState.value as MakeOfferUiState.Content
            assertTrue(content.canSubmit)
        }

    // The request is accepted, cancelled or gone: there is nothing to offer on.
    @Test
    fun aRequestThatIsNotVisibleShowsGone() =
        runTest {
            offers.offerableResult = OfferableRequestResult.NotFound

            val viewModel = opened()

            assertEquals(MakeOfferUiState.Gone, viewModel.uiState.value)
        }

    // Criterion 5 on screen: already offered cannot offer again.
    @Test
    fun alreadyOfferedCannotSubmitAgain() =
        runTest {
            offers.offerableResult = OfferableRequestResult.Loaded(offerableRequest(alreadyOffered = true))

            val viewModel = opened()

            val content = viewModel.uiState.value as MakeOfferUiState.Content
            assertFalse(content.canSubmit)

            viewModel.onSubmit()
            advanceUntilIdle()
            assertTrue(offers.submitted.isEmpty())
        }

    @Test
    fun aSuccessfulOfferMovesToSent() =
        runTest {
            offers.offerableResult = OfferableRequestResult.Loaded(offerableRequest())
            val viewModel = opened()

            viewModel.onAmountChanged("140")
            viewModel.onMessageChanged("Puedo llegar en una hora")
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(MakeOfferUiState.Sent, viewModel.uiState.value)
            assertEquals("r1", offers.submitted.single().requestId)
        }

    // A refused offer keeps the form and its typed values, with the error shown.
    @Test
    fun aRefusedOfferKeepsTheFormWithItsError() =
        runTest {
            offers.offerableResult = OfferableRequestResult.Loaded(offerableRequest())
            offers.submitResult = OfferSubmitResult.Failure(OfferError.NotAllowed)
            val viewModel = opened()

            viewModel.onAmountChanged("140")
            viewModel.onMessageChanged("Puedo llegar en una hora")
            viewModel.onSubmit()
            advanceUntilIdle()

            val content = viewModel.uiState.value as MakeOfferUiState.Content
            assertEquals(OfferError.NotAllowed, content.error)
            assertFalse(content.submitting)
            assertEquals("140", content.amount)
        }
}

@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.home.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.ActiveRequestResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.usecase.GetActiveImmediateRequestUseCase
import bo.saludencasa.features.requests.serviceRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PatientHomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val requests = FakeRequestRepository()

    private fun viewModel(): PatientHomeViewModel = PatientHomeViewModel(GetActiveImmediateRequestUseCase(requests))

    // The home screen is not rebuilt when the patient comes back from publishing:
    // it is the resume that has to bring the new request into "En curso".
    @Test
    fun aRequestPublishedWhileAwayIsInProgressOnTheWayBack() =
        runTest {
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(PatientHomeUiState.NothingInProgress, viewModel.uiState.value)

            requests.activeResult = ActiveRequestResult.Found(serviceRequest(id = "r1"))
            viewModel.refresh()
            advanceUntilIdle()

            val inProgress = viewModel.uiState.value as PatientHomeUiState.InProgress
            assertEquals("r1", inProgress.request.id)
        }

    // The section is a shortcut to what "Solicitudes" always reaches; a read that
    // fails keeps what was shown instead of putting an error on the home screen.
    @Test
    fun aFailedReadKeepsTheRequestThatWasInProgress() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(serviceRequest(id = "r1"))
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            requests.activeResult = ActiveRequestResult.Failure(RequestError.NetworkUnavailable)
            viewModel.refresh()
            advanceUntilIdle()

            val inProgress = viewModel.uiState.value as PatientHomeUiState.InProgress
            assertEquals("r1", inProgress.request.id)
        }
}

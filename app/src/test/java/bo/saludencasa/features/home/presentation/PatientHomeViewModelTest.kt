@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.home.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.ActiveRequestResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.usecase.GetActiveImmediateRequestUseCase
import bo.saludencasa.features.requests.serviceRequest
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attentionSummary
import bo.saludencasa.features.services.domain.model.ActiveAttentionResult
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.usecase.GetActiveAttentionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PatientHomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val requests = FakeRequestRepository()
    private val services = FakeServiceRepository()

    private fun viewModel(): PatientHomeViewModel =
        PatientHomeViewModel(GetActiveImmediateRequestUseCase(requests), GetActiveAttentionUseCase(services))

    private fun PatientHomeViewModel.content() = uiState.value as PatientHomeUiState.Content

    // The home screen is not rebuilt when the patient comes back from publishing:
    // it is the resume that has to bring the new request into "En curso".
    @Test
    fun aRequestPublishedWhileAwayIsInProgressOnTheWayBack() =
        runTest {
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()
            assertNull(viewModel.content().activeRequest)

            requests.activeResult = ActiveRequestResult.Found(serviceRequest(id = "r1"))
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals("r1", viewModel.content().activeRequest?.id)
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

            assertEquals("r1", viewModel.content().activeRequest?.id)
        }

    // HT-27: an accepted offer leaves «Mis solicitudes», so Inicio is the short way
    // to it, and reading it is what the patient's side of the engine answers.
    @Test
    fun anAttentionAgreedWhileAwayAppearsOnTheWayBack() =
        runTest {
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()
            assertNull(viewModel.content().activeAttention)

            services.activeAttention = ActiveAttentionResult.Found(attentionSummary(requestId = "r2"))
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals("r2", viewModel.content().activeAttention?.requestId)
            assertEquals(setOf(AttentionSide.PATIENT), services.sidesRead.toSet())
        }

    @Test
    fun anAttentionThatEndedLeavesInicio() =
        runTest {
            services.activeAttention = ActiveAttentionResult.Found(attentionSummary())
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            services.activeAttention = ActiveAttentionResult.None
            viewModel.refresh()
            advanceUntilIdle()

            assertNull(viewModel.content().activeAttention)
        }

    // Each shortcut answers for itself: one read failing keeps its own and does not
    // take the other down.
    @Test
    fun aFailedReadKeepsTheAttentionAndStillUpdatesTheRequest() =
        runTest {
            services.activeAttention = ActiveAttentionResult.Found(attentionSummary(requestId = "r2"))
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            services.activeAttention = ActiveAttentionResult.Failure(ServiceError.NetworkUnavailable)
            requests.activeResult = ActiveRequestResult.Found(serviceRequest(id = "r1"))
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals("r2", viewModel.content().activeAttention?.requestId)
            assertEquals("r1", viewModel.content().activeRequest?.id)
        }
}

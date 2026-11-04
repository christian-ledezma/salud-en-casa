@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.services.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.AdvanceAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.ObserveAttentionUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttentionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeServiceRepository()

    private fun viewModel() =
        AttentionViewModel(
            "r1",
            GetAttentionUseCase(repository),
            AdvanceAttentionUseCase(repository),
            ObserveAttentionUseCase(repository),
        )

    private fun TestScope.opened(attention: Attention): AttentionViewModel {
        repository.result = AttentionResult.Loaded(attention)
        val viewModel = viewModel()
        advanceUntilIdle()
        return viewModel
    }

    private fun AttentionViewModel.content() = uiState.value as AttentionUiState.Content

    private fun professional(status: ServiceStatus = ServiceStatus.ASSIGNED) =
        attention(status = status, iAmThePatient = false)

    private fun TestScope.confirm(viewModel: AttentionViewModel) {
        viewModel.onMarkRequested()
        viewModel.onMarkConfirmed()
        advanceUntilIdle()
    }

    @Test
    fun showsTheSealedAgreementWithTheCounterpart() =
        runTest {
            val viewModel = opened(attention(counterpartId = "pro-1"))

            val state = viewModel.content()
            assertEquals("pro-1", state.attention.counterpart.id)
            assertEquals("María Quispe", state.attention.counterpart.name)
            assertEquals(true, state.attention.iAmThePatient)
        }

    @Test
    fun showsNotFoundWhenThereIsNoServiceForTheCaller() =
        runTest {
            repository.result = AttentionResult.NotFound

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(AttentionUiState.NotFound, viewModel.uiState.value)
        }

    @Test
    fun reportsAFailure() =
        runTest {
            repository.result = AttentionResult.Failure(ServiceError.NetworkUnavailable)

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(AttentionUiState.Failed(ServiceError.NetworkUnavailable), viewModel.uiState.value)
        }

    // Asking is not doing: neither mark can be undone (docs/decisions.md, 2026-11-04).
    @Test
    fun nothingIsMarkedUntilItIsConfirmed() =
        runTest {
            val viewModel = opened(professional())

            viewModel.onMarkRequested()
            assertEquals(AttentionStep.MarkArrival, viewModel.content().confirming)
            viewModel.onMarkDismissed()
            advanceUntilIdle()

            assertNull(viewModel.content().confirming)
            assertTrue(repository.advances.isEmpty())
        }

    @Test
    fun aMarkMovesTheAttentionToItsNextStep() =
        runTest {
            val viewModel = opened(professional())

            confirm(viewModel)
            assertEquals(ServiceStatus.IN_PROGRESS, viewModel.content().attention.status)
            assertNotNull(viewModel.content().attention.startedAt)
            assertEquals(AttentionStep.MarkCompleted, viewModel.content().attention.nextStepForMe)

            confirm(viewModel)
            assertEquals(ServiceStatus.COMPLETED, viewModel.content().attention.status)
            assertNull(viewModel.content().attention.nextStepForMe)

            assertEquals(listOf(AttentionStep.MarkArrival, AttentionStep.MarkCompleted), repository.advances)
        }

    // A second tap while the first is on its way must not ask the engine for the
    // step after the one it has not answered yet.
    @Test
    fun aSecondConfirmationWhileAdvancingIsIgnored() =
        runTest {
            val hold = CompletableDeferred<Unit>()
            repository.holdAdvance = hold
            val viewModel = opened(professional())

            confirm(viewModel)
            assertTrue(viewModel.content().advancing)
            confirm(viewModel)
            hold.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(AttentionStep.MarkArrival), repository.advances)
            assertFalse(viewModel.content().advancing)
        }

    @Test
    fun aFailedMarkKeepsTheStepAndSaysWhy() =
        runTest {
            repository.advanceError = ServiceError.NetworkUnavailable
            val viewModel = opened(professional())

            confirm(viewModel)

            assertEquals(ServiceError.NetworkUnavailable, viewModel.content().notice)
            assertEquals(ServiceStatus.ASSIGNED, viewModel.content().attention.status)
            assertEquals(AttentionStep.MarkArrival, viewModel.content().attention.nextStepForMe)
        }

    // The attention was cancelled meanwhile: the screen says so by itself.
    @Test
    fun aMarkRefusedBecauseTheAttentionChangedShowsWhatItIsNow() =
        runTest {
            val viewModel = opened(professional())
            repository.advanceError = ServiceError.AttentionChanged
            repository.result = AttentionResult.Loaded(professional(ServiceStatus.CANCELLED))

            confirm(viewModel)

            assertEquals(ServiceStatus.CANCELLED, viewModel.content().attention.status)
            assertNull(viewModel.content().notice)
            assertNull(viewModel.content().attention.nextStepForMe)
        }

    @Test
    fun aMarkRefusedThatCannotBeReadAgainSaysSo() =
        runTest {
            val viewModel = opened(professional())
            repository.advanceError = ServiceError.AttentionChanged
            repository.result = AttentionResult.Failure(ServiceError.NetworkUnavailable)

            confirm(viewModel)

            assertEquals(ServiceError.AttentionChanged, viewModel.content().notice)
        }

    // Marked from a second phone with the dialog open here: the same button would
    // now say the next step, and one tap would complete the attention.
    @Test
    fun aDialogOverAStepThatPassedClosesInsteadOfConfirmingTheNext() =
        runTest {
            val viewModel = opened(professional())
            viewModel.onMarkRequested()

            repository.result = AttentionResult.Loaded(professional(ServiceStatus.IN_PROGRESS))
            repository.signals.emit(Unit)
            advanceUntilIdle()
            assertNull(viewModel.content().confirming)

            viewModel.onMarkConfirmed()
            advanceUntilIdle()
            assertTrue(repository.advances.isEmpty())
        }

    @Test
    fun aSignalKeepsADialogThatStillApplies() =
        runTest {
            val viewModel = opened(professional())
            viewModel.onMarkRequested()

            repository.signals.emit(Unit)
            advanceUntilIdle()

            assertEquals(AttentionStep.MarkArrival, viewModel.content().confirming)
        }

    // A signal in the middle must not enable the button again: a second tap would
    // ask for the step after the one still on its way.
    @Test
    fun aSignalWhileAMarkIsOnItsWayKeepsItOnItsWay() =
        runTest {
            val hold = CompletableDeferred<Unit>()
            repository.holdAdvance = hold
            val viewModel = opened(professional())
            confirm(viewModel)

            repository.signals.emit(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.content().advancing)
            hold.complete(Unit)
        }

    // HU-23, criterion 1: the patient has the screen open when the professional marks.
    @Test
    fun aSignalShowsTheNewStateWithoutReopening() =
        runTest {
            val viewModel = opened(attention(status = ServiceStatus.ASSIGNED))

            repository.result = AttentionResult.Loaded(attention(status = ServiceStatus.IN_PROGRESS))
            repository.signals.emit(Unit)
            advanceUntilIdle()

            assertEquals(ServiceStatus.IN_PROGRESS, viewModel.content().attention.status)
        }

    @Test
    fun aResumeShowsTheNewState() =
        runTest {
            val viewModel = opened(attention(status = ServiceStatus.ASSIGNED))

            repository.result = AttentionResult.Loaded(attention(status = ServiceStatus.COMPLETED))
            viewModel.onResumed()
            advanceUntilIdle()

            assertEquals(ServiceStatus.COMPLETED, viewModel.content().attention.status)
        }

    @Test
    fun aFailedSilentReadLeavesTheScreenAsItWas() =
        runTest {
            val viewModel = opened(attention(status = ServiceStatus.ASSIGNED))

            repository.result = AttentionResult.Failure(ServiceError.NetworkUnavailable)
            repository.signals.emit(Unit)
            advanceUntilIdle()

            assertEquals(ServiceStatus.ASSIGNED, viewModel.content().attention.status)
        }

    // RF-10.1 and RF-10.2. The engine refuses it too.
    @Test
    fun aPatientCannotConfirmAMark() =
        runTest {
            val viewModel = opened(attention(status = ServiceStatus.ASSIGNED, iAmThePatient = true))

            confirm(viewModel)

            assertTrue(repository.advances.isEmpty())
        }
}

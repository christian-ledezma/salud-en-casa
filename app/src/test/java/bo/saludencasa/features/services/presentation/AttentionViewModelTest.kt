@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.services.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.ConfirmPaymentError
import bo.saludencasa.features.services.domain.model.PaymentAnswer
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.services.domain.model.PaymentStatus
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.AdvanceAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.AnswerPaymentUseCase
import bo.saludencasa.features.services.domain.usecase.CancelAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.ConfirmPaymentUseCase
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.ObserveAttentionUseCase
import bo.saludencasa.features.services.paidInCash
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
            CancelAttentionUseCase(repository),
            ConfirmPaymentUseCase(repository),
            AnswerPaymentUseCase(repository),
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

    private fun TestScope.cancel(
        viewModel: AttentionViewModel,
        reason: String = REASON,
    ) {
        viewModel.onCancelRequested()
        viewModel.onCancelReasonChanged(reason)
        viewModel.onCancelConfirmed()
        advanceUntilIdle()
    }

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

    // RF-10.3.
    @Test
    fun eitherPartyCancelsAndTheScreenRereadsIt() =
        runTest {
            listOf(true, false).forEach { iAmThePatient ->
                val viewModel = opened(attention(iAmThePatient = iAmThePatient))

                cancel(viewModel)

                val state = viewModel.content()
                assertEquals(ServiceStatus.CANCELLED, state.attention.status)
                assertEquals(REASON, state.attention.cancellation?.reason)
                assertNull(state.cancelDraft)
            }
        }

    @Test
    fun aCompletedAttentionDoesNotOpenTheDialog() =
        runTest {
            val viewModel = opened(attention(status = ServiceStatus.COMPLETED))

            viewModel.onCancelRequested()

            assertNull(viewModel.content().cancelDraft)
        }

    // cancellationRequiresReason.
    @Test
    fun withoutAReasonNothingTravelsAndTheDialogSaysWhy() =
        runTest {
            val viewModel = opened(attention())

            cancel(viewModel, reason = "   ")

            assertTrue(repository.cancels.isEmpty())
            assertEquals(CancelAttentionError.ReasonEmpty, viewModel.content().cancelDraft?.error)
            assertEquals(ServiceStatus.ASSIGNED, viewModel.content().attention.status)
        }

    @Test
    fun writingAgainClearsTheError() =
        runTest {
            val viewModel = opened(attention())
            cancel(viewModel, reason = "")

            viewModel.onCancelReasonChanged("N")

            assertNull(viewModel.content().cancelDraft?.error)
        }

    // Writing the reason again would punish a dropped connection.
    @Test
    fun aFailureKeepsWhatWasWrittenAndAllowsRetrying() =
        runTest {
            repository.cancelError = ServiceError.NetworkUnavailable
            val viewModel = opened(attention())

            cancel(viewModel)

            val draft = viewModel.content().cancelDraft
            assertEquals(REASON, draft?.reason)
            assertEquals(CancelAttentionError.Service(ServiceError.NetworkUnavailable), draft?.error)
            assertEquals(false, draft?.sending)

            repository.cancelError = null
            viewModel.onCancelConfirmed()
            advanceUntilIdle()

            assertEquals(ServiceStatus.CANCELLED, viewModel.content().attention.status)
        }

    // A second tap must not send a second reason, which the engine would refuse.
    @Test
    fun aCancellationOnItsWayIsNotSentTwiceNorDismissed() =
        runTest {
            repository.holdCancel = CompletableDeferred()
            val viewModel = opened(attention())

            viewModel.onCancelRequested()
            viewModel.onCancelReasonChanged(REASON)
            viewModel.onCancelConfirmed()
            advanceUntilIdle()
            viewModel.onCancelConfirmed()
            viewModel.onCancelDismissed()
            advanceUntilIdle()

            assertEquals(1, repository.cancels.size)
            assertEquals(true, viewModel.content().cancelDraft?.sending)

            repository.holdCancel?.complete(Unit)
            advanceUntilIdle()
            assertNull(viewModel.content().cancelDraft)
        }

    // The professional marked it completed while the dialog was open: the button would
    // answer a question the screen stopped asking.
    @Test
    fun aSignalThatEndsTheAttentionClosesTheDialog() =
        runTest {
            val viewModel = opened(attention(status = ServiceStatus.IN_PROGRESS))
            viewModel.onCancelRequested()
            viewModel.onCancelReasonChanged(REASON)

            repository.result = AttentionResult.Loaded(attention(status = ServiceStatus.COMPLETED))
            repository.signals.emit(Unit)
            advanceUntilIdle()

            assertEquals(ServiceStatus.COMPLETED, viewModel.content().attention.status)
            assertNull(viewModel.content().cancelDraft)
        }

    @Test
    fun aSignalThatKeepsItCancellableKeepsTheDialogAndWhatWasWritten() =
        runTest {
            val viewModel = opened(attention(status = ServiceStatus.ASSIGNED))
            viewModel.onCancelRequested()
            viewModel.onCancelReasonChanged(REASON)

            repository.result = AttentionResult.Loaded(attention(status = ServiceStatus.IN_PROGRESS))
            repository.signals.emit(Unit)
            advanceUntilIdle()

            assertEquals(REASON, viewModel.content().cancelDraft?.reason)
        }

    // cannotCancelCompletedService: the engine refused because it got there first.
    @Test
    fun anAttentionThatChangedIsRereadWithoutAskingAnything() =
        runTest {
            val viewModel = opened(attention(status = ServiceStatus.IN_PROGRESS))
            repository.cancelError = ServiceError.AttentionChanged
            viewModel.onCancelRequested()
            viewModel.onCancelReasonChanged(REASON)
            repository.result = AttentionResult.Loaded(attention(status = ServiceStatus.COMPLETED))

            viewModel.onCancelConfirmed()
            advanceUntilIdle()

            assertEquals(ServiceStatus.COMPLETED, viewModel.content().attention.status)
            assertNull(viewModel.content().cancelDraft)
        }

    @Test
    fun anAttentionThatChangedAndCannotBeRereadSaysSo() =
        runTest {
            val viewModel = opened(attention(status = ServiceStatus.IN_PROGRESS))
            repository.cancelError = ServiceError.AttentionChanged
            viewModel.onCancelRequested()
            viewModel.onCancelReasonChanged(REASON)
            repository.result = AttentionResult.Failure(ServiceError.NetworkUnavailable)

            viewModel.onCancelConfirmed()
            advanceUntilIdle()

            assertEquals(
                CancelAttentionError.Service(ServiceError.AttentionChanged),
                viewModel.content().cancelDraft?.error,
            )
        }

    private companion object {
        const val REASON = "Me surgió una emergencia."
    }

    private fun completedForThePatient() = attention(status = ServiceStatus.COMPLETED, iAmThePatient = true)

    @Test
    fun thePayDialogOpensOnlyForThePatientOfACompletedAttention() =
        runTest {
            listOf(
                attention(status = ServiceStatus.COMPLETED, iAmThePatient = false),
                attention(status = ServiceStatus.IN_PROGRESS, iAmThePatient = true),
                attention(status = ServiceStatus.COMPLETED, iAmThePatient = true, payment = paidInCash()),
            ).forEach { attention ->
                val viewModel = opened(attention)
                viewModel.onPayRequested()
                assertNull(viewModel.content().paymentDraft)
            }
        }

    // RF-11.2: the method is the patient's answer, never a default.
    @Test
    fun nothingIsSentUntilAMethodIsChosen() =
        runTest {
            val viewModel = opened(completedForThePatient())

            viewModel.onPayRequested()
            viewModel.onPayConfirmed()
            advanceUntilIdle()

            assertTrue(repository.payments.isEmpty())
            assertFalse(viewModel.content().paymentDraft!!.canConfirm)
        }

    @Test
    fun confirmingClosesTheDialogAndShowsThePatientsConfirmation() =
        runTest {
            val viewModel = opened(completedForThePatient())

            viewModel.onPayRequested()
            viewModel.onPaymentMethodChosen(PaymentMethod.DIRECT_QR)
            viewModel.onPayConfirmed()
            advanceUntilIdle()

            val state = viewModel.content()
            assertEquals(listOf(PaymentMethod.DIRECT_QR), repository.payments)
            assertNull(state.paymentDraft)
            assertEquals(PaymentStatus.PATIENT_CONFIRMED, state.attention.payment.status)
            assertFalse(state.attention.canConfirmPayment)
        }

    @Test
    fun aFailedConfirmationKeepsTheDialogAndTheChosenMethod() =
        runTest {
            repository.paymentError = ServiceError.NetworkUnavailable
            val viewModel = opened(completedForThePatient())

            viewModel.onPayRequested()
            viewModel.onPaymentMethodChosen(PaymentMethod.CASH)
            viewModel.onPayConfirmed()
            advanceUntilIdle()

            val draft = viewModel.content().paymentDraft!!
            assertEquals(PaymentMethod.CASH, draft.method)
            assertEquals(ConfirmPaymentError.Service(ServiceError.NetworkUnavailable), draft.error)
            assertFalse(draft.sending)
        }

    // Confirmed from another phone first: the dialog closes on what the engine says.
    @Test
    fun aPaymentConfirmedElsewhereClosesTheDialog() =
        runTest {
            val viewModel = opened(completedForThePatient())
            viewModel.onPayRequested()
            viewModel.onPaymentMethodChosen(PaymentMethod.CASH)

            repository.paymentError = ServiceError.AttentionChanged
            repository.result =
                AttentionResult.Loaded(
                    attention(status = ServiceStatus.COMPLETED, iAmThePatient = true, payment = paidInCash()),
                )
            viewModel.onPayConfirmed()
            advanceUntilIdle()

            assertNull(viewModel.content().paymentDraft)
            assertEquals(
                PaymentStatus.PATIENT_CONFIRMED,
                viewModel
                    .content()
                    .attention.payment.status,
            )
        }

    @Test
    fun aConfirmationOnItsWayCannotBeDismissedOrSentTwice() =
        runTest {
            repository.holdPayment = CompletableDeferred()
            val viewModel = opened(completedForThePatient())
            viewModel.onPayRequested()
            viewModel.onPaymentMethodChosen(PaymentMethod.CASH)

            viewModel.onPayConfirmed()
            advanceUntilIdle()
            viewModel.onPayDismissed()
            viewModel.onPayConfirmed()
            viewModel.onPaymentMethodChosen(PaymentMethod.DIRECT_QR)
            advanceUntilIdle()

            val draft = viewModel.content().paymentDraft!!
            assertTrue(draft.sending)
            assertEquals(PaymentMethod.CASH, draft.method)
            assertEquals(1, repository.payments.size)
            repository.holdPayment!!.complete(Unit)
            advanceUntilIdle()
            assertNull(viewModel.content().paymentDraft)
        }

    private fun declaredToTheProfessional() =
        attention(status = ServiceStatus.COMPLETED, iAmThePatient = false, payment = paidInCash())

    @Test
    fun theAnswerOpensOnlyForTheProfessionalOnceThePatientConfirmed() =
        runTest {
            listOf(
                attention(status = ServiceStatus.COMPLETED, iAmThePatient = false),
                attention(status = ServiceStatus.COMPLETED, iAmThePatient = true, payment = paidInCash()),
                attention(
                    status = ServiceStatus.COMPLETED,
                    iAmThePatient = false,
                    payment = paidInCash().copy(status = PaymentStatus.BOTH_CONFIRMED),
                ),
            ).forEach { attention ->
                val viewModel = opened(attention)
                viewModel.onAnswerRequested(PaymentAnswer.RECEIVED)
                assertNull(viewModel.content().answerDraft)
            }
        }

    // The button chose the answer; dismissing the dialog sends nothing.
    @Test
    fun dismissingTheAnswerSendsNothing() =
        runTest {
            val viewModel = opened(declaredToTheProfessional())

            viewModel.onAnswerRequested(PaymentAnswer.NOT_RECEIVED)
            viewModel.onAnswerDismissed()
            advanceUntilIdle()

            assertTrue(repository.answers.isEmpty())
            assertNull(viewModel.content().answerDraft)
        }

    @Test
    fun confirmingTheReceiptClosesTheDialogAndShowsThePaymentConfirmedByBoth() =
        runTest {
            val viewModel = opened(declaredToTheProfessional())

            viewModel.onAnswerRequested(PaymentAnswer.RECEIVED)
            viewModel.onAnswerConfirmed()
            advanceUntilIdle()

            val state = viewModel.content()
            assertEquals(listOf(PaymentAnswer.RECEIVED), repository.answers)
            assertNull(state.answerDraft)
            assertEquals(PaymentStatus.BOTH_CONFIRMED, state.attention.payment.status)
            assertFalse(state.attention.canAnswerPayment)
        }

    @Test
    fun sayingItWasNotReceivedLeavesThePaymentInReview() =
        runTest {
            val viewModel = opened(declaredToTheProfessional())

            viewModel.onAnswerRequested(PaymentAnswer.NOT_RECEIVED)
            viewModel.onAnswerConfirmed()
            advanceUntilIdle()

            assertEquals(listOf(PaymentAnswer.NOT_RECEIVED), repository.answers)
            assertEquals(
                PaymentStatus.DISPUTED,
                viewModel
                    .content()
                    .attention.payment.status,
            )
        }

    @Test
    fun aFailedAnswerKeepsTheDialogWithItsAnswer() =
        runTest {
            repository.answerError = ServiceError.NetworkUnavailable
            val viewModel = opened(declaredToTheProfessional())

            viewModel.onAnswerRequested(PaymentAnswer.NOT_RECEIVED)
            viewModel.onAnswerConfirmed()
            advanceUntilIdle()

            val draft = viewModel.content().answerDraft!!
            assertEquals(PaymentAnswer.NOT_RECEIVED, draft.answer)
            assertEquals(ServiceError.NetworkUnavailable, draft.error)
            assertFalse(draft.sending)
        }

    // Answered from another phone first: the dialog closes on what the engine says.
    @Test
    fun aPaymentAnsweredElsewhereClosesTheDialog() =
        runTest {
            val viewModel = opened(declaredToTheProfessional())
            viewModel.onAnswerRequested(PaymentAnswer.RECEIVED)

            repository.answerError = ServiceError.AttentionChanged
            repository.result =
                AttentionResult.Loaded(
                    declaredToTheProfessional().copy(payment = paidInCash().copy(status = PaymentStatus.DISPUTED)),
                )
            viewModel.onAnswerConfirmed()
            advanceUntilIdle()

            assertNull(viewModel.content().answerDraft)
            assertEquals(
                PaymentStatus.DISPUTED,
                viewModel
                    .content()
                    .attention.payment.status,
            )
        }

    @Test
    fun anAnswerOnItsWayCannotBeDismissedOrSentTwice() =
        runTest {
            repository.holdAnswer = CompletableDeferred()
            val viewModel = opened(declaredToTheProfessional())
            viewModel.onAnswerRequested(PaymentAnswer.RECEIVED)

            viewModel.onAnswerConfirmed()
            advanceUntilIdle()
            viewModel.onAnswerDismissed()
            viewModel.onAnswerConfirmed()
            advanceUntilIdle()

            assertTrue(viewModel.content().answerDraft!!.sending)
            assertEquals(1, repository.answers.size)
            repository.holdAnswer!!.complete(Unit)
            advanceUntilIdle()
            assertNull(viewModel.content().answerDraft)
        }
}

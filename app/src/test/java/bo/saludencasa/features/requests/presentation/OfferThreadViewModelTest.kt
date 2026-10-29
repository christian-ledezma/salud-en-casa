@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.requests.FakeOfferRepository
import bo.saludencasa.features.requests.domain.model.OfferActionResult
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferStatus
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferThreadResult
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.usecase.GetOfferThreadUseCase
import bo.saludencasa.features.requests.domain.usecase.GetOfferableRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveOfferSignalsUseCase
import bo.saludencasa.features.requests.domain.usecase.RejectOfferUseCase
import bo.saludencasa.features.requests.domain.usecase.SubmitCounterOfferUseCase
import bo.saludencasa.features.requests.domain.usecase.SubmitOfferUseCase
import bo.saludencasa.features.requests.offerThread
import bo.saludencasa.features.requests.offerableRequest
import bo.saludencasa.features.requests.requestOffer
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
import java.math.BigDecimal

class OfferThreadViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val offers = FakeOfferRepository()

    private fun viewModel(professionalId: String? = "professional-1"): OfferThreadViewModel =
        OfferThreadViewModel(
            requestId = "r1",
            professionalId = professionalId,
            getOfferThread = GetOfferThreadUseCase(offers),
            getOfferableRequest = GetOfferableRequestUseCase(offers),
            submitOffer = SubmitOfferUseCase(offers),
            submitCounterOffer = SubmitCounterOfferUseCase(offers),
            rejectOffer = RejectOfferUseCase(offers),
            observeOfferSignals = ObserveOfferSignalsUseCase(offers),
        )

    private fun TestScope.opened(professionalId: String? = "professional-1"): OfferThreadViewModel {
        val viewModel = viewModel(professionalId)
        advanceUntilIdle()
        return viewModel
    }

    private fun content(viewModel: OfferThreadViewModel) = viewModel.uiState.value as OfferThreadUiState.Content

    // The professional's offer is on the table, so it is the patient's turn.
    private fun theirTurnThread() =
        offerThread(offers = listOf(requestOffer(id = "o1", issuer = OfferIssuer.PROFESSIONAL)))

    // HT-24's criterion 3: from the inbox or the notification, the professional
    // with no thread yet sees the request and the form of the first offer.
    @Test
    fun theProfessionalWithoutAThreadSeesTheFirstOfferForm() =
        runTest {
            offers.threadResult = OfferThreadResult.NotFound
            offers.offerableResult = OfferableRequestResult.Loaded(offerableRequest(id = "r1"))

            val state = opened(professionalId = null).uiState.value

            assertEquals(OfferThreadUiState.Opening(offerableRequest(id = "r1")), state)
        }

    // Sending the first offer turns the same screen into the negotiation, with
    // the request still on top: no "offer sent", no trip back to the inbox.
    @Test
    fun sendingTheFirstOfferTurnsTheScreenIntoTheNegotiation() =
        runTest {
            offers.threadResult = OfferThreadResult.NotFound
            offers.offerableResult = OfferableRequestResult.Loaded(offerableRequest(id = "r1"))
            val viewModel = opened(professionalId = null)

            viewModel.onAmountChanged("160")
            offers.threadResult = OfferThreadResult.Loaded(offerThread(iAmTheProfessional = true))
            viewModel.onSubmit()
            advanceUntilIdle()

            val submission = offers.submitted.single()
            assertNull(submission.parentOfferId)
            assertNull(submission.message)
            val state = content(viewModel)
            assertEquals(offerableRequest(id = "r1"), state.request)
            assertFalse(state.thread.isMyTurn)
        }

    // The offer is made; if reading the thread then fails, the screen says so
    // and offers to retry, instead of leaving the form up with its button off.
    @Test
    fun aFirstOfferWhoseThreadCannotBeReadDoesNotLeaveTheFormStuck() =
        runTest {
            offers.threadResult = OfferThreadResult.NotFound
            offers.offerableResult = OfferableRequestResult.Loaded(offerableRequest(id = "r1"))
            val viewModel = opened(professionalId = null)

            viewModel.onAmountChanged("160")
            offers.threadResult = OfferThreadResult.Failure(OfferError.NetworkUnavailable)
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(OfferThreadUiState.Failed(OfferError.NetworkUnavailable), viewModel.uiState.value)
        }

    // The professional on the first-offer form already listens on their side,
    // so the patient's answer to it is not missed.
    @Test
    fun theFirstOfferFormListensOnTheProfessionalsSide() =
        runTest {
            offers.threadResult = OfferThreadResult.NotFound
            offers.offerableResult = OfferableRequestResult.Loaded(offerableRequest(id = "r1"))

            opened(professionalId = null)

            assertEquals(listOf(true), offers.signalSides)
        }

    // A refusal means the screen was behind the engine: the thread is read again,
    // with the error still on screen.
    @Test
    fun aRefusedCounterOfferReadsTheThreadAgain() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            offers.submitResult = OfferSubmitResult.Failure(OfferError.NotAllowed)
            val viewModel = opened()

            viewModel.onCounterClicked()
            viewModel.onAmountChanged("140")
            val rejected = theirTurnThread().offers.single().copy(status = OfferStatus.REJECTED)
            offers.threadResult = OfferThreadResult.Loaded(offerThread(offers = listOf(rejected)))
            viewModel.onSubmit()
            advanceUntilIdle()

            val state = content(viewModel)
            assertTrue(state.thread.isClosed)
            assertEquals(OfferError.NotAllowed, state.error)
        }

    @Test
    fun aRefusedFirstOfferKeepsTheFormWithItsError() =
        runTest {
            offers.threadResult = OfferThreadResult.NotFound
            offers.offerableResult = OfferableRequestResult.Loaded(offerableRequest(id = "r1"))
            offers.submitResult = OfferSubmitResult.Failure(OfferError.NotAllowed)
            val viewModel = opened(professionalId = null)

            viewModel.onAmountChanged("160")
            viewModel.onSubmit()
            advanceUntilIdle()

            val state = viewModel.uiState.value as OfferThreadUiState.Opening
            assertEquals(OfferError.NotAllowed, state.error)
            assertEquals("160", state.amount)
            assertFalse(state.submitting)
        }

    // The request may be gone by the time the notification is tapped: the screen
    // says so instead of offering a form the engine would refuse.
    @Test
    fun aRequestThatCanNoLongerBeOfferedOnShowsGone() =
        runTest {
            offers.threadResult = OfferThreadResult.NotFound
            offers.offerableResult = OfferableRequestResult.NotFound

            assertEquals(OfferThreadUiState.Gone, opened(professionalId = null).uiState.value)
        }

    // The thread the patient answers carries their floor: the screen hears about
    // it before anything leaves.
    @Test
    fun aCounterBelowThePatientsFloorIsRefusedOnScreen() =
        runTest {
            offers.threadResult =
                OfferThreadResult.Loaded(theirTurnThread().copy(suggestedBudgetBob = BigDecimal("130.00")))
            val viewModel = opened()

            viewModel.onCounterClicked()
            viewModel.onAmountChanged("120")
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(OfferError.BelowMinimum(BigDecimal("130.00")), content(viewModel).error)
            assertTrue(offers.submitted.isEmpty())
        }

    @Test
    fun theOfferOnTheTableCanBeAnswered() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())

            val viewModel = opened()

            assertTrue(content(viewModel).canAnswer)
            assertTrue(content(viewModel).thread.isMyTurn)
        }

    // My own last offer means I am the one waiting: nothing to answer.
    @Test
    fun myOwnLastOfferCannotBeAnswered() =
        runTest {
            offers.threadResult =
                OfferThreadResult.Loaded(
                    offerThread(
                        offers =
                            listOf(
                                requestOffer(id = "o1", issuer = OfferIssuer.PROFESSIONAL),
                                requestOffer(id = "o2", parentOfferId = "o1", issuer = OfferIssuer.PATIENT),
                            ),
                    ),
                )

            val viewModel = opened()

            assertFalse(content(viewModel).canAnswer)
        }

    // A rejection closes the thread, and the engine would refuse anything else:
    // the screen offers nothing either.
    @Test
    fun aRejectedThreadCannotBeAnswered() =
        runTest {
            offers.threadResult =
                OfferThreadResult.Loaded(
                    offerThread(
                        offers =
                            listOf(
                                requestOffer(
                                    id = "o1",
                                    issuer = OfferIssuer.PROFESSIONAL,
                                    status = OfferStatus.REJECTED,
                                ),
                            ),
                    ),
                )

            val viewModel = opened()

            assertTrue(content(viewModel).thread.isClosed)
            assertFalse(content(viewModel).canAnswer)
        }

    @Test
    fun aThreadThatIsNotVisibleShowsGone() =
        runTest {
            offers.threadResult = OfferThreadResult.NotFound

            assertEquals(OfferThreadUiState.Gone, opened().uiState.value)
        }

    // RF-08.2: the counter-offer answers the offer on the table, on the thread of
    // that professional.
    @Test
    fun aCounterOfferAnswersTheOfferOnTheTable() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            val viewModel = opened()

            viewModel.onCounterClicked()
            viewModel.onAmountChanged("140")
            viewModel.onMessageChanged("Puedo por este monto")
            viewModel.onSubmit()
            advanceUntilIdle()

            val submission = offers.submitted.single()
            assertEquals("o1", submission.parentOfferId)
            assertEquals("professional-1", submission.professionalId)
            assertFalse(content(viewModel).countering)
        }

    @Test
    fun aRefusedCounterOfferKeepsWhatWasTyped() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            offers.submitResult = OfferSubmitResult.Failure(OfferError.NotAllowed)
            val viewModel = opened()

            viewModel.onCounterClicked()
            viewModel.onAmountChanged("140")
            viewModel.onMessageChanged("Puedo por este monto")
            viewModel.onSubmit()
            advanceUntilIdle()

            val state = content(viewModel)
            assertEquals(OfferError.NotAllowed, state.error)
            assertTrue(state.countering)
            assertEquals("140", state.amount)
        }

    // Criterion 4: rejecting is confirmed before anything is written.
    @Test
    fun rejectingIsConfirmedBeforeItIsWritten() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            val viewModel = opened()

            viewModel.onRejectClicked()
            advanceUntilIdle()
            assertTrue(content(viewModel).confirmingRejection)
            assertTrue(offers.rejected.isEmpty())

            viewModel.onRejectConfirmed()
            advanceUntilIdle()
            assertEquals(listOf("o1"), offers.rejected)
        }

    @Test
    fun aDismissedConfirmationWritesNothing() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            val viewModel = opened()

            viewModel.onRejectClicked()
            viewModel.onRejectDismissed()
            advanceUntilIdle()

            assertFalse(content(viewModel).confirmingRejection)
            assertTrue(offers.rejected.isEmpty())
        }

    // The confirmation is not a licence: out of turn nothing is written, so a
    // stale screen cannot reject what is no longer on the table.
    @Test
    fun rejectingOutOfTurnWritesNothing() =
        runTest {
            offers.threadResult =
                OfferThreadResult.Loaded(
                    offerThread(
                        offers =
                            listOf(
                                requestOffer(id = "o1", issuer = OfferIssuer.PROFESSIONAL),
                                requestOffer(id = "o2", parentOfferId = "o1", issuer = OfferIssuer.PATIENT),
                            ),
                    ),
                )
            val viewModel = opened()

            viewModel.onRejectClicked()
            viewModel.onRejectConfirmed()
            advanceUntilIdle()

            assertTrue(offers.rejected.isEmpty())
        }

    // The refusal is reported as such. That a policy which updates no row is a
    // refusal and not a success is the repository's doing, and the engine's
    // experiment `nobody_rejects_their_own_offer` is what proves the row count.
    @Test
    fun aRefusedRejectionIsReported() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            offers.rejectResult = OfferActionResult.Failure(OfferError.NotAllowed)
            val viewModel = opened()

            viewModel.onRejectClicked()
            viewModel.onRejectConfirmed()
            advanceUntilIdle()

            assertEquals(OfferError.NotAllowed, content(viewModel).error)
        }

    // Each side listens on its own topic: the professional's thread is read with
    // no professionalId in the route, and that is the side that listens.
    @Test
    fun theProfessionalListensOnTheirOwnTopic() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(offerThread(iAmTheProfessional = true))

            opened(professionalId = null)

            assertEquals(listOf(true), offers.signalSides)
        }

    @Test
    fun thePatientListensOnTheirOwnTopic() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())

            opened()

            assertEquals(listOf(false), offers.signalSides)
        }

    // A signal landing while the counter-offer is in flight must not re-enable
    // the button: a second tap would send a second offer on the same turn.
    @Test
    fun aSignalWhileSubmittingDoesNotReEnableTheForm() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            offers.holdSubmit = CompletableDeferred()
            val viewModel = opened()

            viewModel.onCounterClicked()
            viewModel.onAmountChanged("140")
            viewModel.onMessageChanged("Puedo por este monto")
            viewModel.onSubmit()
            advanceUntilIdle()
            assertTrue(content(viewModel).submitting)

            offers.offerSignals.emit(Unit)
            advanceUntilIdle()

            val state = content(viewModel)
            assertTrue(state.submitting)
            assertFalse(state.canSubmit)
            assertEquals("140", state.amount)

            offers.holdSubmit?.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, offers.submitted.size)
        }

    // A read nobody asked for that fails does not take the screen away, nor the
    // form on top of it.
    @Test
    fun aFailedSignalReloadKeepsTheThreadAndTheForm() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            val viewModel = opened()
            viewModel.onCounterClicked()
            viewModel.onAmountChanged("140")

            offers.threadResult = OfferThreadResult.Failure(OfferError.NetworkUnavailable)
            offers.offerSignals.emit(Unit)
            advanceUntilIdle()

            val state = content(viewModel)
            assertTrue(state.countering)
            assertEquals("140", state.amount)
        }

    // Asking for the thread again, on the other hand, does report the failure.
    @Test
    fun anExplicitRefreshThatFailsIsReported() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            val viewModel = opened()

            offers.threadResult = OfferThreadResult.Failure(OfferError.NetworkUnavailable)
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(OfferThreadUiState.Failed(OfferError.NetworkUnavailable), viewModel.uiState.value)
        }

    // Criterion 3: the answer appears without a reload, and a signal landing
    // while the form is open does not wipe what was typed.
    @Test
    fun aSignalRefreshesTheThreadWithoutLosingWhatIsTyped() =
        runTest {
            offers.threadResult = OfferThreadResult.Loaded(theirTurnThread())
            val viewModel = opened()
            viewModel.onCounterClicked()
            viewModel.onAmountChanged("140")

            viewModel.uiState.test {
                assertEquals("140", (awaitItem() as OfferThreadUiState.Content).amount)

                offers.threadResult =
                    OfferThreadResult.Loaded(
                        offerThread(
                            offers =
                                listOf(
                                    requestOffer(id = "o1", issuer = OfferIssuer.PROFESSIONAL),
                                    requestOffer(id = "o2", parentOfferId = "o1", issuer = OfferIssuer.PROFESSIONAL),
                                ),
                        ),
                    )
                offers.offerSignals.emit(Unit)

                val next = awaitItem() as OfferThreadUiState.Content
                assertEquals(2, next.thread.offers.size)
                assertEquals("140", next.amount)
                cancelAndIgnoreRemainingEvents()
            }
        }
}

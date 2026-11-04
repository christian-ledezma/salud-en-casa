@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.usecase.GetServiceTypeCatalogUseCase
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.venousLine
import bo.saludencasa.features.location.FakeAddressRepository
import bo.saludencasa.features.location.address
import bo.saludencasa.features.location.coordinate
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.requests.FakeOfferRepository
import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.ActiveRequestResult
import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.model.OfferActionResult
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferStatus
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.OfferThreadsResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.usecase.AcceptOfferUseCase
import bo.saludencasa.features.requests.domain.usecase.CancelRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.CreateImmediateRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.FindProfessionalsAvailableNowUseCase
import bo.saludencasa.features.requests.domain.usecase.GetActiveImmediateRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetOfferThreadsUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveOfferSignalsUseCase
import bo.saludencasa.features.requests.offerThread
import bo.saludencasa.features.requests.requestOffer
import bo.saludencasa.features.requests.serviceRequest
import bo.saludencasa.features.search.FakeProfessionalSearchRepository
import bo.saludencasa.features.search.domain.model.SearchError
import bo.saludencasa.features.search.domain.usecase.SearchNearbyProfessionalsUseCase
import bo.saludencasa.features.search.nearbyProfessional
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImmediateCareViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val office =
        address(id = "a2", alias = "Oficina", isPrimary = false, coordinate = coordinate(-17.39, -66.15))
    private val home = address(id = "a1", alias = "Casa", isPrimary = true, coordinate = coordinate(-17.38, -66.16))

    private val catalog = FakeCatalogRepository(catalog = listOf(generalConsultation, venousLine))
    private val addresses = FakeAddressRepository(listResult = AddressListResult.Success(listOf(office, home)))
    private val requests = FakeRequestRepository()
    private val offers = FakeOfferRepository()
    private val search = FakeProfessionalSearchRepository(professionals = listOf(nearbyProfessional(id = "p1")))

    private val published = serviceRequest(id = "published", serviceTypeName = venousLine.name)

    private fun viewModel(): ImmediateCareViewModel =
        ImmediateCareViewModel(
            GetServiceTypeCatalogUseCase(catalog),
            GetMyAddressesUseCase(addresses),
            GetActiveImmediateRequestUseCase(requests),
            CreateImmediateRequestUseCase(requests),
            CancelRequestUseCase(requests),
            FindProfessionalsAvailableNowUseCase(SearchNearbyProfessionalsUseCase(search)),
            GetOfferThreadsUseCase(offers),
            ObserveOfferSignalsUseCase(offers),
            AcceptOfferUseCase(offers),
        )

    private fun TestScope.opened(): ImmediateCareViewModel {
        val viewModel = viewModel()
        viewModel.refresh()
        advanceUntilIdle()
        return viewModel
    }

    private fun form(viewModel: ImmediateCareViewModel): ImmediateCareUiState.Form =
        viewModel.uiState.value as ImmediateCareUiState.Form

    private fun TestScope.publishedFrom(viewModel: ImmediateCareViewModel) {
        viewModel.onServiceTypeSelected(venousLine.id)
        viewModel.onDescriptionChanged("Necesito una vía para el suero")
        requests.activeResult = ActiveRequestResult.Found(published)
        viewModel.onSubmit()
        advanceUntilIdle()
    }

    // HT-24: coming back from «Agregar otra dirección» shows the new address and
    // chooses it, and nothing else the patient had filled in is lost.
    @Test
    fun comingBackWithANewAddressChoosesItAndKeepsTheForm() =
        runTest {
            val viewModel = opened()
            viewModel.onServiceTypeSelected(venousLine.id)
            viewModel.onDescriptionChanged("Necesito una vía para el suero")

            val newHome =
                address(id = "a3", alias = "Casa nueva", isPrimary = false, coordinate = coordinate(-17.40, -66.14))
            addresses.listResult = AddressListResult.Success(listOf(office, home, newHome))
            viewModel.refresh()
            advanceUntilIdle()

            val state = form(viewModel)
            assertEquals("a3", state.selectedAddressId)
            assertEquals(venousLine.id, state.selectedServiceTypeId)
            assertEquals("Necesito una vía para el suero", state.description)
            // The map now shows who is available around the new address.
            assertEquals(newHome.coordinate, search.requests.last().origin)
        }

    // Coming back from a thread lands on the wait with the same request: its
    // cards stay even if reading the offers again fails.
    @Test
    fun comingBackToTheWaitKeepsTheCardsWhenTheReadFails() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)
            offers.threadsResult = OfferThreadsResult.Loaded(listOf(offerThread(professionalId = "p1")))
            val viewModel = opened()

            offers.threadsResult = OfferThreadsResult.Failure(OfferError.NetworkUnavailable)
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(listOf("p1"), waitingThreads(viewModel).map { it.professionalId })
        }

    // A read of the active request that fails on coming back says nothing about
    // the wait on screen, which stays.
    @Test
    fun aFailedReadOnComingBackKeepsTheWait() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)
            offers.threadsResult = OfferThreadsResult.Loaded(listOf(offerThread(professionalId = "p1")))
            val viewModel = opened()

            requests.activeResult = ActiveRequestResult.Failure(RequestError.NetworkUnavailable)
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(listOf("p1"), waitingThreads(viewModel).map { it.professionalId })
        }

    // HT-23: coming back with a request still open shows that request. A blank
    // form here is how a patient ends up publishing the same need twice.
    @Test
    fun aPatientWithAnOpenImmediateRequestSeesItInsteadOfANewForm() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)

            val viewModel = opened()

            assertEquals(ImmediateCareUiState.Waiting(published, false, null), viewModel.uiState.value)
            assertTrue(search.requests.isEmpty())
        }

    // Arriving at the wait shows the offers that already landed, as cards.
    @Test
    fun enteringTheWaitShowsTheOffersThatAlreadyExist() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)
            offers.threadsResult = OfferThreadsResult.Loaded(listOf(offerThread(professionalId = "p1")))

            val viewModel = opened()

            assertEquals(listOf("p1"), waitingThreads(viewModel).map { it.professionalId })
        }

    // HT-24's criterion 1: an offer landing shows up by itself while waiting,
    // with no reload.
    @Test
    fun anOfferThatLandsWhileWaitingAppearsFromTheSignal() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)
            val viewModel = opened()
            assertTrue(waitingThreads(viewModel).isEmpty())

            offers.threadsResult =
                OfferThreadsResult.Loaded(
                    listOf(offerThread(professionalId = "p1"), offerThread(professionalId = "p2")),
                )
            offers.offerSignals.emit(Unit)
            advanceUntilIdle()

            assertEquals(listOf("p1", "p2"), waitingThreads(viewModel).map { it.professionalId })
        }

    // A rejected negotiation leaves the wait, as a declined driver leaves the
    // list in InDrive; it is still in its thread.
    @Test
    fun aRejectedNegotiationLeavesTheWait() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)
            offers.threadsResult =
                OfferThreadsResult.Loaded(
                    listOf(
                        offerThread(professionalId = "p1"),
                        offerThread(
                            professionalId = "p2",
                            offers = listOf(requestOffer(id = "o9", status = OfferStatus.REJECTED)),
                        ),
                    ),
                )

            val viewModel = opened()

            assertEquals(listOf("p1"), waitingThreads(viewModel).map { it.professionalId })
        }

    private fun waitingThreads(viewModel: ImmediateCareViewModel) = waiting(viewModel).threads

    private fun waiting(viewModel: ImmediateCareViewModel) = viewModel.uiState.value as ImmediateCareUiState.Waiting

    private fun threadOffering(
        offerId: String,
        professionalId: String = "p1",
    ) = offerThread(professionalId = professionalId, offers = listOf(requestOffer(id = offerId)))

    private fun TestScope.waitingWith(vararg threads: OfferThread): ImmediateCareViewModel {
        requests.activeResult = ActiveRequestResult.Found(published)
        offers.threadsResult = OfferThreadsResult.Loaded(threads.toList())
        return opened()
    }

    // HT-28: arriving from a card that says «2 ofertas», the screen must not say it
    // is still looking for a professional while the offers are on their way.
    @Test
    fun theWaitDoesNotClaimNobodyOfferedBeforeTheFirstReadAnswers() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)
            offers.threadsResult = OfferThreadsResult.Loaded(listOf(threadOffering("o1")))
            val gate = CompletableDeferred<Unit>()
            offers.holdThreads = gate

            val viewModel = opened()
            assertFalse(waiting(viewModel).offersRead)

            gate.complete(Unit)
            advanceUntilIdle()

            assertTrue(waiting(viewModel).offersRead)
            assertEquals(listOf("p1"), waitingThreads(viewModel).map { it.professionalId })
        }

    // A first read that fails has nothing to wait for any more.
    @Test
    fun aFailedFirstReadStillLeavesTheWaitOnScreen() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)
            offers.threadsResult = OfferThreadsResult.Failure(OfferError.NetworkUnavailable)

            val viewModel = opened()

            assertTrue(waiting(viewModel).offersRead)
        }

    // An offer landing during the first read is not lost: the screen is already
    // listening, so it reads again.
    @Test
    fun anOfferThatLandsDuringTheFirstReadIsReadAgain() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)
            val gate = CompletableDeferred<Unit>()
            offers.holdThreads = gate
            val viewModel = opened()
            val readsBefore = offers.threadReads

            offers.offerSignals.emit(Unit)
            advanceUntilIdle()

            assertEquals(readsBefore + 1, offers.threadReads)
            offers.threadsResult = OfferThreadsResult.Loaded(listOf(threadOffering("o1")))
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf("p1"), waitingThreads(viewModel).map { it.professionalId })
        }

    // The dialog says an amount, and that amount is what gets frozen. A signal that
    // replaces the list while it is open must not move the acceptance to another offer.
    @Test
    fun theOfferAcceptedIsTheOneTheDialogNamedEvenIfASignalReplacedTheList() =
        runTest {
            val named = threadOffering("o1")
            val viewModel = waitingWith(named)
            viewModel.onAcceptClicked(named)

            offers.threadsResult = OfferThreadsResult.Loaded(listOf(threadOffering("o2")))
            offers.offerSignals.emit(Unit)
            advanceUntilIdle()
            assertEquals(named, waiting(viewModel).confirmingAcceptance)

            viewModel.onAcceptConfirmed()
            advanceUntilIdle()

            assertEquals(listOf("o1"), offers.accepted)
        }

    @Test
    fun confirmingMovesOnToTheAttentionOfThatRequest() =
        runTest {
            val thread = threadOffering("o1")
            val viewModel = waitingWith(thread)
            viewModel.onAcceptClicked(thread)

            viewModel.onAcceptConfirmed()
            advanceUntilIdle()

            assertEquals(ImmediateCareUiState.Accepted(published.id), viewModel.uiState.value)
        }

    // Accepting closes the request, so the read of a late resume finds none open. It
    // must not bring the form back, which invites publishing the same need again.
    @Test
    fun aResumeAfterAcceptingNeverBringsTheFormBack() =
        runTest {
            val thread = threadOffering("o1")
            val viewModel = waitingWith(thread)
            viewModel.onAcceptClicked(thread)
            viewModel.onAcceptConfirmed()
            advanceUntilIdle()

            requests.activeResult = ActiveRequestResult.None
            viewModel.refresh()
            offers.offerSignals.emit(Unit)
            advanceUntilIdle()

            assertEquals(ImmediateCareUiState.Accepted(published.id), viewModel.uiState.value)
        }

    // The same, for a read that left before the acceptance and answers after it.
    @Test
    fun aReadStillTravellingWhenTheAcceptanceLandsNeverBringsTheFormBack() =
        runTest {
            val thread = threadOffering("o1")
            val viewModel = waitingWith(thread)
            viewModel.onAcceptClicked(thread)

            requests.activeResult = ActiveRequestResult.None
            val gate = CompletableDeferred<Unit>()
            requests.activeGate = gate
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onAcceptConfirmed()
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(ImmediateCareUiState.Accepted(published.id), viewModel.uiState.value)
        }

    // An offer read while the cancellation travelled is still the offer on the table:
    // a failed cancellation must not put the older amount back on a card that accepts.
    @Test
    fun aFailedCancellationKeepsTheOffersReadWhileItTravelled() =
        runTest {
            val viewModel = waitingWith(threadOffering("o1"))
            val gate = CompletableDeferred<Unit>()
            requests.cancelGate = gate
            requests.cancelResult = CancelRequestResult.Failure(RequestError.NetworkUnavailable)
            viewModel.onCancel()
            advanceUntilIdle()

            offers.threadsResult = OfferThreadsResult.Loaded(listOf(threadOffering("o2")))
            offers.offerSignals.emit(Unit)
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf("o2"), waitingThreads(viewModel).map { it.lastOffer?.id })
        }

    // Closing the dialog is not an answer to the offer.
    @Test
    fun dismissingTheConfirmationAcceptsNothing() =
        runTest {
            val thread = threadOffering("o1")
            val viewModel = waitingWith(thread)
            viewModel.onAcceptClicked(thread)

            viewModel.onAcceptDismissed()
            viewModel.onAcceptConfirmed()
            advanceUntilIdle()

            assertTrue(offers.accepted.isEmpty())
            assertEquals(null, waiting(viewModel).confirmingAcceptance)
        }

    // An offer the patient wrote is the professional's to answer: the engine would
    // refuse it, and the dialog must not get that far.
    @Test
    fun anOfferThatIsNotThePatientsTurnCannotBeConfirmed() =
        runTest {
            val mine =
                offerThread(
                    professionalId = "p1",
                    offers = listOf(requestOffer(id = "o1", issuer = OfferIssuer.PATIENT)),
                )
            val viewModel = waitingWith(mine)

            viewModel.onAcceptClicked(mine)

            assertEquals(null, waiting(viewModel).confirmingAcceptance)
        }

    // Two taps must not send two acceptances, and the request cannot be cancelled
    // while it is being accepted.
    @Test
    fun whileAcceptingNothingElseCanBeSent() =
        runTest {
            val thread = threadOffering("o1")
            val viewModel = waitingWith(thread)
            val gate = CompletableDeferred<Unit>()
            offers.holdAccept = gate
            viewModel.onAcceptClicked(thread)
            viewModel.onAcceptConfirmed()
            advanceUntilIdle()
            assertTrue(waiting(viewModel).accepting)

            viewModel.onAcceptClicked(thread)
            viewModel.onAcceptConfirmed()
            viewModel.onCancel()
            advanceUntilIdle()

            assertEquals(listOf("o1"), offers.accepted)
            assertTrue(requests.cancelledIds.isEmpty())
            gate.complete(Unit)
        }

    // The offer changed under the card: the screen says so and shows the current one.
    @Test
    fun aRefusedAcceptanceSaysSoAndReadsTheOffersAgain() =
        runTest {
            val thread = threadOffering("o1")
            val viewModel = waitingWith(thread)
            offers.acceptResult = OfferActionResult.Failure(OfferError.NotAllowed)
            offers.threadsResult = OfferThreadsResult.Loaded(listOf(threadOffering("o2")))
            viewModel.onAcceptClicked(thread)

            viewModel.onAcceptConfirmed()
            advanceUntilIdle()

            val state = waiting(viewModel)
            assertEquals(OfferError.NotAllowed, state.acceptError)
            assertFalse(state.accepting)
            assertEquals(listOf("o2"), state.threads.map { it.lastOffer?.id })
        }

    // Without a connection nothing changed: the same offer stays, ready to retry.
    @Test
    fun aFailedConnectionKeepsTheOfferAndAllowsARetry() =
        runTest {
            val thread = threadOffering("o1")
            val viewModel = waitingWith(thread)
            offers.acceptResult = OfferActionResult.Failure(OfferError.NetworkUnavailable)
            viewModel.onAcceptClicked(thread)
            viewModel.onAcceptConfirmed()
            advanceUntilIdle()

            assertEquals(OfferError.NetworkUnavailable, waiting(viewModel).acceptError)
            assertEquals(listOf(thread), waitingThreads(viewModel))

            viewModel.onAcceptClicked(thread)

            assertEquals(null, waiting(viewModel).acceptError)
            assertEquals(thread, waiting(viewModel).confirmingAcceptance)
        }

    @Test
    fun withoutAnAddressTheStateIsNoAddress() =
        runTest {
            addresses.listResult = AddressListResult.Success(emptyList())

            val viewModel = opened()

            assertEquals(ImmediateCareUiState.NoAddress, viewModel.uiState.value)
        }

    @Test
    fun aCatalogThatCannotBeReadIsReportedAsFailed() =
        runTest {
            catalog.readFailure = CatalogError.NetworkUnavailable

            val viewModel = opened()

            assertEquals(ImmediateCareUiState.Failed(RequestError.NetworkUnavailable), viewModel.uiState.value)
        }

    // The primary address is offered first even when it is not the first row,
    // and the map looks around it before any service is chosen.
    @Test
    fun theFormStartsOnThePrimaryAddressAndTheMapLooksAroundIt() =
        runTest {
            val viewModel = opened()

            assertEquals(home.id, form(viewModel).selectedAddressId)
            assertEquals(home.coordinate, search.requests.single().origin)
            assertEquals(
                listOf("p1"),
                (form(viewModel).nearby as NearbyState.Found).professionals.map { it.professionalId },
            )
        }

    // The request is published around the chosen address, so the people on the
    // map have to be the ones around that address and not around the home.
    @Test
    fun choosingAnotherAddressMovesTheSearchThere() =
        runTest {
            val viewModel = opened()

            viewModel.onAddressSelected(office.id)
            advanceUntilIdle()

            assertEquals(office.coordinate, search.requests.last().origin)
        }

    @Test
    fun choosingTheServiceNarrowsTheMapToThoseWhoProvideIt() =
        runTest {
            val viewModel = opened()

            viewModel.onServiceTypeSelected(venousLine.id)
            advanceUntilIdle()

            assertEquals(
                venousLine.id,
                search.requests
                    .last()
                    .criteria.serviceTypeId,
            )
            assertFalse(form(viewModel).typeListExpanded)
        }

    // A reply about the address the patient just left, landing last, would put
    // on the map people who do not answer the question on screen.
    @Test
    fun aReplyAboutTheAddressJustLeftNeverOverwritesTheCurrentOne() =
        runTest {
            val viewModel = opened()
            val firstGate = CompletableDeferred<Unit>()
            search.gate = firstGate
            search.responder = { request ->
                if (request.origin == home.coordinate) {
                    listOf(nearbyProfessional(id = "near-home"))
                } else {
                    listOf(nearbyProfessional(id = "near-office"))
                }
            }
            viewModel.onServiceTypeSelected(venousLine.id)
            advanceUntilIdle()

            search.gate = null
            viewModel.onAddressSelected(office.id)
            advanceUntilIdle()
            firstGate.complete(Unit)
            advanceUntilIdle()

            val nearby = form(viewModel).nearby as NearbyState.Found
            assertEquals(listOf("near-office"), nearby.professionals.map { it.professionalId })
        }

    // Availability changes in minutes and the request lasts longer than that:
    // an empty map must not stand between the patient and publishing.
    @Test
    fun nobodyAvailableNearbyStillLetsThePatientPublish() =
        runTest {
            search.professionals = emptyList()
            val viewModel = opened()

            viewModel.onServiceTypeSelected(venousLine.id)
            advanceUntilIdle()

            assertEquals(NearbyState.Found(emptyList(), more = false), form(viewModel).nearby)
            assertTrue(form(viewModel).canSubmit)
        }

    @Test
    fun aMapThatCannotBeReadLeavesTheFormUsable() =
        runTest {
            search.failure = SearchError.NetworkUnavailable
            val viewModel = opened()

            viewModel.onServiceTypeSelected(venousLine.id)
            advanceUntilIdle()

            assertEquals(NearbyState.Unavailable, form(viewModel).nearby)
            assertTrue(form(viewModel).canSubmit)
        }

    @Test
    fun theSubmitButtonIsHeldUntilAServiceTypeIsChosen() =
        runTest {
            val viewModel = opened()

            assertFalse(form(viewModel).canSubmit)
        }

    // Publishing stays on this screen, which becomes the wait for that request.
    @Test
    fun publishingTurnsTheScreenIntoTheWaitForThatRequest() =
        runTest {
            val viewModel = opened()

            publishedFrom(viewModel)

            assertEquals(ImmediateCareUiState.Waiting(published, false, null), viewModel.uiState.value)
            val draft = requests.createdDrafts.single()
            assertEquals(venousLine.id, draft.serviceTypeId)
            assertEquals(home.id, draft.addressId)
        }

    // The request exists by then. Bringing the form back because the read-back
    // failed invites the patient to publish it a second time.
    @Test
    fun aRequestPublishedButNotReadBackNeverBringsTheFormBack() =
        runTest {
            val viewModel = opened()
            viewModel.onServiceTypeSelected(venousLine.id)
            requests.activeResult = ActiveRequestResult.Failure(RequestError.NetworkUnavailable)

            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(ImmediateCareUiState.Failed(RequestError.NetworkUnavailable), viewModel.uiState.value)
        }

    @Test
    fun aRejectedBudgetKeepsTheFormAndCarriesTheReason() =
        runTest {
            requests.createResult = RequestResult.Failure(RequestError.InvalidBudget)
            val viewModel = opened()
            viewModel.onServiceTypeSelected(venousLine.id)
            viewModel.onBudgetChanged("-5")

            viewModel.onSubmit()
            advanceUntilIdle()

            val form = form(viewModel)
            assertEquals(RequestError.InvalidBudget, form.submitError)
            assertEquals("-5", form.budget)
            assertFalse(form.submitting)
        }

    // Cancelling to change one detail must not make the patient fill it all in
    // again.
    @Test
    fun cancellingBringsBackTheSameChoices() =
        runTest {
            val viewModel = opened()
            publishedFrom(viewModel)
            requests.activeResult = ActiveRequestResult.None

            viewModel.onCancel()
            advanceUntilIdle()

            val form = form(viewModel)
            assertEquals(listOf(published.id), requests.cancelledIds)
            assertEquals(venousLine.id, form.selectedServiceTypeId)
            assertEquals("Necesito una vía para el suero", form.description)
            assertFalse(form.submitting)
        }

    @Test
    fun aFailedCancellationKeepsTheWaitAndSaysWhy() =
        runTest {
            val viewModel = opened()
            publishedFrom(viewModel)
            requests.cancelResult = CancelRequestResult.Failure(RequestError.NetworkUnavailable)

            viewModel.onCancel()
            advanceUntilIdle()

            assertEquals(
                ImmediateCareUiState.Waiting(
                    published,
                    cancelling = false,
                    cancelError = RequestError.NetworkUnavailable,
                ),
                viewModel.uiState.value,
            )
        }

    // A second tap while the first cancellation is on its way must not send a
    // second one.
    @Test
    fun cancellingTwiceWhileTheFirstRunsCancelsOnce() =
        runTest {
            val viewModel = opened()
            publishedFrom(viewModel)
            requests.cancelGate = CompletableDeferred()

            viewModel.onCancel()
            advanceUntilIdle()
            viewModel.onCancel()
            advanceUntilIdle()

            assertEquals(1, requests.cancelledIds.size)
        }

    // The resume that follows the permission dialog, or a trip to a profile,
    // must not wipe what the patient was typing.
    @Test
    fun aResumeWhileTheFormIsOpenKeepsWhatWasTyped() =
        runTest {
            val viewModel = opened()
            viewModel.onDescriptionChanged("Fiebre alta desde anoche")

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals("Fiebre alta desde anoche", form(viewModel).description)
        }

    // The request expired, or was cancelled from «Mis solicitudes», while the
    // screen was away: it must stop showing as waiting.
    @Test
    fun aRequestThatClosedWhileAwayStopsShowingAsWaiting() =
        runTest {
            requests.activeResult = ActiveRequestResult.Found(published)
            val viewModel = opened()
            requests.activeResult = ActiveRequestResult.None

            viewModel.refresh()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is ImmediateCareUiState.Form)
        }
}

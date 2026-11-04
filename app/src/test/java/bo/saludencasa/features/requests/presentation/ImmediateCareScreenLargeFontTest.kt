package bo.saludencasa.features.requests.presentation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.distanceText
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.core.util.formatTime
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.location.address
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.offerThread
import bo.saludencasa.features.requests.requestOffer
import bo.saludencasa.features.requests.serviceRequest
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalTime
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ImmediateCareScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // The longest of the twelve catalog entries, and a short one to measure it by.
    private val venousLine =
        ServiceType("t1", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60)
    private val vaccination = ServiceType("t2", "Vacunación", null, BigDecimal("90.00"), 20)

    private val home = address(id = "a1", alias = "Mi casa", isPrimary = true)
    private val mother =
        address(id = "a2", alias = "Casa de mi mamá", addressText = "Calle Jordán 456, Cochabamba", isPrimary = false)

    private fun form(
        nearby: NearbyState = NearbyState.Found(emptyList(), more = false),
        selectedServiceTypeId: String? = venousLine.id,
        typeListExpanded: Boolean = false,
    ) = ImmediateCareUiState.Form(
        serviceTypes = listOf(venousLine, vaccination),
        addresses = listOf(home, mother),
        selectedServiceTypeId = selectedServiceTypeId,
        selectedAddressId = home.id,
        typeListExpanded = typeListExpanded,
        description = "",
        budget = "",
        nearby = nearby,
        selectedProfessionalId = null,
        submitting = false,
        submitError = null,
    )

    private fun show(uiState: ImmediateCareUiState) {
        composeRule.setContentAtDoubleFontScale {
            ImmediateCareContent(
                uiState = uiState,
                onBack = {},
                actions = ImmediateCareActions(),
                map = { _, modifier -> Box(modifier = modifier.testTag(MAP_TAG)) },
            )
        }
    }

    @Test
    fun theWayBackAndTheMapStayOnScreen() {
        show(form())

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithTag(MAP_TAG))
    }

    // The button is fixed to the foot, outside the content that scrolls: it has
    // to be there without scrolling, whatever the font did to the form above.
    @Test
    fun askingForCareIsAlwaysWithinReach() {
        show(form())

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.immediate_submit)),
        )
    }

    // HT-23: nobody nearby says so, and the button stays enabled.
    @Test
    fun nobodyNearbyIsSaidAndStillLetsThePatientAsk() {
        show(form(nearby = NearbyState.Found(emptyList(), more = false)))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.immediate_nearby_none)))
        composeRule.onNodeWithText(string(R.string.immediate_submit)).assertIsEnabled()
    }

    @Test
    fun everyAddressOptionStaysOnScreen() {
        show(form())

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(home.alias))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(mother.alias))
    }

    // Open, the longest name of the catalog wraps instead of hiding its tail:
    // its row is taller than the row of a short name (docs/decisions.md,
    // 2026-10-09, the relative measure).
    @Test
    fun theLongestServiceTypeWrapsWhenTheListIsOpen() {
        show(form(selectedServiceTypeId = null, typeListExpanded = true))

        val longRow = composeRule.onNodeWithText(venousLine.name)
        composeRule.assertFitsTheScreen(longRow)
        val shortBounds = composeRule.onNodeWithText(vaccination.name).getUnclippedBoundsInRoot()
        val longBounds = longRow.getUnclippedBoundsInRoot()

        assertTrue(
            "Both rows are the same height, so the long name did not wrap and its tail is hidden.",
            longBounds.bottom - longBounds.top > shortBounds.bottom - shortBounds.top,
        )
    }

    @Test
    fun theWaitSaysItIsSearchingAndKeepsItsDeadlineAndTheWayOut() {
        show(ImmediateCareUiState.Waiting(serviceRequest(serviceTypeName = venousLine.name), false, null))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.immediate_waiting_title)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(venousLine.name))
        composeRule.assertFitsTheScreen(
            // The fixture expires at 13:00 UTC, which is 09:00 in La Paz.
            composeRule.onNodeWithText(
                string(R.string.immediate_waiting_expires, formatTime(LocalTime.of(9, 0), Locale.getDefault())),
            ),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.request_list_cancel_action)))
        composeRule.onNodeWithText(string(R.string.immediate_offers_title)).assertDoesNotExist()
    }

    // HT-24: three offers stacked as cards, each with its professional, its
    // distance and its amount, all reachable and none off the edge.
    @Test
    fun threeOffersStackAsCardsThatFitTheScreen() {
        val threads =
            listOf(
                offerThread(professionalId = "p1", professionalName = "Ana Pérez Quiroga de la Fuente")
                    .copy(distanceM = 1200),
                offerThread(professionalId = "p2", professionalName = "Luis Gutiérrez").copy(distanceM = 3400),
                offerThread(professionalId = "p3", professionalName = "Carla Rojas").copy(distanceM = 900),
            )
        show(
            ImmediateCareUiState.Waiting(
                serviceRequest(),
                cancelling = false,
                cancelError = null,
                threads = threads,
            ),
        )

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Ana Pérez Quiroga de la Fuente"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Carla Rojas"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(inboxDistance(3400)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.immediate_offers_title)))
        composeRule.onNodeWithText(string(R.string.immediate_waiting_title)).assertDoesNotExist()
    }

    private fun waitingWith(
        thread: OfferThread,
        acceptError: OfferError? = null,
        confirming: Boolean = false,
        accepting: Boolean = false,
    ) = ImmediateCareUiState.Waiting(
        serviceRequest(),
        cancelling = false,
        cancelError = null,
        threads = listOf(thread),
        confirmingAcceptance = thread.takeIf { confirming },
        accepting = accepting,
        acceptError = acceptError,
    )

    private fun acceptLabel(thread: OfferThread): String =
        string(R.string.offer_card_accept, formatBob(thread.lastOffer!!.amountBob, Locale.getDefault()))

    // HT-28: on the patient's turn the amount can be accepted right there, with the
    // way to negotiate next to it. At 200 % they stack, with accepting on top.
    @Test
    fun onThePatientsTurnAcceptingAndNegotiatingStackWithAcceptingOnTop() {
        val thread = offerThread()
        show(waitingWith(thread))

        val accept = composeRule.onNodeWithText(acceptLabel(thread))
        val negotiate = composeRule.onNodeWithText(string(R.string.offer_card_negotiate))
        composeRule.assertFitsTheScreen(accept)
        composeRule.assertFitsTheScreen(negotiate)
        assertTrue(
            "Accepting is not above negotiating.",
            accept.getUnclippedBoundsInRoot().top < negotiate.getUnclippedBoundsInRoot().top,
        )
    }

    // The professional has to answer first: nothing here can be accepted.
    @Test
    fun whileThePatientWaitsForAnAnswerOnlyTheNegotiationOpens() {
        val thread = offerThread(offers = listOf(requestOffer(issuer = OfferIssuer.PATIENT)))
        show(waitingWith(thread))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_card_view)))
        composeRule.onNodeWithText(acceptLabel(thread)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.offer_card_negotiate)).assertDoesNotExist()
    }

    @Test
    fun theButtonsHandTheirOfferOverAndTheRestOpensTheNegotiation() {
        val thread = offerThread(professionalId = "p7")
        var accepted: OfferThread? = null
        var opened: String? = null
        composeRule.setContentAtDoubleFontScale {
            ImmediateCareContent(
                uiState = waitingWith(thread),
                onBack = {},
                actions =
                    ImmediateCareActions(
                        onAccept = { accepted = it },
                        onOpenThread = { _, professionalId -> opened = professionalId },
                    ),
            )
        }

        composeRule.onNodeWithText(acceptLabel(thread)).performScrollTo().performClick()
        assertEquals(thread, accepted)
        assertEquals(null, opened)

        composeRule.onNodeWithText(string(R.string.offer_card_negotiate)).performScrollTo().performClick()
        assertEquals("p7", opened)
    }

    // A second tap while the first travels must have nothing to press.
    @Test
    fun whileAcceptingTheOfferButtonsAreOff() {
        val thread = offerThread()
        show(waitingWith(thread, accepting = true))

        composeRule.onNodeWithText(acceptLabel(thread)).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.offer_card_negotiate)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.request_list_cancel_action)).performScrollTo().assertIsNotEnabled()
    }

    // Whoever confirms reads who and how much, with the two ways out inside the phone.
    @Test
    fun theConfirmationNamesTheProfessionalAndTheAmount() {
        val thread = offerThread(professionalName = "Ana Pérez Quiroga de la Fuente")
        show(waitingWith(thread, confirming = true))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(
                string(
                    R.string.offer_accept_confirm_body_named,
                    formatBob(thread.lastOffer!!.amountBob, Locale.getDefault()),
                    "Ana Pérez Quiroga de la Fuente",
                ),
            ),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_accept_confirm_back)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_accept_confirm_action)),
        )
    }

    // The engine refusing means the offer changed; a lost connection does not, and
    // saying so would send the patient looking for a change that did not happen.
    @Test
    fun aRefusedAcceptanceSaysTheOfferChanged() {
        show(waitingWith(offerThread(), acceptError = OfferError.NotAllowed))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.immediate_offer_changed)))
    }

    @Test
    fun aLostConnectionSaysThatInstead() {
        show(waitingWith(offerThread(), acceptError = OfferError.NetworkUnavailable))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.error_network_unavailable)))
        composeRule.onNodeWithText(string(R.string.immediate_offer_changed)).assertDoesNotExist()
    }

    // Until the first read answers there is no way to tell, so nothing is claimed.
    @Test
    fun beforeTheFirstReadAnswersTheScreenClaimsNothing() {
        show(ImmediateCareUiState.Waiting(serviceRequest(), false, null, offersRead = false))

        composeRule.onNodeWithText(string(R.string.immediate_waiting_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.immediate_offers_title)).assertDoesNotExist()
    }

    // The address reads as an address: the alias, its street under it, and the
    // way to add another without leaving the form.
    @Test
    fun theAddressShowsItsStreetAndTheWayToAddAnother() {
        show(form())

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(home.addressText, useUnmergedTree = true))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.request_address_add_another)))
    }

    @Test
    fun aFailedCancellationSaysWhyNextToTheButton() {
        show(
            ImmediateCareUiState.Waiting(
                serviceRequest(),
                cancelling = false,
                cancelError = RequestError.NetworkUnavailable,
            ),
        )

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.error_network_unavailable)))
    }

    // The patient who arrives here without an address has to be able to leave
    // for the screen that registers one.
    @Test
    fun theNoAddressWayOutStaysOnScreen() {
        show(ImmediateCareUiState.NoAddress)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_create_no_address_action)),
        )
    }

    @Test
    fun aFailedLoadKeepsItsRetryOnScreen() {
        show(ImmediateCareUiState.Failed(RequestError.NetworkUnavailable))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    // The distance as the card writes it, from the same resource and locale.
    private fun inboxDistance(distanceM: Int): String =
        distanceText(
            ApplicationProvider.getApplicationContext<Context>().resources,
            distanceM,
            Locale.getDefault(),
        )

    private companion object {
        const val MAP_TAG = "immediate_map"
    }
}

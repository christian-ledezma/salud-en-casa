package bo.saludencasa.features.requests.presentation

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferStatus
import bo.saludencasa.features.requests.domain.model.PatientReputation
import bo.saludencasa.features.requests.offerThread
import bo.saludencasa.features.requests.offerableRequest
import bo.saludencasa.features.requests.requestOffer
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.plural
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class OfferThreadScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        uiState: OfferThreadUiState,
        actions: OfferThreadActions = OfferThreadActions(),
    ) {
        composeRule.setContentAtDoubleFontScale {
            OfferThreadContent(uiState = uiState, onBack = {}, actions = actions)
        }
    }

    private fun chain(
        vararg amounts: Pair<String, OfferIssuer>,
        lastStatus: OfferStatus = OfferStatus.PROPOSED,
    ) = amounts.mapIndexed { index, (amount, issuer) ->
        requestOffer(
            id = "o$index",
            parentOfferId = if (index == 0) null else "o${index - 1}",
            amountBob = BigDecimal(amount),
            issuer = issuer,
            status = if (index == amounts.lastIndex) lastStatus else OfferStatus.PROPOSED,
            message = "Mensaje de la propuesta número $index para esta atención.",
        )
    }

    // HT-24: the professional's first offer is on the same screen as the
    // negotiation: the request, the amount, the optional message and the button
    // fixed to the foot.
    @Test
    fun theFirstOfferFormStaysOnScreen() {
        show(OfferThreadUiState.Opening(request = offerableRequest(), amount = "160"))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Vacunación"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_amount_label)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_message_label)))
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_submit)).assertIsEnabled(),
        )
    }

    // HU-38, criterion 4: a refused first offer says why, on screen at 200 %.
    // The reason sits in the bar fixed to the foot, above the button.
    @Test
    fun aFirstOfferWithoutASubscriptionSaysWhy() {
        show(
            OfferThreadUiState.Opening(
                request = offerableRequest(),
                amount = "160",
                error = OfferError.SubscriptionExpired,
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.error_offer_subscription_expired)),
        )
    }

    // The patient sees their floor under the amount before typing, so the
    // refusal is never the first time they hear of it.
    @Test
    fun thePatientSeesTheirMinimumUnderTheAmount() {
        show(
            OfferThreadUiState.Content(
                thread =
                    offerThread(offers = chain("160.00" to OfferIssuer.PROFESSIONAL))
                        .copy(suggestedBudgetBob = BigDecimal("130.00")),
                countering = true,
            ),
        )

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(
                string(R.string.offer_counter_minimum, formatBob(BigDecimal("130.00"), Locale.getDefault())),
            ),
        )
    }

    // Below the floor, the field itself says what the minimum is.
    @Test
    fun aCounterBelowTheMinimumSaysItOnTheField() {
        show(
            OfferThreadUiState.Content(
                thread = offerThread(offers = chain("160.00" to OfferIssuer.PROFESSIONAL)),
                countering = true,
                amount = "100",
                error = OfferError.BelowMinimum(BigDecimal("130.00")),
            ),
        )

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(
                string(R.string.error_offer_below_minimum, formatBob(BigDecimal("130.00"), Locale.getDefault())),
            ),
        )
    }

    // Every amount of the thread is reachable and none runs off the edge, even
    // with five rounds at 200 % of font size.
    @Test
    fun everyRoundOfTheThreadStaysOnScreen() {
        val offers =
            chain(
                "160.00" to OfferIssuer.PROFESSIONAL,
                "120.00" to OfferIssuer.PATIENT,
                "150.00" to OfferIssuer.PROFESSIONAL,
                "130.00" to OfferIssuer.PATIENT,
                "140.00" to OfferIssuer.PROFESSIONAL,
            )
        show(OfferThreadUiState.Content(thread = offerThread(offers = offers)))

        offers.forEach { offer ->
            composeRule.assertFitsTheScreen(
                composeRule.onNodeWithText(formatBob(offer.amountBob, Locale.getDefault())),
            )
        }
    }

    // On my turn the three answers are offered -- accept, counter and reject --
    // and all stay on screen, with accept the primary one fixed to the foot.
    @Test
    fun onMyTurnTheThreeAnswersAreReachable() {
        show(OfferThreadUiState.Content(thread = offerThread(offers = chain("160.00" to OfferIssuer.PROFESSIONAL))))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_accept_button)).assertIsEnabled(),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_counter_open)).assertIsEnabled(),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_reject)).assertIsEnabled(),
        )
    }

    // The acceptance confirmation names the amount and stays on screen.
    @Test
    fun theAcceptConfirmationStaysOnScreen() {
        show(
            OfferThreadUiState.Content(
                thread = offerThread(offers = chain("160.00" to OfferIssuer.PROFESSIONAL)),
                confirmingAcceptance = true,
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_accept_confirm_title)),
        )
    }

    private fun sameAmount(message: String) =
        OfferThreadUiState.Content(
            thread = offerThread(offers = chain("160.00" to OfferIssuer.PROFESSIONAL)),
            countering = true,
            amount = "160",
            message = message,
            confirmingAcceptance = true,
        )

    private fun bob(amount: String) = formatBob(BigDecimal(amount), Locale.getDefault())

    // HT-29: writing the amount on the table says that it accepts the offer, and
    // both ways out stay on screen.
    @Test
    fun theSameAmountSaysItAcceptsTheOffer() {
        show(sameAmount(message = ""))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.offer_counter_accepts_body, bob("160.00"))),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_accept_confirm_back)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_accept_confirm_action)),
        )
    }

    // What was written does not travel with an acceptance, and the person hears it
    // before confirming, not after.
    @Test
    fun theSameAmountWithAMessageWarnsItWillNotBeSent() {
        show(sameAmount(message = "Puedo pagar en efectivo."))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.offer_counter_accepts_body_with_message, bob("160.00"))),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_accept_confirm_action)),
        )
        composeRule.onNodeWithText(string(R.string.offer_counter_accepts_body, bob("160.00"))).assertDoesNotExist()
    }

    // The button keeps its own confirmation: nothing was typed there.
    @Test
    fun theAcceptButtonKeepsItsOwnConfirmation() {
        show(
            OfferThreadUiState.Content(
                thread = offerThread(offers = chain("160.00" to OfferIssuer.PROFESSIONAL)),
                confirmingAcceptance = true,
            ),
        )

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.offer_accept_confirm_body, bob("160.00"))),
        )
        composeRule.onNodeWithText(string(R.string.offer_counter_accepts_body, bob("160.00"))).assertDoesNotExist()
    }

    @Test
    fun theCounterOfferFormStaysOnScreen() {
        show(
            OfferThreadUiState.Content(
                thread = offerThread(offers = chain("160.00" to OfferIssuer.PROFESSIONAL)),
                countering = true,
                amount = "140",
            ),
        )

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_counter_amount_label)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_counter_message_label)))
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_counter_submit)),
        )
    }

    // A closed thread says so and offers no control to press.
    @Test
    fun aClosedThreadSaysSoAndOffersNothing() {
        show(
            OfferThreadUiState.Content(
                thread =
                    offerThread(
                        offers = chain("160.00" to OfferIssuer.PROFESSIONAL, lastStatus = OfferStatus.REJECTED),
                    ),
            ),
        )

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.thread_state_closed)))
        composeRule.onNodeWithText(string(R.string.thread_rejected_badge)).assertExists()
        composeRule.onNodeWithText(string(R.string.offer_counter_open)).assertDoesNotExist()
    }

    // The professional reads the request above the thread, and never the
    // counterpart header, which on their side would be their own card.
    @Test
    fun theProfessionalReadsTheRequestAndNotACounterpartHeader() {
        show(
            OfferThreadUiState.Content(
                thread =
                    offerThread(
                        professionalName = "Ana Pérez Quiroga",
                        offers = chain("160.00" to OfferIssuer.PROFESSIONAL),
                        iAmTheProfessional = true,
                    ),
                request = offerableRequest(),
            ),
        )

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Vacunación"))
        composeRule.onNodeWithText("Ana Pérez Quiroga").assertDoesNotExist()
    }

    // The detail may not come back: the thread is still readable, and still
    // without a header that belongs to the other side.
    @Test
    fun theProfessionalWithoutTheRequestStillReadsTheThread() {
        show(
            OfferThreadUiState.Content(
                thread =
                    offerThread(
                        professionalName = "Ana Pérez Quiroga",
                        offers = chain("160.00" to OfferIssuer.PROFESSIONAL),
                        iAmTheProfessional = true,
                    ),
            ),
        )

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(formatBob(BigDecimal("160.00"), Locale.getDefault())),
        )
        composeRule.onNodeWithText("Ana Pérez Quiroga").assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.negotiation_no_rating)).assertDoesNotExist()
    }

    @Test
    fun theGoneStateStaysOnScreen() {
        show(OfferThreadUiState.Gone)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.thread_gone_title)))
    }

    @Test
    fun aFailureKeepsItsRetryOnScreen() {
        show(OfferThreadUiState.Failed(OfferError.NetworkUnavailable))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    // HU-41: the mark sits under the message of the first offer, with what it does
    // said beside it, and the whole row is the target, not only the box.
    @Test
    fun theFinalOfferMarkIsReachableOnTheFirstOfferFormAndTheRowTogglesIt() {
        var marked = false
        show(
            OfferThreadUiState.Opening(request = offerableRequest(), amount = "160"),
            actions = OfferThreadActions(onFinalChanged = { marked = it }),
        )

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_final_supporting)))
        composeRule.onNodeWithText(string(R.string.offer_final_label)).performScrollTo().performClick()

        assertTrue(marked)
    }

    // Marked, the button says what will leave before it leaves: there is no dialog.
    @Test
    fun aMarkedCounterOfferSaysItIsTheLastOnItsButton() {
        show(
            OfferThreadUiState.Content(
                thread = offerThread(offers = chain("160.00" to OfferIssuer.PROFESSIONAL)),
                countering = true,
                amount = "140",
                isFinal = true,
            ),
        )

        composeRule.assertFitsTheScreen(composeRule.onNode(isToggleable()).assertIsOn())
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_final_submit)),
        )
        composeRule.onNodeWithText(string(R.string.offer_counter_submit)).assertDoesNotExist()
    }

    // RN-15: before the other side's final offer, accepting and rejecting are all
    // there is, and the row and the state both say why.
    @Test
    fun theOtherSidesFinalOfferLeavesOnlyAcceptAndReject() {
        val offers =
            chain("160.00" to OfferIssuer.PROFESSIONAL).map { it.copy(isFinal = true) }
        show(OfferThreadUiState.Content(thread = offerThread(offers = offers)))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_accept_button)).assertIsEnabled(),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_reject)).assertIsEnabled(),
        )
        composeRule.onNodeWithText(string(R.string.offer_counter_open)).assertDoesNotExist()
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.thread_final_badge)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.thread_state_final_to_answer)))
    }

    // One's own final offer is waited on, and the reader is told it was the last.
    @Test
    fun myOwnFinalOfferIsWaitedOnAndSaysSo() {
        val offers =
            chain("160.00" to OfferIssuer.PROFESSIONAL).map { it.copy(isFinal = true) }
        show(OfferThreadUiState.Content(thread = offerThread(offers = offers, iAmTheProfessional = true)))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.thread_state_final_sent)))
        composeRule.onNodeWithText(string(R.string.offer_accept_button)).assertDoesNotExist()
    }

    // HU-30: before offering, the professional reads how the patient has been rated.
    @Test
    fun theDetailBeforeOfferingSaysHowThePatientHasBeenRated() {
        show(
            OfferThreadUiState.Opening(
                request = offerableRequest(patientReputation = PatientReputation(BigDecimal("3.00"), 2)),
                amount = "160",
            ),
        )

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("3.0"))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(plural(R.plurals.patient_reputation_count, 2, 2)),
        )
    }

    @Test
    fun theDetailOfAPatientNobodyRatedSaysSo() {
        show(OfferThreadUiState.Opening(request = offerableRequest(), amount = "160"))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.patient_reputation_none)))
    }
}

package bo.saludencasa.features.requests.presentation

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferStatus
import bo.saludencasa.features.requests.offerThread
import bo.saludencasa.features.requests.offerableRequest
import bo.saludencasa.features.requests.requestOffer
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class OfferThreadScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(uiState: OfferThreadUiState) {
        composeRule.setContentAtDoubleFontScale {
            OfferThreadContent(uiState = uiState, onBack = {}, actions = OfferThreadActions())
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
}

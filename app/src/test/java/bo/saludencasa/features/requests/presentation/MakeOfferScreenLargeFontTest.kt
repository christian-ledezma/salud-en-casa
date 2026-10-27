package bo.saludencasa.features.requests.presentation

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.offerableRequest
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MakeOfferScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(uiState: MakeOfferUiState) {
        composeRule.setContentAtDoubleFontScale {
            MakeOfferContent(uiState = uiState, onBack = {}, onSent = {}, actions = MakeOfferActions())
        }
    }

    private fun content(
        alreadyOffered: Boolean = false,
        error: OfferError? = null,
    ) = MakeOfferUiState.Content(
        request = offerableRequest(alreadyOffered = alreadyOffered),
        amount = "140",
        message = "Puedo llegar en una hora",
        submitting = false,
        error = error,
    )

    @Test
    fun theFormFieldsStayOnScreen() {
        show(content())

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_amount_label)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_message_label)))
    }

    // The submit button is fixed to the foot, so it is reachable whatever the
    // font did to the detail and the fields above it.
    @Test
    fun sendingTheOfferIsAlwaysWithinReach() {
        show(content())

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.offer_submit)),
        )
        composeRule.onNodeWithText(string(R.string.offer_submit)).assertIsEnabled()
    }

    @Test
    fun theAlreadyOfferedNoticeStaysOnScreen() {
        show(content(alreadyOffered = true))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_already_title)))
    }

    @Test
    fun theGoneMessageStaysOnScreen() {
        show(MakeOfferUiState.Gone)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_request_gone_title)))
    }

    @Test
    fun theSentConfirmationStaysOnScreen() {
        show(MakeOfferUiState.Sent)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.offer_sent_title)))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.immediate_waiting_my_requests)),
        )
    }

    @Test
    fun aFailedLoadKeepsItsRetryOnScreen() {
        show(MakeOfferUiState.Failed(OfferError.NetworkUnavailable))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }
}

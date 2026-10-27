package bo.saludencasa.features.requests.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.requestOffer
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class NegotiationScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(uiState: NegotiationUiState) {
        composeRule.setContentAtDoubleFontScale {
            NegotiationContent(uiState = uiState, onBack = {}, onRetry = {})
        }
    }

    // The first offer's professional and amount are on screen at the top of the
    // list, without the card running off the edge.
    @Test
    fun theFirstOfferStaysOnScreen() {
        val offer = requestOffer(id = "o1", professionalName = "Ana Pérez Quiroga", amountBob = BigDecimal("160.00"))
        show(NegotiationUiState.Content(listOf(offer)))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Ana Pérez Quiroga"))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(formatBob(BigDecimal("160.00"), Locale.getDefault())),
        )
    }

    @Test
    fun theEmptyStateStaysOnScreen() {
        show(NegotiationUiState.Empty)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.negotiation_empty_title)))
    }

    @Test
    fun aFailureKeepsItsRetryOnScreen() {
        show(NegotiationUiState.Failed(OfferError.NetworkUnavailable))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }
}

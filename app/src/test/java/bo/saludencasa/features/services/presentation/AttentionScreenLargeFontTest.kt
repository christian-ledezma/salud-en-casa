package bo.saludencasa.features.services.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.ServiceError
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
class AttentionScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(uiState: AttentionUiState) {
        composeRule.setContentAtDoubleFontScale {
            AttentionContent(uiState = uiState, onBack = {}, onRetry = {}, onOpenProfessionalProfile = {})
        }
    }

    // The sealed agreement: the banner, the professional with the link to their
    // public profile, and the frozen amount, all reachable at 200 % of font size.
    // No phone and no call button are shown.
    @Test
    fun theSealedAgreementStaysOnScreen() {
        show(AttentionUiState.Content(attention()))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.attention_sealed_banner)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("María Quispe"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.attention_view_professional)))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(formatBob(BigDecimal("150.00"), Locale.getDefault())),
        )
    }

    @Test
    fun theNotFoundStateStaysOnScreen() {
        show(AttentionUiState.NotFound)

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.attention_not_found_title)),
        )
    }

    @Test
    fun aFailureKeepsItsRetryOnScreen() {
        show(AttentionUiState.Failed(ServiceError.NetworkUnavailable))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.common_retry)),
        )
    }
}

package bo.saludencasa.features.subscription.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatDate
import bo.saludencasa.features.subscription.domain.model.SubscriptionError
import bo.saludencasa.features.subscription.domain.model.SubscriptionStatus
import bo.saludencasa.features.subscription.subscription
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class SubscriptionScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(uiState: SubscriptionUiState) {
        composeRule.setContentAtDoubleFontScale {
            SubscriptionContent(uiState = uiState, onBack = {}, onRetry = {})
        }
    }

    private val end get() = formatDate(subscription().endsAt, Locale.getDefault())

    // RF-14.3: the state, its end date, what the professional has while it lasts
    // and what an expiry leaves them, all reachable at 200 % on 320 dp.
    @Test
    fun aTrialShowsItsEndAndEverythingItGives() {
        show(SubscriptionUiState.Content(subscription(SubscriptionStatus.TRIAL)))

        listOf(
            string(R.string.subscription_status_trial),
            string(R.string.subscription_valid_until, end),
            string(R.string.subscription_gain_search),
            string(R.string.subscription_gain_requests),
            string(R.string.subscription_gain_booking),
            string(R.string.subscription_gain_offers),
            string(R.string.subscription_keep_negotiations),
            string(R.string.subscription_keep_attentions),
            string(R.string.subscription_purchase_soon),
        ).forEach { text -> composeRule.assertFitsTheScreen(composeRule.onNodeWithText(text)) }
    }

    // Expired, the same four items read as what is lost, and the two that go on
    // are still said: criterion 5 of HU-38.
    @Test
    fun anExpiredSubscriptionSaysWhatIsLostAndWhatGoesOn() {
        show(SubscriptionUiState.Content(subscription(SubscriptionStatus.EXPIRED)))

        listOf(
            string(R.string.subscription_status_expired),
            string(R.string.subscription_expired_on, end),
            string(R.string.subscription_gains_title_expired),
            string(R.string.subscription_gain_offers),
            string(R.string.subscription_keeps_title_expired),
            string(R.string.subscription_keep_negotiations),
            string(R.string.subscription_keep_attentions),
        ).forEach { text -> composeRule.assertFitsTheScreen(composeRule.onNodeWithText(text)) }
        assertEquals(
            0,
            composeRule
                .onAllNodesWithText(string(R.string.subscription_gains_title_valid))
                .fetchSemanticsNodes()
                .size,
        )
    }

    @Test
    fun theRequirementTurnedOffSaysSo() {
        show(SubscriptionUiState.NotRequired)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.subscription_not_required_title)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.subscription_not_required_body)))
    }

    @Test
    fun aFailureKeepsItsRetryOnScreen() {
        show(SubscriptionUiState.Failed(SubscriptionError.NetworkUnavailable))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.error_network_unavailable)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }
}

package bo.saludencasa.features.services.presentation

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.services.attentionSummary
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class MyAttentionsScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val appointment = Instant.parse("2026-11-05T14:30:00Z")

    private fun show(
        uiState: MyAttentionsUiState,
        actions: MyAttentionsActions = MyAttentionsActions(),
    ) {
        composeRule.setContentAtDoubleFontScale {
            MyAttentionsContent(uiState = uiState, actions = actions)
        }
    }

    // What the professional needs to recognise the attention, all of it reachable
    // at 200 %.
    @Test
    fun aCardShowsWhatWasAgreed() {
        show(
            MyAttentionsUiState.Content(
                attentions = listOf(attentionSummary(scheduledAt = appointment)),
                endReached = true,
            ),
        )

        val locale = Locale.getDefault()
        listOf(
            "Curación de heridas",
            string(R.string.attention_status_assigned),
            "María Quispe",
            formatBob(BigDecimal("150.00"), locale),
            string(R.string.request_scheduled_for, formatAppointment(appointment, locale)),
        ).forEach { text ->
            composeRule.assertFitsTheScreenWithoutScrolling(composeRule.onNodeWithText(text))
        }
    }

    @Test
    fun aCancelledAttentionSaysSo() {
        show(
            MyAttentionsUiState.Content(
                attentions = listOf(attentionSummary(status = ServiceStatus.CANCELLED)),
                endReached = true,
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.attention_status_cancelled)),
        )
    }

    // The card is the way back to the attention and its conversation: the hole
    // this list exists to close.
    @Test
    fun tappingACardOpensItsAttention() {
        var opened: String? = null
        show(
            MyAttentionsUiState.Content(listOf(attentionSummary(requestId = "r9")), endReached = true),
            MyAttentionsActions(onOpenAttention = { opened = it }),
        )

        composeRule.onNodeWithText("María Quispe").performClick()

        assertEquals("r9", opened)
    }

    @Test
    fun theNextPageStaysReachableBelowTheCards() {
        show(
            MyAttentionsUiState.Content(
                attentions = List(3) { attentionSummary(requestId = "r$it") },
                endReached = false,
            ),
        )

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.my_attentions_load_more)),
        )
    }

    @Test
    fun theEmptyStateExplainsItself() {
        show(MyAttentionsUiState.Empty)

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.my_attentions_empty_title)),
        )
    }

    @Test
    fun aFailureKeepsItsRetryOnScreen() {
        show(MyAttentionsUiState.Failed(ServiceError.NetworkUnavailable))

        composeRule.assertFitsTheScreenWithoutScrolling(composeRule.onNodeWithText(string(R.string.common_retry)))
    }
}

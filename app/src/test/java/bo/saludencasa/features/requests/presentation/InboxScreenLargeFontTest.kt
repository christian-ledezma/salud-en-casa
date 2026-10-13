package bo.saludencasa.features.requests.presentation

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class InboxScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun request(id: String): InboxRequest =
        InboxRequest(
            id = id,
            serviceTypeName = "Colocación y control de vía venosa",
            city = "Cochabamba",
            description = "Necesito atención para un familiar esta tarde.",
            suggestedBudgetBob = BigDecimal("150.00"),
            distanceM = 1200,
            status = RequestStatus.PUBLISHED,
            createdAt = Instant.parse("2026-10-13T12:00:00Z"),
        )

    private fun show(
        requests: List<InboxRequest>,
        endReached: Boolean = true,
    ) {
        composeRule.setContentAtDoubleFontScale {
            InboxContent(
                uiState =
                    InboxUiState.Content(
                        requests = requests,
                        loadingMore = false,
                        endReached = endReached,
                        notice = null,
                    ),
                onBack = {},
                actions = InboxActions(),
            )
        }
    }

    // A LazyColumn composes nothing below the fold, so a row is reached by its
    // index first.
    private fun scrollTo(index: Int) {
        composeRule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(index)
    }

    @Test
    fun theWayBackStaysOnScreen() {
        show(listOf(request("r1")))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
    }

    @Test
    fun aCardKeepsItsServiceTypeOnScreen() {
        show(listOf(request("r1")))
        scrollTo(1)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText("Colocación y control de vía venosa"),
        )
    }

    @Test
    fun theLoadMoreFooterStaysOnScreen() {
        show(listOf(request("r1")), endReached = false)
        scrollTo(2)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_inbox_load_more)),
        )
    }

    @Test
    fun theEmptyStateKeepsItsHeadingOnScreen() {
        composeRule.setContentAtDoubleFontScale {
            InboxContent(
                uiState = InboxUiState.Empty,
                onBack = {},
                actions = InboxActions(),
            )
        }

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_inbox_empty_title)),
        )
    }
}

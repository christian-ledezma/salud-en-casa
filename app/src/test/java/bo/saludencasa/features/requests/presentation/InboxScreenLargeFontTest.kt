package bo.saludencasa.features.requests.presentation

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.features.notifications.presentation.NotificationRationale
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class InboxScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun request(
        id: String,
        scheduledAt: Instant? = null,
    ): InboxRequest =
        InboxRequest(
            id = id,
            serviceTypeName = "Colocación y control de vía venosa",
            city = "Cochabamba",
            description = "Necesito atención para un familiar esta tarde.",
            suggestedBudgetBob = BigDecimal("150.00"),
            distanceM = 1200,
            status = RequestStatus.PUBLISHED,
            createdAt = Instant.parse("2026-10-13T12:00:00Z"),
            modality = if (scheduledAt == null) RequestModality.IMMEDIATE else RequestModality.SCHEDULED,
            scheduledAt = scheduledAt,
        )

    private fun show(
        requests: List<InboxRequest>,
        endReached: Boolean = true,
        withPrompt: Boolean = false,
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
                permissionPrompt = {
                    if (withPrompt) {
                        NotificationRationale(
                            body = R.string.notification_permission_body_professional,
                            onEnable = {},
                            onDismiss = {},
                        )
                    }
                },
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

    // HU-16: a scheduled request names its appointment and says it is scheduled,
    // and neither line may fall off a 320 dp card at a doubled font.
    @Test
    fun aScheduledCardKeepsItsBadgeAndAppointmentOnScreen() {
        val at = Instant.parse("2026-10-16T13:30:00Z")
        show(listOf(request("r1", scheduledAt = at)))
        scrollTo(1)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.request_modality_scheduled)))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_scheduled_for, formatAppointment(at, deviceLocale()))),
        )
    }

    // The locale the card formats the appointment with.
    private fun deviceLocale(): Locale =
        ApplicationProvider
            .getApplicationContext<Context>()
            .resources.configuration.locales[0]

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

    // The permission card sits above the requests; both of its choices and the
    // first request must stay reachable below it.
    @Test
    fun thePermissionCardLeavesTheInboxReachable() {
        show(listOf(request("r1")), withPrompt = true)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.notification_permission_enable)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.notification_permission_dismiss)))
        scrollTo(1)
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Colocación y control de vía venosa"))
    }
}

package bo.saludencasa.features.requests.presentation

import android.content.Context
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.model.ServiceRequest
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
class MyRequestsScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun request(
        id: String,
        status: RequestStatus,
        scheduledAt: Instant? = null,
    ): ServiceRequest =
        ServiceRequest(
            id = id,
            serviceTypeName = "Colocación y control de vía venosa",
            addressText = "Avenida América 1234, Cochabamba",
            city = "Cochabamba",
            status = status,
            modality = if (scheduledAt == null) RequestModality.IMMEDIATE else RequestModality.SCHEDULED,
            description = null,
            suggestedBudgetBob = BigDecimal("150.00"),
            expiresAt = scheduledAt ?: Instant.parse("2026-10-12T13:00:00Z"),
            createdAt = Instant.parse("2026-10-12T12:00:00Z"),
            scheduledAt = scheduledAt,
            professionalName = "Ana Quispe Mamani de la Fuente".takeIf { scheduledAt != null },
        )

    private fun show(
        requests: List<ServiceRequest>,
        endReached: Boolean = true,
    ) {
        composeRule.setContentAtDoubleFontScale {
            MyRequestsContent(
                uiState =
                    MyRequestsUiState.Content(
                        requests = requests,
                        loadingMore = false,
                        endReached = endReached,
                        notice = null,
                        cancellingId = null,
                    ),
                onBack = {},
                actions = MyRequestsActions(),
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
        show(listOf(request("r1", RequestStatus.PUBLISHED)))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
    }

    @Test
    fun aCardKeepsItsNameAndStatusOnScreen() {
        show(listOf(request("r1", RequestStatus.PUBLISHED)))
        scrollTo(1)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText("Colocación y control de vía venosa"),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_status_published)),
        )
    }

    // The cancel action is the whole point of this screen for a published
    // request, and it has to stay reachable at a doubled font.
    @Test
    fun theCancelActionOfAPublishedRequestStaysOnScreen() {
        show(listOf(request("r1", RequestStatus.PUBLISHED)))
        scrollTo(1)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_list_cancel_action)),
        )
    }

    // HU-16: the patient reads when and with whom; the professional's name is
    // a long one on purpose.
    @Test
    fun aScheduledCardKeepsItsAppointmentAndProfessionalOnScreen() {
        val at = Instant.parse("2026-10-16T13:30:00Z")
        show(listOf(request("r1", RequestStatus.PUBLISHED, scheduledAt = at)))
        scrollTo(1)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_scheduled_for, formatAppointment(at, deviceLocale()))),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(
                string(R.string.request_list_with_professional, "Ana Quispe Mamani de la Fuente"),
            ),
        )
    }

    // The locale the card formats the appointment with.
    private fun deviceLocale(): Locale =
        ApplicationProvider
            .getApplicationContext<Context>()
            .resources.configuration.locales[0]

    @Test
    fun theLoadMoreFooterStaysOnScreen() {
        show(listOf(request("r1", RequestStatus.PUBLISHED)), endReached = false)
        scrollTo(2)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_list_load_more)),
        )
    }

    @Test
    fun theEmptyStateKeepsItsCallToActionOnScreen() {
        composeRule.setContentAtDoubleFontScale {
            MyRequestsContent(
                uiState = MyRequestsUiState.Empty,
                onBack = {},
                actions = MyRequestsActions(),
            )
        }

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_list_create_action)),
        )
    }
}

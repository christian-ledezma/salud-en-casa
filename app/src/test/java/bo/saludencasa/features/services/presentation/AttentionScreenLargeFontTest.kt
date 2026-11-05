package bo.saludencasa.features.services.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.attentionPlace
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionCancellation
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class AttentionScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(uiState: AttentionUiState) {
        composeRule.setContentAtDoubleFontScale {
            AttentionContent(uiState = uiState, actions = AttentionActions())
        }
    }

    private fun professional(status: ServiceStatus = ServiceStatus.ASSIGNED): Attention =
        attention(status = status, iAmThePatient = false)

    private fun markButton(res: Int) = composeRule.onNodeWithText(string(res))

    @Test
    fun everythingTheAttentionSaysStaysOnScreen() {
        show(AttentionUiState.Content(professional()))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.attention_hero_assigned_professional_title)),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("María Quispe"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.attention_open_conversation)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Av. Arce 2631"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.attention_directions)))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(formatBob(BigDecimal("150.00"), Locale.getDefault())),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.attention_step_arrival)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Curación de una herida en la pierna derecha."))
    }

    // Fixed to the foot, so it stays there however long the page is.
    @Test
    fun theProfessionalAlwaysHasTheNextMarkOnScreen() {
        show(AttentionUiState.Content(professional()))

        composeRule.assertFitsTheScreenWithoutScrolling(markButton(R.string.attention_mark_arrival).assertIsEnabled())
    }

    @Test
    fun aMarkOnItsWayCannotBeTappedAgain() {
        show(AttentionUiState.Content(professional(), advancing = true))

        markButton(R.string.attention_mark_arrival).assertIsNotEnabled()
    }

    @Test
    fun theDialogKeepsBothAnswersOnScreen() {
        show(
            AttentionUiState.Content(
                professional(ServiceStatus.IN_PROGRESS),
                confirming = AttentionStep.MarkCompleted,
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.attention_confirm_completed_title)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.attention_mark_not_yet)),
        )
        // The button behind the dialog says the same, so the answer is the last of the two.
        val answers = composeRule.onAllNodesWithText(string(R.string.attention_mark_completed))
        composeRule.assertFitsTheScreenWithoutScrolling(answers[answers.fetchSemanticsNodes().lastIndex])
    }

    @Test
    fun aFailedMarkSaysWhyAboveTheButton() {
        show(AttentionUiState.Content(professional(), notice = ServiceError.NetworkUnavailable))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.attention_mark_failed_network)),
        )
    }

    // RF-10.1 and RF-10.2.
    @Test
    fun thePatientIsNeverOfferedAMark() {
        show(AttentionUiState.Content(attention(status = ServiceStatus.ASSIGNED, iAmThePatient = true)))

        markButton(R.string.attention_mark_arrival).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.attention_directions)).assertDoesNotExist()
    }

    @Test
    fun thePatientSeesWhoArrived() {
        show(AttentionUiState.Content(attention(status = ServiceStatus.IN_PROGRESS, iAmThePatient = true)))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.attention_hero_in_progress_patient_title, "María Quispe")),
        )
    }

    @Test
    fun aFinishedAttentionOffersNoMark() {
        show(AttentionUiState.Content(professional(ServiceStatus.COMPLETED)))

        markButton(R.string.attention_mark_completed).assertDoesNotExist()
        markButton(R.string.attention_mark_arrival).assertDoesNotExist()
    }

    // docs/decisions.md, 2026-11-04, HU-23: the engine no longer gives the address,
    // and the city still says where it was.
    @Test
    fun withoutTheAddressTheCityStandsInAndThereAreNoDirections() {
        val hidden = attentionPlace(addressText = null, latitude = null, longitude = null)
        show(
            AttentionUiState.Content(
                attention(status = ServiceStatus.COMPLETED, iAmThePatient = false, place = hidden),
            ),
        )

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("La Paz"))
        composeRule.onNodeWithText(string(R.string.attention_directions)).assertDoesNotExist()
    }

    private fun cancelButton() = composeRule.onNodeWithText(string(R.string.attention_cancel_action))

    // RF-10.3.
    @Test
    fun bothPartiesReachTheCancelButtonUntilItIsCompleted() {
        val attentions =
            listOf(
                attention(status = ServiceStatus.ASSIGNED, iAmThePatient = true),
                attention(status = ServiceStatus.IN_PROGRESS, iAmThePatient = false),
            )
        var current by mutableStateOf(attentions.first())
        composeRule.setContentAtDoubleFontScale {
            AttentionContent(uiState = AttentionUiState.Content(current), actions = AttentionActions())
        }

        attentions.forEach { each ->
            current = each
            composeRule.waitForIdle()
            composeRule.assertFitsTheScreen(cancelButton().assertIsEnabled())
        }
    }

    @Test
    fun aMarkOnItsWayBlocksCancelling() {
        show(AttentionUiState.Content(professional(), advancing = true))

        composeRule.assertFitsTheScreen(cancelButton().assertIsNotEnabled())
    }

    // cannotCancelCompletedService.
    @Test
    fun aFinishedAttentionOffersNoCancel() {
        show(AttentionUiState.Content(attention(status = ServiceStatus.COMPLETED)))

        cancelButton().assertDoesNotExist()
    }

    @Test
    fun theCancelDialogKeepsItsTextFieldAndAnswersOnScreen() {
        show(
            AttentionUiState.Content(
                attention(),
                cancelDraft = CancelDraft(error = CancelAttentionError.ReasonEmpty),
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.attention_cancel_dialog_title)),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.attention_cancel_reason_empty)))
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.attention_cancel_back)),
        )
        // The button behind the dialog says the same, so the answer is the last of the two.
        val answers = composeRule.onAllNodesWithText(string(R.string.attention_cancel_action))
        composeRule.assertFitsTheScreenWithoutScrolling(answers[answers.fetchSemanticsNodes().lastIndex])
    }

    @Test
    fun aCancellationOnItsWayCannotBeAnsweredAgain() {
        show(AttentionUiState.Content(attention(), cancelDraft = CancelDraft(reason = "x", sending = true)))

        composeRule.onNodeWithText(string(R.string.attention_cancel_back)).assertIsNotEnabled()
        val answers = composeRule.onAllNodesWithText(string(R.string.attention_cancel_action))
        answers[answers.fetchSemanticsNodes().lastIndex].assertIsNotEnabled()
    }

    private fun cancelled(byMe: Boolean) =
        attention(
            status = ServiceStatus.CANCELLED,
            cancellation = AttentionCancellation("No voy a poder llegar.", byMe, Instant.parse("2026-11-04T20:20:00Z")),
        )

    @Test
    fun theCounterpartSeesWhoCancelledWhyAndThatNothingIsOwed() {
        show(AttentionUiState.Content(cancelled(byMe = false)))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.attention_hero_cancelled_by_other_body, "María Quispe")),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("No voy a poder llegar."))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.attention_no_payment_pending)))
        cancelButton().assertDoesNotExist()
    }

    @Test
    fun whoCancelledIsToldItWasThem() {
        show(AttentionUiState.Content(cancelled(byMe = true)))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.attention_hero_cancelled_by_me_body)),
        )
    }

    // Past its 7 days, the screen must not offer what the engine refuses.
    @Test
    fun afterItsWindowTheProfessionalIsNoLongerToldTheyCanRecord() {
        show(
            AttentionUiState.Content(
                attention(status = ServiceStatus.COMPLETED, iAmThePatient = false, conversationOpen = false),
            ),
        )

        composeRule.onNodeWithText(string(R.string.attention_hero_completed_professional_body)).assertDoesNotExist()
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

package bo.saludencasa.features.disputes.presentation

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.model.DisputeResolution
import bo.saludencasa.features.disputes.paymentDispute
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
class DisputeScreensLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val locale: Locale get() = Locale.getDefault()

    private fun amount() = formatBob(BigDecimal("45.00"), locale)

    private fun showQueue(uiState: DisputeQueueUiState) {
        composeRule.setContentAtDoubleFontScale {
            DisputeQueueContent(uiState = uiState, actions = DisputeQueueActions())
        }
    }

    private fun showReview(uiState: DisputeReviewUiState) {
        composeRule.setContentAtDoubleFontScale {
            DisputeReviewContent(uiState = uiState, actions = DisputeReviewActions())
        }
    }

    @Test
    fun eachDisputeInTheQueueShowsWhatWhoAndSinceWhen() {
        showQueue(DisputeQueueUiState.Content(listOf(paymentDispute()), endReached = false))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Aplicación de inyectables"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(amount()))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.dispute_queue_parties, "Ana Quispe", "María Flores")),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(
                string(R.string.dispute_queue_since, formatAppointment(Instant.parse("2026-11-23T14:15:00Z"), locale)),
            ),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.dispute_queue_load_more)))
    }

    @Test
    fun anEmptyQueueSaysWhatWillAppearThere() {
        showQueue(DisputeQueueUiState.Empty)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.dispute_queue_empty_title)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.dispute_queue_empty_body)))
    }

    // Both answers at the foot, so neither needs a scroll to be found.
    @Test
    fun theReviewKeepsBothResolutionsAtTheFoot() {
        showReview(DisputeReviewUiState.Content(paymentDispute()))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.dispute_review_paid_action)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.dispute_review_not_paid_action)),
        )
    }

    // Criterion 3: amounts, states and parties, and their email to ask them.
    @Test
    fun theReviewShowsTheAmountWhatEachDeclaredAndHowToReachThem() {
        showReview(DisputeReviewUiState.Content(paymentDispute()))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(amount()))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(
                string(
                    R.string.dispute_review_patient_paid_direct_qr,
                    "Ana Quispe",
                    formatAppointment(Instant.parse("2026-11-23T13:40:00Z"), locale),
                ),
            ),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(
                string(
                    R.string.dispute_review_professional_denied,
                    "María Flores",
                    formatAppointment(Instant.parse("2026-11-23T14:15:00Z"), locale),
                ),
            ),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("ana.quispe@ejemplo.com"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("maria.flores@ejemplo.com"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.dispute_review_contact_note)))
    }

    @Test
    fun theResolveDialogKeepsItsErrorAndAnswersOnScreen() {
        showReview(
            DisputeReviewUiState.Content(
                paymentDispute(),
                draft = ResolveDraft(DisputeResolution.NOT_PAID, error = DisputeError.NetworkUnavailable),
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.dispute_review_not_paid_dialog_title)),
        )
        // Reached without a gesture, checked before anything that scrolls.
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.dispute_review_failed_network)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.dispute_review_back)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.dispute_review_not_paid_confirm)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(
                string(R.string.dispute_review_not_paid_dialog_body, amount(), "Ana Quispe", "María Flores"),
            ),
        )
    }

    @Test
    fun aResolutionOnItsWayCannotBeSentAgain() {
        showReview(
            DisputeReviewUiState.Content(
                paymentDispute(),
                draft = ResolveDraft(DisputeResolution.PAID, sending = true),
            ),
        )

        composeRule.onNodeWithText(string(R.string.dispute_review_paid_confirm)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.dispute_review_back)).assertIsNotEnabled()
    }

    @Test
    fun aDisputeAlreadyResolvedSaysSo() {
        showReview(DisputeReviewUiState.Gone)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.dispute_review_gone_title)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.dispute_review_gone_body)))
    }
}

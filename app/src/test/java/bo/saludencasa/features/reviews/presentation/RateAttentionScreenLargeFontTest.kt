package bo.saludencasa.features.reviews.presentation

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewError
import bo.saludencasa.features.reviews.domain.vo.ReviewComment
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.plural
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RateAttentionScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        uiState: RateAttentionUiState,
        actions: RateAttentionActions = RateAttentionActions(),
    ) {
        composeRule.setContentAtDoubleFontScale {
            RateAttentionContent(uiState = uiState, actions = actions)
        }
    }

    private fun content(
        rating: Int = 4,
        comment: String = "Muy puntual",
        confirming: Boolean = false,
        error: SubmitReviewError? = null,
    ) = RateAttentionUiState.Content(
        attention = attention(status = ServiceStatus.COMPLETED, iAmThePatient = true),
        rating = rating,
        comment = comment,
        confirming = confirming,
        error = error,
    )

    private fun star(value: Int) =
        composeRule.onNodeWithContentDescription(plural(R.plurals.cd_review_stars, value, value))

    private fun submitButton() = composeRule.onNodeWithText(string(R.string.review_submit_action))

    @Test
    fun everythingTheRatingAsksForStaysOnScreen() {
        show(content())

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.review_rate_title)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("María Quispe"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.review_rate_question)))
        // Five stars of 48 dp are 240 dp, which is what a 320 dp phone leaves in the card.
        (1..5).forEach { composeRule.assertFitsTheScreen(star(it)) }
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.review_rating_4)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.review_comment_label)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Muy puntual"))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.review_rate_notice, "María Quispe")))
    }

    // Fixed to the foot, so it stays there however long the comment grows.
    @Test
    fun theSubmitButtonIsAlwaysOnScreen() {
        show(content())

        composeRule.assertFitsTheScreenWithoutScrolling(submitButton().assertIsEnabled())
    }

    @Test
    fun theSubmitButtonIsOffUntilAStarIsChosenAndTheLabelWaitsForIt() {
        show(content(rating = 0))

        submitButton().assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.review_rating_4)).assertDoesNotExist()
    }

    @Test
    fun tappingAStarChoosesExactlyThatStar() {
        var chosen = 0
        show(content(rating = 0), RateAttentionActions(onRatingChanged = { chosen = it }))

        star(5).performClick()

        assertEquals(5, chosen)
    }

    @Test
    fun aCommentOverTheLimitSaysHowManyCharactersAreAllowed() {
        show(
            content(
                comment = "x".repeat(ReviewComment.MAX_LENGTH + 1),
                error = SubmitReviewError.CommentTooLong,
            ),
        )

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.error_review_comment_too_long, ReviewComment.MAX_LENGTH)),
        )
    }

    @Test
    fun theConfirmationKeepsItsStarsItsWordsAndBothAnswersOnScreen() {
        show(content(rating = 4, confirming = true))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.review_confirm_title)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(
                plural(R.plurals.review_confirm_body_with_comment, 4, 4, "María Quispe"),
            ),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.review_confirm_back)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.review_confirm_action)),
        )
    }

    // A rating without words does not promise a comment on the profile.
    @Test
    fun theConfirmationOfARatingWithNoWordsDoesNotMentionAComment() {
        show(content(rating = 5, comment = "", confirming = true))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(
                plural(R.plurals.review_confirm_body_without_comment, 5, 5, "María Quispe"),
            ),
        )
    }

    @Test
    fun anAttentionThatCannotBeRatedSaysWhyWithoutOfferingARetry() {
        show(RateAttentionUiState.Unavailable(ReviewError.AlreadyRated))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.error_review_already_rated)))
        composeRule.onNodeWithText(string(R.string.common_retry)).assertDoesNotExist()
    }

    @Test
    fun aFailureToReadOffersTheRetryOnScreen() {
        show(RateAttentionUiState.Unavailable(ReviewError.NetworkUnavailable))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.error_network_unavailable)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    // HU-30: the same screen, said to the professional about the patient.
    private fun professionalContent(
        rating: Int = 4,
        comment: String = "Esperaba en la puerta.",
        confirming: Boolean = false,
    ) = RateAttentionUiState.Content(
        attention = attention(status = ServiceStatus.COMPLETED, iAmThePatient = false),
        rating = rating,
        comment = comment,
        confirming = confirming,
    )

    @Test
    fun theProfessionalIsToldThePatientWillNotSeeTheRatingAndWhatOthersWillSee() {
        show(professionalContent())

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.review_rate_question_professional)),
        )
        (1..5).forEach { composeRule.assertFitsTheScreen(star(it)) }
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.review_rate_notice_professional, "María Quispe")),
        )
        composeRule.onNodeWithText(string(R.string.review_rate_question)).assertDoesNotExist()
        composeRule.assertFitsTheScreenWithoutScrolling(submitButton().assertIsEnabled())
    }

    @Test
    fun theProfessionalsConfirmationSaysWhatOtherProfessionalsWillSee() {
        show(professionalContent(confirming = true))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(
                plural(R.plurals.review_confirm_body_with_comment_professional, 4, 4, "María Quispe"),
            ),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.review_confirm_action)),
        )
    }

    @Test
    fun theProfessionalsConfirmationOfARatingWithNoWordsDoesNotMentionAComment() {
        show(professionalContent(rating = 2, comment = "", confirming = true))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(
                plural(R.plurals.review_confirm_body_without_comment_professional, 2, 2, "María Quispe"),
            ),
        )
    }
}

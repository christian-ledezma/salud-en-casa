@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.reviews.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.reviews.FakeReviewRepository
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewError
import bo.saludencasa.features.reviews.domain.usecase.SubmitReviewUseCase
import bo.saludencasa.features.reviews.domain.vo.ReviewComment
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.MyReview
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class RateAttentionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val services = FakeServiceRepository()
    private val reviews = FakeReviewRepository()

    // The view model reads the attention as it is created, on the test dispatcher: the
    // read lands only once the dispatcher has been advanced.
    private fun TestScope.viewModel(initialRating: Int = 0): RateAttentionViewModel {
        val viewModel =
            RateAttentionViewModel(
                requestId = "r1",
                initialRating = initialRating,
                getAttention = GetAttentionUseCase(services),
                submitReview = SubmitReviewUseCase(reviews, GetAttentionUseCase(services)),
            )
        advanceUntilIdle()
        return viewModel
    }

    private fun rateable() = attention(status = ServiceStatus.COMPLETED, iAmThePatient = true)

    private fun RateAttentionViewModel.content() = uiState.value as RateAttentionUiState.Content

    private fun TestScope.opened(initialRating: Int = 0): RateAttentionViewModel {
        services.result = AttentionResult.Loaded(rateable())
        return viewModel(initialRating)
    }

    @Test
    fun theStarTappedOnTheAttentionArrivesAlreadyChosen() =
        runTest {
            val viewModel = opened(initialRating = 4)

            assertEquals(4, viewModel.content().rating)
            assertTrue(viewModel.content().canSubmit)
        }

    @Test
    fun aStarOutsideOneToFiveIsNoStarAtAll() =
        runTest {
            assertEquals(0, opened(initialRating = 9).content().rating)
            assertEquals(0, opened(initialRating = -2).content().rating)
        }

    @Test
    fun withoutStarsTheRatingCannotBeSent() =
        runTest {
            val viewModel = opened()

            viewModel.onSubmitRequested()

            assertFalse(viewModel.content().canSubmit)
            assertFalse(viewModel.content().confirming)
        }

    @Test
    fun sendingAsksForConfirmationFirstAndSendsNothingYet() =
        runTest {
            val viewModel = opened(initialRating = 4)
            viewModel.onCommentChanged("Muy puntual")

            viewModel.onSubmitRequested()

            assertTrue(viewModel.content().confirming)
            assertTrue(reviews.submissions.isEmpty())
        }

    @Test
    fun goingBackFromTheConfirmationKeepsTheStarsAndTheWords() =
        runTest {
            val viewModel = opened(initialRating = 4)
            viewModel.onCommentChanged("Muy puntual")
            viewModel.onSubmitRequested()

            viewModel.onConfirmDismissed()

            assertFalse(viewModel.content().confirming)
            assertEquals(4, viewModel.content().rating)
            assertEquals("Muy puntual", viewModel.content().comment)
            assertTrue(reviews.submissions.isEmpty())
        }

    @Test
    fun confirmingSendsTheRatingOnceAndLeavesTheScreen() =
        runTest {
            val viewModel = opened(initialRating = 4)
            viewModel.onCommentChanged("Muy puntual")
            viewModel.onSubmitRequested()

            viewModel.onConfirmed()
            advanceUntilIdle()

            assertEquals(RateAttentionUiState.Submitted, viewModel.uiState.value)
            assertEquals(
                listOf(FakeReviewRepository.Submission("s1", "p1", 4, "Muy puntual")),
                reviews.submissions,
            )
        }

    @Test
    fun aSecondTapOnConfirmWhileTheFirstIsInFlightSendsNothingMore() =
        runTest {
            val viewModel = opened(initialRating = 5)
            reviews.submitGate = CompletableDeferred()
            viewModel.onSubmitRequested()

            viewModel.onConfirmed()
            viewModel.onConfirmed()
            advanceUntilIdle()
            reviews.submitGate?.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, reviews.submissions.size)
        }

    @Test
    fun aCommentOverTheLimitIsSaidBeforeAnyConfirmation() =
        runTest {
            val viewModel = opened(initialRating = 5)
            viewModel.onCommentChanged("x".repeat(ReviewComment.MAX_LENGTH + 1))

            viewModel.onSubmitRequested()

            assertEquals(SubmitReviewError.CommentTooLong, viewModel.content().error)
            assertFalse(viewModel.content().confirming)
            assertTrue(reviews.submissions.isEmpty())
        }

    // A refusal must not cost the person the words they wrote.
    @Test
    fun aRefusedRatingKeepsTheFormWithTheStarsAndTheWordsTyped() =
        runTest {
            val viewModel = opened(initialRating = 3)
            viewModel.onCommentChanged("Llegó tarde")
            reviews.submitFailure = ReviewError.NetworkUnavailable
            viewModel.onSubmitRequested()

            viewModel.onConfirmed()
            advanceUntilIdle()

            val content = viewModel.content()
            assertEquals(SubmitReviewError.Review(ReviewError.NetworkUnavailable), content.error)
            assertFalse(content.sending)
            assertEquals(3, content.rating)
            assertEquals("Llegó tarde", content.comment)
        }

    @Test
    fun changingTheStarsOrTheWordsClearsTheLastError() =
        runTest {
            val viewModel = opened(initialRating = 3)
            reviews.submitFailure = ReviewError.NetworkUnavailable
            viewModel.onSubmitRequested()
            viewModel.onConfirmed()
            advanceUntilIdle()

            viewModel.onRatingChanged(5)

            assertEquals(null, viewModel.content().error)
        }

    @Test
    fun anAttentionAlreadyRatedShowsNoFormAndSaysSo() =
        runTest {
            services.result =
                AttentionResult.Loaded(
                    attention(
                        status = ServiceStatus.COMPLETED,
                        iAmThePatient = true,
                        myReview = MyReview(4, null, Instant.parse("2026-11-26T16:00:00Z")),
                    ),
                )

            assertEquals(
                RateAttentionUiState.Unavailable(ReviewError.AlreadyRated),
                viewModel(4).uiState.value,
            )
        }

    @Test
    fun anAttentionNotCompletedShowsNoForm() =
        runTest {
            services.result =
                AttentionResult.Loaded(attention(status = ServiceStatus.IN_PROGRESS, iAmThePatient = true))

            assertEquals(
                RateAttentionUiState.Unavailable(ReviewError.NotAllowed),
                viewModel(4).uiState.value,
            )
        }

    @Test
    fun anAttentionThatIsNotThereShowsNoForm() =
        runTest {
            services.result = AttentionResult.NotFound

            assertEquals(
                RateAttentionUiState.Unavailable(ReviewError.NotAllowed),
                viewModel(4).uiState.value,
            )
        }

    @Test
    fun aFailureToReadCanBeRetriedAndTheFormComesBack() =
        runTest {
            services.result = AttentionResult.Failure(ServiceError.NetworkUnavailable)
            val viewModel = viewModel(4)
            assertEquals(RateAttentionUiState.Unavailable(ReviewError.NetworkUnavailable), viewModel.uiState.value)

            services.result = AttentionResult.Loaded(rateable())
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(4, viewModel.content().rating)
        }
}

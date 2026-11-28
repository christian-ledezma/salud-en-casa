package bo.saludencasa.features.reviews.domain.usecase

import bo.saludencasa.features.reviews.FakeReviewRepository
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewResult
import bo.saludencasa.features.reviews.domain.vo.ReviewComment
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.MyReview
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SubmitReviewUseCaseTest {
    private val reviews = FakeReviewRepository()
    private val services = FakeServiceRepository()
    private val useCase = SubmitReviewUseCase(reviews, GetAttentionUseCase(services))

    private fun rateable(): Attention = attention(status = ServiceStatus.COMPLETED, iAmThePatient = true)

    private fun failure(error: SubmitReviewError) = SubmitReviewResult.Failure(error)

    private fun refusedAs(error: ReviewError) = failure(SubmitReviewError.Review(error))

    @Test
    fun theRatingGoesToTheServiceAndTheProfessionalOfTheAttentionAsTheEngineReadsIt() =
        runTest {
            services.result = AttentionResult.Loaded(rateable())

            assertEquals(SubmitReviewResult.Submitted, useCase("r1", 4, "  Muy puntual \n"))

            // s1 and p1 are the service and the counterpart of the attention: no other
            // identifier reaches the repository, whatever the screen held.
            assertEquals(
                listOf(FakeReviewRepository.Submission("s1", "p1", 4, "Muy puntual")),
                reviews.submissions,
            )
        }

    @Test
    fun aBlankCommentIsARatingWithNoWordsAndTravelsAsNone() =
        runTest {
            services.result = AttentionResult.Loaded(rateable())

            assertEquals(SubmitReviewResult.Submitted, useCase("r1", 5, "   "))

            assertNull(reviews.submissions.single().comment)
        }

    @Test
    fun noStarsIsRefusedWithoutReadingTheAttentionOrSending() =
        runTest {
            services.result = AttentionResult.Loaded(rateable())

            assertEquals(failure(SubmitReviewError.RatingMissing), useCase("r1", 0, "ok"))
            assertEquals(failure(SubmitReviewError.RatingMissing), useCase("r1", 6, "ok"))

            assertTrue(reviews.submissions.isEmpty())
        }

    @Test
    fun aCommentOverTheLimitIsRefusedAndNeverLeavesThePhone() =
        runTest {
            services.result = AttentionResult.Loaded(rateable())

            assertEquals(
                failure(SubmitReviewError.CommentTooLong),
                useCase("r1", 3, "x".repeat(ReviewComment.MAX_LENGTH + 1)),
            )

            assertTrue(reviews.submissions.isEmpty())
        }

    // RN-10: only a completed service is rated.
    @Test
    fun anAttentionThatIsNotCompletedIsNeverRated() =
        runTest {
            listOf(ServiceStatus.ASSIGNED, ServiceStatus.IN_PROGRESS, ServiceStatus.CANCELLED).forEach { status ->
                services.result = AttentionResult.Loaded(attention(status = status, iAmThePatient = true))

                assertEquals(status.name, refusedAs(ReviewError.NotAllowed), useCase("r1", 5, "ok"))
            }

            assertTrue(reviews.submissions.isEmpty())
        }

    // RF-12.2, HU-30: the professional of the attention rates the patient, who is its
    // counterpart, with the same rules.
    @Test
    fun theProfessionalRatesThePatientOfTheAttention() =
        runTest {
            services.result =
                AttentionResult.Loaded(
                    attention(status = ServiceStatus.COMPLETED, iAmThePatient = false, counterpartId = "pat-1"),
                )

            assertEquals(SubmitReviewResult.Submitted, useCase("r1", 4, "Esperaba en la puerta."))

            assertEquals(
                listOf(FakeReviewRepository.Submission("s1", "pat-1", 4, "Esperaba en la puerta.")),
                reviews.submissions,
            )
        }

    @Test
    fun theProfessionalCannotRateAnAttentionThatIsNotCompleted() =
        runTest {
            services.result =
                AttentionResult.Loaded(attention(status = ServiceStatus.IN_PROGRESS, iAmThePatient = false))

            assertEquals(refusedAs(ReviewError.NotAllowed), useCase("r1", 5, "ok"))
            assertTrue(reviews.submissions.isEmpty())
        }

    @Test
    fun theProfessionalCannotRateTwiceTheSameAttention() =
        runTest {
            val rated =
                attention(
                    status = ServiceStatus.COMPLETED,
                    iAmThePatient = false,
                    myReview = MyReview(4, null, Instant.parse("2026-11-27T12:00:00Z")),
                )
            services.result = AttentionResult.Loaded(rated)

            assertEquals(refusedAs(ReviewError.AlreadyRated), useCase("r1", 5, "ok"))
            assertTrue(reviews.submissions.isEmpty())
        }

    @Test
    fun anAttentionAlreadyRatedIsNotRatedTwiceAndNothingIsSent() =
        runTest {
            val rated =
                attention(
                    status = ServiceStatus.COMPLETED,
                    iAmThePatient = true,
                    myReview = MyReview(4, null, Instant.parse("2026-11-26T12:00:00Z")),
                )
            services.result = AttentionResult.Loaded(rated)

            assertEquals(refusedAs(ReviewError.AlreadyRated), useCase("r1", 5, "ok"))
            assertTrue(reviews.submissions.isEmpty())
        }

    @Test
    fun anAttentionThatIsNotThereIsNotRated() =
        runTest {
            services.result = AttentionResult.NotFound

            assertEquals(refusedAs(ReviewError.NotAllowed), useCase("r1", 5, "ok"))
            assertTrue(reviews.submissions.isEmpty())
        }

    @Test
    fun aFailureToReadTheAttentionIsReportedAndNothingIsSent() =
        runTest {
            services.result = AttentionResult.Failure(ServiceError.NetworkUnavailable)

            assertEquals(refusedAs(ReviewError.NetworkUnavailable), useCase("r1", 5, "ok"))
            assertTrue(reviews.submissions.isEmpty())
        }

    // Two phones: the second reads the attention before the first has landed.
    @Test
    fun theEnginesRefusalReachesTheScreenAsItWas() =
        runTest {
            services.result = AttentionResult.Loaded(rateable())
            reviews.submitFailure = ReviewError.AlreadyRated

            assertEquals(refusedAs(ReviewError.AlreadyRated), useCase("r1", 5, "ok"))
        }
}

package bo.saludencasa.features.subscription.domain.usecase

import bo.saludencasa.features.subscription.FakeSubscriptionRepository
import bo.saludencasa.features.subscription.domain.model.SubscriptionError
import bo.saludencasa.features.subscription.domain.model.SubscriptionResult
import bo.saludencasa.features.subscription.domain.model.SubscriptionState
import bo.saludencasa.features.subscription.domain.model.SubscriptionStatus
import bo.saludencasa.features.subscription.required
import bo.saludencasa.features.subscription.subscription
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMySubscriptionUseCaseTest {
    private val repository = FakeSubscriptionRepository()
    private val getMySubscription = GetMySubscriptionUseCase(repository)

    // Criterion 6 of HU-38: with the requirement off nothing changes, so an
    // expired trial is not shown either. Showing it would warn about a
    // consequence that does not happen.
    @Test
    fun withTheRequirementOffAnExpiredTrialIsNotShown() =
        runTest {
            repository.state =
                SubscriptionState(required = false, subscription = subscription(SubscriptionStatus.EXPIRED))

            assertEquals(SubscriptionResult.None, getMySubscription())
        }

    // A professional whose verification was never approved has no period: the
    // home screen already says they are not verified, which is the reason.
    @Test
    fun withTheRequirementOnAndNoPeriodThereIsNothingToShow() =
        runTest {
            repository.state = required(subscription = null)

            assertEquals(SubscriptionResult.None, getMySubscription())
        }

    @Test
    fun withTheRequirementOnAnExpiredTrialIsShown() =
        runTest {
            val expired = subscription(SubscriptionStatus.EXPIRED)
            repository.state = required(expired)

            assertEquals(SubscriptionResult.Found(expired), getMySubscription())
        }

    @Test
    fun aFailedReadIsAFailureAndNotNothingToShow() =
        runTest {
            repository.error = SubscriptionError.NetworkUnavailable

            assertEquals(SubscriptionResult.Failure(SubscriptionError.NetworkUnavailable), getMySubscription())
        }
}

package bo.saludencasa.features.subscription

import bo.saludencasa.features.subscription.domain.model.Subscription
import bo.saludencasa.features.subscription.domain.model.SubscriptionError
import bo.saludencasa.features.subscription.domain.model.SubscriptionState
import bo.saludencasa.features.subscription.domain.model.SubscriptionStateResult
import bo.saludencasa.features.subscription.domain.model.SubscriptionStatus
import bo.saludencasa.features.subscription.domain.repository.ISubscriptionRepository
import java.time.Instant

class FakeSubscriptionRepository(
    var state: SubscriptionState = SubscriptionState(required = false, subscription = null),
    var error: SubscriptionError? = null,
) : ISubscriptionRepository {
    var reads: Int = 0
        private set

    override suspend fun getMine(): SubscriptionStateResult {
        reads++
        return error?.let(SubscriptionStateResult::Failure) ?: SubscriptionStateResult.Loaded(state)
    }
}

fun subscription(
    status: SubscriptionStatus = SubscriptionStatus.TRIAL,
    endsAt: Instant = Instant.parse("2026-12-24T16:00:00Z"),
): Subscription = Subscription(status, endsAt)

fun required(subscription: Subscription?): SubscriptionState =
    SubscriptionState(required = true, subscription = subscription)

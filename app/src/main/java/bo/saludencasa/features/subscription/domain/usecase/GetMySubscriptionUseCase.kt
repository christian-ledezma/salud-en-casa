package bo.saludencasa.features.subscription.domain.usecase

import bo.saludencasa.features.subscription.domain.model.SubscriptionResult
import bo.saludencasa.features.subscription.domain.model.SubscriptionStateResult
import bo.saludencasa.features.subscription.domain.repository.ISubscriptionRepository

// docs/decisions.md, 2026-11-24, HU-38, nothing is shown with the requirement off.
class GetMySubscriptionUseCase(
    private val repository: ISubscriptionRepository,
) {
    suspend operator fun invoke(): SubscriptionResult =
        when (val result = repository.getMine()) {
            is SubscriptionStateResult.Loaded -> {
                val subscription = result.state.subscription
                if (result.state.required && subscription != null) {
                    SubscriptionResult.Found(subscription)
                } else {
                    SubscriptionResult.None
                }
            }

            is SubscriptionStateResult.Failure -> {
                SubscriptionResult.Failure(result.error)
            }
        }
}

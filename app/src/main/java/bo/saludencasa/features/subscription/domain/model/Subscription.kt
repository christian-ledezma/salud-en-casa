package bo.saludencasa.features.subscription.domain.model

import java.time.Instant

// TRIAL is the only source today; PLAY_BILLING joins in Sprint 11 (HU-39).
enum class SubscriptionStatus { TRIAL, EXPIRED }

data class Subscription(
    val status: SubscriptionStatus,
    val endsAt: Instant,
) {
    val isValid: Boolean get() = status != SubscriptionStatus.EXPIRED
}

// What the engine says about the caller: whether the platform requires a
// subscription, and the latest period, if there is one.
data class SubscriptionState(
    val required: Boolean,
    val subscription: Subscription?,
)

sealed interface SubscriptionError {
    data object NotSignedIn : SubscriptionError

    // A row this version cannot read, such as a status it does not know.
    data object UnreadableResult : SubscriptionError

    data object NetworkUnavailable : SubscriptionError

    data object Unexpected : SubscriptionError
}

sealed interface SubscriptionStateResult {
    data class Loaded(
        val state: SubscriptionState,
    ) : SubscriptionStateResult

    data class Failure(
        val error: SubscriptionError,
    ) : SubscriptionStateResult
}

sealed interface SubscriptionResult {
    data class Found(
        val subscription: Subscription,
    ) : SubscriptionResult

    // Nothing to show: the requirement is off, or there is no period yet
    // because the verification was never approved (HU-38).
    data object None : SubscriptionResult

    data class Failure(
        val error: SubscriptionError,
    ) : SubscriptionResult
}

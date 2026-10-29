package bo.saludencasa.features.notifications.domain.model

import bo.saludencasa.features.profile.domain.model.ProfileError

sealed interface NotificationDestination {
    data object Inbox : NotificationDestination

    data object MyRequests : NotificationDestination

    // The one screen of a negotiation (docs/decisions.md, 2026-10-27, HT-24). A
    // null professionalId opens it as its professional: a request for them, not
    // yet offered on. With one, the thread of that professional, for either side.
    data class OfferThread(
        val requestId: String,
        val professionalId: String?,
    ) : NotificationDestination

    // The sealed agreement of a request (HU-19), opened by request: a service has
    // one, and each party resolves their own side.
    data class Attention(
        val requestId: String,
    ) : NotificationDestination
}

enum class DiscardReason {
    MALFORMED,
    NO_SESSION,
    OTHER_ACCOUNT,
}

sealed interface IncomingNotification {
    data class Show(
        val payload: PushPayload,
    ) : IncomingNotification

    data class Discarded(
        val reason: DiscardReason,
    ) : IncomingNotification
}

sealed interface NotificationOpenResult {
    data class Open(
        val destination: NotificationDestination,
    ) : NotificationOpenResult

    data object Dropped : NotificationOpenResult

    data class Failure(
        val error: ProfileError,
    ) : NotificationOpenResult
}

sealed interface NotificationError {
    data object NotSignedIn : NotificationError

    data object NetworkUnavailable : NotificationError

    data object Unexpected : NotificationError
}

sealed interface DeviceResult {
    data object Success : DeviceResult

    data class Failure(
        val error: NotificationError,
    ) : DeviceResult
}

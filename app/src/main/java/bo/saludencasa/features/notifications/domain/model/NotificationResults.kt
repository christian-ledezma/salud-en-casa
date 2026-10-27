package bo.saludencasa.features.notifications.domain.model

import bo.saludencasa.features.profile.domain.model.ProfileError

sealed interface NotificationDestination {
    data class Inbox(
        val highlightRequestId: String?,
    ) : NotificationDestination

    data object MyRequests : NotificationDestination

    // HU-17: the patient opens the offers on their request. docs/decisions.md,
    // 2026-10-23.
    data class Negotiation(
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

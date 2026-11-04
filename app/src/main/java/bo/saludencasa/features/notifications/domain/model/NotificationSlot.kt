package bo.saludencasa.features.notifications.domain.model

// A shown notification is its request plus its slot. docs/decisions.md,
// 2026-11-03, HU-21: a message keeps its own slot, so it never replaces another
// event of the same request.
enum class NotificationSlot {
    EVENT,
    MESSAGE,
}

fun PushPayload.slot(): NotificationSlot =
    when (this) {
        is PushPayload.MessageReceived -> NotificationSlot.MESSAGE
        else -> NotificationSlot.EVENT
    }

package bo.saludencasa.features.conversation.presentation

import bo.saludencasa.core.util.AppointmentZone
import java.time.LocalDate

// What the reversed list draws, newest first: the bubbles, a day label above
// each day's messages, and at the top either the older page or the start.
internal sealed interface ConversationRow {
    val key: String

    data class Bubble(
        val item: ConversationItem,
        val showsReceipt: Boolean,
    ) : ConversationRow {
        override val key: String get() = item.key
    }

    data class Day(
        val date: LocalDate,
    ) : ConversationRow {
        override val key: String get() = "day-$date"
    }

    data class Older(
        val state: OlderMessages,
    ) : ConversationRow {
        override val key: String get() = "older"
    }

    data object Start : ConversationRow {
        override val key: String get() = "start"
    }
}

internal fun conversationRows(
    items: List<ConversationItem>,
    older: OlderMessages,
    today: LocalDate,
): List<ConversationRow> {
    if (items.isEmpty()) return emptyList()

    val receiptKey = items.lastOrNull { it.sentByMe }?.key
    val rows = mutableListOf<ConversationRow>()
    var day: LocalDate? = null
    for (item in items.asReversed()) {
        val itemDay = item.day(today)
        if (day != null && itemDay != day) rows += ConversationRow.Day(day)
        day = itemDay
        rows += ConversationRow.Bubble(item, showsReceipt = item.key == receiptKey)
    }
    day?.let { rows += ConversationRow.Day(it) }

    rows +=
        when (older) {
            OlderMessages.EXHAUSTED -> ConversationRow.Start
            OlderMessages.IDLE, OlderMessages.LOADING, OlderMessages.FAILED -> ConversationRow.Older(older)
        }
    return rows
}

// What has not reached the engine yet is being written now.
private fun ConversationItem.day(today: LocalDate): LocalDate =
    when (this) {
        is ConversationItem.Delivered -> message.createdAt.atZone(AppointmentZone).toLocalDate()
        is ConversationItem.Sending, is ConversationItem.NotSent -> today
    }

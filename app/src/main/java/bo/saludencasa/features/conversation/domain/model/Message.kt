package bo.saludencasa.features.conversation.domain.model

import java.time.Instant

data class Message(
    val id: String,
    val content: String,
    val sentByMe: Boolean,
    val createdAt: Instant,
    val readAt: Instant?,
    val isWorkRecord: Boolean = false,
)

// docs/decisions.md, 2026-11-03, HU-21: the id breaks a tie in time.
data class MessageCursor(
    val createdAt: Instant,
    val id: String,
) {
    companion object {
        // The nil uuid sorts before every other in the engine, so this cursor sits
        // before every message of that instant.
        fun startOf(instant: Instant): MessageCursor = MessageCursor(instant, NIL_UUID)

        private const val NIL_UUID = "00000000-0000-0000-0000-000000000000"
    }
}

fun Message.cursor(): MessageCursor = MessageCursor(createdAt, id)

// The engine's order: a lowercase uuid compares as text the way the engine compares it.
fun List<Message>.inChronologicalOrder(): List<Message> = sortedWith(compareBy({ it.createdAt }, { it.id }))

// A fetched row replaces the one on screen with the same id, which is how a read
// receipt arrives, and pages that overlap never repeat a message.
fun List<Message>.mergedWith(incoming: List<Message>): List<Message> =
    (associateBy(Message::id) + incoming.associateBy(Message::id)).values.toList().inChronologicalOrder()

sealed interface MessageQuery {
    data object Newest : MessageQuery

    data class OlderThan(
        val cursor: MessageCursor,
    ) : MessageQuery

    data class NewerThan(
        val cursor: MessageCursor,
    ) : MessageQuery
}

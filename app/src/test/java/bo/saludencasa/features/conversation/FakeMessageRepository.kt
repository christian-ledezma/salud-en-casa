package bo.saludencasa.features.conversation

import bo.saludencasa.features.conversation.domain.model.MarkReadResult
import bo.saludencasa.features.conversation.domain.model.Message
import bo.saludencasa.features.conversation.domain.model.MessageCursor
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.conversation.domain.model.MessageQuery
import bo.saludencasa.features.conversation.domain.model.MessagesResult
import bo.saludencasa.features.conversation.domain.model.SendMessageResult
import bo.saludencasa.features.conversation.domain.model.cursor
import bo.saludencasa.features.conversation.domain.model.inChronologicalOrder
import bo.saludencasa.features.conversation.domain.repository.IMessageRepository
import bo.saludencasa.features.conversation.domain.vo.MessageContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.Instant

const val REQUEST_ID = "61eb5448-0000-4000-8000-000000000000"

// The engine in memory: it pages by cursor the way conversation_page does, and
// stamps the dates the way the triggers do, so a test drives real paging.
class FakeMessageRepository : IMessageRepository {
    val stored = mutableListOf<Message>()
    val sent = mutableListOf<Pair<String, String>>()
    var markReadCalls = 0
    var fetchError: MessageError? = null
    var sendError: MessageError? = null
    var holdSend: CompletableDeferred<Unit>? = null
    var holdFetch: CompletableDeferred<Unit>? = null
    private var clock: Instant = Instant.parse("2026-11-03T13:00:00Z")

    val signals = MutableSharedFlow<Unit>()

    fun nextInstant(): Instant {
        clock = clock.plusSeconds(1)
        return clock
    }

    fun receive(content: String): Message = store(content, sentByMe = false)

    fun store(
        content: String,
        sentByMe: Boolean,
        id: String = "m-${stored.size.toString().padStart(4, '0')}",
    ): Message {
        val message = Message(id, content, sentByMe, nextInstant(), readAt = null)
        stored += message
        return message
    }

    // What the counterpart's mark_conversation_read does to what I wrote.
    fun counterpartReadsEverything() {
        val at = nextInstant()
        stored.replaceAll { if (it.sentByMe && it.readAt == null) it.copy(readAt = at) else it }
    }

    override suspend fun fetch(
        requestId: String,
        query: MessageQuery,
        limit: Int,
    ): MessagesResult {
        fetchError?.let { return MessagesResult.Failure(it) }
        val ordered = stored.inChronologicalOrder()
        val page =
            when (query) {
                MessageQuery.Newest -> ordered.takeLast(limit)
                is MessageQuery.OlderThan -> ordered.filter { it.cursor() < query.cursor }.takeLast(limit)
                is MessageQuery.NewerThan -> ordered.filter { it.cursor() > query.cursor }.take(limit)
            }
        // The snapshot is taken before the hold: what arrives meanwhile is not in it.
        holdFetch?.await()
        return MessagesResult.Loaded(page)
    }

    override suspend fun send(
        requestId: String,
        messageId: String,
        content: MessageContent,
    ): SendMessageResult {
        sent += messageId to content.value
        sendError?.let { return SendMessageResult.Failure(it) }
        val existing = stored.firstOrNull { it.id == messageId }
        val message = existing ?: store(content.value, sentByMe = true, id = messageId)
        // The engine has the row; only the reply is late.
        holdSend?.await()
        return SendMessageResult.Sent(message)
    }

    override suspend fun markRead(requestId: String): MarkReadResult {
        markReadCalls++
        val at = nextInstant()
        stored.replaceAll { if (!it.sentByMe && it.readAt == null) it.copy(readAt = at) else it }
        return MarkReadResult.Marked
    }

    override fun signals(requestId: String): Flow<Unit> = signals
}

private operator fun MessageCursor.compareTo(other: MessageCursor): Int =
    compareValuesBy(this, other, MessageCursor::createdAt, MessageCursor::id)

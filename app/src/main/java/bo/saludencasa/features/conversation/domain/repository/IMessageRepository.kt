package bo.saludencasa.features.conversation.domain.repository

import bo.saludencasa.features.conversation.domain.model.MarkReadResult
import bo.saludencasa.features.conversation.domain.model.MessageQuery
import bo.saludencasa.features.conversation.domain.model.MessagesResult
import bo.saludencasa.features.conversation.domain.model.SendMessageResult
import bo.saludencasa.features.conversation.domain.vo.MessageContent
import kotlinx.coroutines.flow.Flow

interface IMessageRepository {
    suspend fun fetch(
        requestId: String,
        query: MessageQuery,
        limit: Int,
    ): MessagesResult

    // The id is chosen by the phone, so a retry after an ambiguous failure cannot
    // write the message twice (docs/decisions.md, 2026-11-03, HU-21).
    suspend fun send(
        requestId: String,
        messageId: String,
        content: MessageContent,
    ): SendMessageResult

    suspend fun markRead(requestId: String): MarkReadResult

    fun signals(requestId: String): Flow<Unit>
}

package bo.saludencasa.features.conversation.data.repository

import bo.saludencasa.features.conversation.data.datasource.SupabaseMessageDataSource
import bo.saludencasa.features.conversation.data.mapper.isAlreadyStored
import bo.saludencasa.features.conversation.data.mapper.toMessage
import bo.saludencasa.features.conversation.data.mapper.toMessageError
import bo.saludencasa.features.conversation.data.model.ConversationPageParams
import bo.saludencasa.features.conversation.data.model.MessageDto
import bo.saludencasa.features.conversation.data.model.NewMessageRow
import bo.saludencasa.features.conversation.domain.model.MarkReadResult
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.conversation.domain.model.MessageQuery
import bo.saludencasa.features.conversation.domain.model.MessagesResult
import bo.saludencasa.features.conversation.domain.model.SendMessageResult
import bo.saludencasa.features.conversation.domain.model.inChronologicalOrder
import bo.saludencasa.features.conversation.domain.repository.IMessageRepository
import bo.saludencasa.features.conversation.domain.vo.MessageContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class MessageRepository(
    private val dataSource: SupabaseMessageDataSource,
) : IMessageRepository {
    override suspend fun fetch(
        requestId: String,
        query: MessageQuery,
        limit: Int,
    ): MessagesResult {
        val me = dataSource.currentUserId() ?: return MessagesResult.Failure(MessageError.NotSignedIn)
        val params =
            when (query) {
                MessageQuery.Newest -> {
                    ConversationPageParams(requestId, limit)
                }

                is MessageQuery.OlderThan -> {
                    ConversationPageParams(
                        requestId = requestId,
                        limit = limit,
                        beforeCreatedAt = query.cursor.createdAt.toString(),
                        beforeId = query.cursor.id,
                    )
                }

                is MessageQuery.NewerThan -> {
                    ConversationPageParams(
                        requestId = requestId,
                        limit = limit,
                        afterCreatedAt = query.cursor.createdAt.toString(),
                        afterId = query.cursor.id,
                    )
                }
            }

        return try {
            val messages =
                dataSource.fetchPage(params).map { dto ->
                    dto.toMessage(me) ?: return MessagesResult.Failure(MessageError.Unexpected)
                }
            MessagesResult.Loaded(messages.inChronologicalOrder())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            MessagesResult.Failure(failure.toMessageError())
        }
    }

    override suspend fun send(
        requestId: String,
        messageId: String,
        content: MessageContent,
        isWorkRecord: Boolean,
    ): SendMessageResult {
        val me = dataSource.currentUserId() ?: return SendMessageResult.Failure(MessageError.NotSignedIn)
        val row =
            NewMessageRow(
                id = messageId,
                requestId = requestId,
                senderId = me,
                content = content.value,
                isWorkRecord = isWorkRecord,
            )

        return try {
            dataSource.insert(row).toSent(me)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            if (failure.isAlreadyStored()) return alreadyStored(messageId, me)
            SendMessageResult.Failure(failure.toMessageError(isWorkRecord))
        }
    }

    override suspend fun markRead(requestId: String): MarkReadResult =
        try {
            dataSource.markRead(requestId)
            MarkReadResult.Marked
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            MarkReadResult.Failure(failure.toMessageError())
        }

    override fun signals(requestId: String): Flow<Unit> {
        if (dataSource.currentUserId() == null) return emptyFlow()
        return dataSource.signals(requestId)
    }

    private suspend fun alreadyStored(
        messageId: String,
        me: String,
    ): SendMessageResult =
        try {
            dataSource.fetchOne(messageId)?.toSent(me) ?: SendMessageResult.Failure(MessageError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            SendMessageResult.Failure(failure.toMessageError())
        }

    private fun MessageDto.toSent(me: String): SendMessageResult =
        toMessage(me)?.let(SendMessageResult::Sent) ?: SendMessageResult.Failure(MessageError.Unexpected)
}

package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.domain.model.Message
import bo.saludencasa.features.conversation.domain.model.MessageCursor
import bo.saludencasa.features.conversation.domain.model.MessagePageResult
import bo.saludencasa.features.conversation.domain.model.MessageQuery
import bo.saludencasa.features.conversation.domain.model.MessagesResult
import bo.saludencasa.features.conversation.domain.model.cursor

// What a signal asks for: everything newer than what is on screen, page after
// page until one comes back short, so nothing is left in a gap. While my last
// message is unread the walk starts at it, so its read receipt comes back too.
class CatchUpMessagesUseCase(
    private val getMessages: GetMessagesUseCase,
) {
    suspend operator fun invoke(
        requestId: String,
        loaded: List<Message>,
    ): MessagesResult {
        var anchor =
            loaded
                .lastOrNull { it.sentByMe }
                ?.takeIf { it.readAt == null }
                ?.let { MessageCursor.startOf(it.createdAt) }
                ?: loaded.lastOrNull()?.cursor()
                ?: return getMessages(requestId, MessageQuery.Newest).toMessagesResult()

        val caughtUp = mutableListOf<Message>()
        while (true) {
            when (val page = getMessages(requestId, MessageQuery.NewerThan(anchor))) {
                is MessagePageResult.Failure -> {
                    return MessagesResult.Failure(page.error)
                }

                is MessagePageResult.Loaded -> {
                    caughtUp += page.messages
                    if (!page.full) return MessagesResult.Loaded(caughtUp)
                    anchor = page.messages.last().cursor()
                }
            }
        }
    }

    private fun MessagePageResult.toMessagesResult(): MessagesResult =
        when (this) {
            is MessagePageResult.Loaded -> MessagesResult.Loaded(messages)
            is MessagePageResult.Failure -> MessagesResult.Failure(error)
        }
}

package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.domain.model.MessagePageResult
import bo.saludencasa.features.conversation.domain.model.MessageQuery
import bo.saludencasa.features.conversation.domain.model.MessagesResult
import bo.saludencasa.features.conversation.domain.repository.IMessageRepository

class GetMessagesUseCase(
    private val repository: IMessageRepository,
) {
    // RNF-08: every read is one bounded page.
    suspend operator fun invoke(
        requestId: String,
        query: MessageQuery,
    ): MessagePageResult =
        when (val result = repository.fetch(requestId, query, PAGE_SIZE)) {
            is MessagesResult.Loaded -> MessagePageResult.Loaded(result.messages, result.messages.size >= PAGE_SIZE)
            is MessagesResult.Failure -> MessagePageResult.Failure(result.error)
        }

    companion object {
        const val PAGE_SIZE = 30
    }
}

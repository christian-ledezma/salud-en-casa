package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.domain.model.MarkReadResult
import bo.saludencasa.features.conversation.domain.repository.IMessageRepository

class MarkConversationReadUseCase(
    private val repository: IMessageRepository,
) {
    suspend operator fun invoke(requestId: String): MarkReadResult = repository.markRead(requestId)
}

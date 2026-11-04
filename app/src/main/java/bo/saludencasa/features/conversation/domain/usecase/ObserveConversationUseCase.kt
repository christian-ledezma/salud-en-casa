package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.domain.repository.IMessageRepository
import kotlinx.coroutines.flow.Flow

class ObserveConversationUseCase(
    private val repository: IMessageRepository,
) {
    operator fun invoke(requestId: String): Flow<Unit> = repository.signals(requestId)
}

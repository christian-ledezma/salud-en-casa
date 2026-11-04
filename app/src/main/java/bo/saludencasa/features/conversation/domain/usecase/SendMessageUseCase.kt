package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.conversation.domain.model.SendMessageResult
import bo.saludencasa.features.conversation.domain.repository.IMessageRepository
import bo.saludencasa.features.conversation.domain.vo.MessageContent

class SendMessageUseCase(
    private val repository: IMessageRepository,
) {
    suspend operator fun invoke(
        requestId: String,
        messageId: String,
        text: String,
    ): SendMessageResult {
        val content =
            MessageContent.create(text).getOrElse { failure ->
                val error = if (failure.message == "message_empty") MessageError.Empty else MessageError.TooLong
                return SendMessageResult.Failure(error)
            }
        return repository.send(requestId, messageId, content)
    }
}

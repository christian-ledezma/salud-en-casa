package bo.saludencasa.features.conversation.domain.model

import bo.saludencasa.features.services.domain.model.ServiceStatus

sealed interface MessageError {
    data object Empty : MessageError

    data object TooLong : MessageError

    // The attention was cancelled: the engine refuses new messages.
    data object ConversationClosed : MessageError

    data object NotSignedIn : MessageError

    data object NetworkUnavailable : MessageError

    data object Unexpected : MessageError
}

data class ConversationHeader(
    val counterpartName: String,
    val counterpartPhotoUrl: String?,
    val serviceTypeName: String,
    val status: ServiceStatus,
    val canWrite: Boolean,
)

sealed interface ConversationHeaderResult {
    data class Loaded(
        val header: ConversationHeader,
    ) : ConversationHeaderResult

    data object NotFound : ConversationHeaderResult

    data class Failure(
        val error: MessageError,
    ) : ConversationHeaderResult
}

// Messages in ascending order, oldest first, whatever the query.
sealed interface MessagesResult {
    data class Loaded(
        val messages: List<Message>,
    ) : MessagesResult

    data class Failure(
        val error: MessageError,
    ) : MessagesResult
}

sealed interface MessagePageResult {
    // full: the page came back at its size, so there may be more beyond it.
    data class Loaded(
        val messages: List<Message>,
        val full: Boolean,
    ) : MessagePageResult

    data class Failure(
        val error: MessageError,
    ) : MessagePageResult
}

sealed interface SendMessageResult {
    data class Sent(
        val message: Message,
    ) : SendMessageResult

    data class Failure(
        val error: MessageError,
    ) : SendMessageResult
}

sealed interface MarkReadResult {
    data object Marked : MarkReadResult

    data class Failure(
        val error: MessageError,
    ) : MarkReadResult
}

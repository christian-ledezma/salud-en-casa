package bo.saludencasa.features.conversation.data.mapper

import bo.saludencasa.features.conversation.data.model.MessageDto
import bo.saludencasa.features.conversation.domain.model.Message
import bo.saludencasa.features.conversation.domain.model.MessageError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime

internal fun MessageDto.toMessage(signedInUserId: String): Message? {
    val created = parseInstant(createdAt) ?: return null
    val read = readAt?.let { parseInstant(it) ?: return null }
    return Message(
        id = id,
        content = content,
        sentByMe = senderId == signedInUserId,
        createdAt = created,
        readAt = read,
    )
}

// The insert policy refuses a cancelled attention with the platform's row-level
// security error; it is the only refusal a party of the service can meet.
internal fun Throwable.toMessageError(): MessageError {
    if (message?.contains(ROW_LEVEL_SECURITY) == true) return MessageError.ConversationClosed
    return when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> MessageError.NetworkUnavailable
        else -> MessageError.Unexpected
    }
}

// A retry of a message the engine already stored meets its own primary key.
internal fun Throwable.isAlreadyStored(): Boolean = message?.contains(MESSAGE_PRIMARY_KEY) == true

// PostgREST writes the offset as +00:00, which Instant.parse does not take.
private fun parseInstant(text: String): Instant? = runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()

private const val ROW_LEVEL_SECURITY = "row-level security"
private const val MESSAGE_PRIMARY_KEY = "messages_pkey"

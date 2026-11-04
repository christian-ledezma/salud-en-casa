package bo.saludencasa.features.conversation.domain.vo

@JvmInline
value class MessageContent private constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 4000

        fun create(raw: String): Result<MessageContent> {
            val trimmed = raw.trim()
            // The engine checks char_length, which counts code points: an emoji is
            // one, while String.length counts it as two UTF-16 units.
            val length = trimmed.codePointCount(0, trimmed.length)
            return when {
                trimmed.isEmpty() -> failure("message_empty")
                length > MAX_LENGTH -> failure("message_too_long")
                else -> Result.success(MessageContent(trimmed))
            }
        }

        private fun failure(key: String): Result<MessageContent> = Result.failure(IllegalArgumentException(key))
    }
}

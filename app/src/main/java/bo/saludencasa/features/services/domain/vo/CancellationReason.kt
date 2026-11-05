package bo.saludencasa.features.services.domain.vo

@JvmInline
value class CancellationReason private constructor(
    val value: String,
) {
    companion object {
        // Same bound as services_cancellation_reason_valid.
        const val MAX_LENGTH = 300

        // The engine checks char_length, which counts code points: an emoji is one,
        // while String.length counts it as two UTF-16 units.
        fun lengthOf(raw: String): Int {
            val trimmed = raw.trim()
            return trimmed.codePointCount(0, trimmed.length)
        }

        fun create(raw: String): Result<CancellationReason> =
            when {
                raw.isBlank() -> failure("cancellation_reason_empty")
                lengthOf(raw) > MAX_LENGTH -> failure("cancellation_reason_too_long")
                else -> Result.success(CancellationReason(raw.trim()))
            }

        private fun failure(key: String): Result<CancellationReason> = Result.failure(IllegalArgumentException(key))
    }
}

package bo.saludencasa.core.vo

@JvmInline
value class Email private constructor(
    val value: String,
) {
    companion object {
        // RFC 5321 caps the whole address at 254 characters. The shape check is
        // deliberately loose: the address that matters is the one the identity
        // provider already verified, and a stricter pattern only rejects valid
        // addresses nobody expected.
        private const val MAX_LENGTH = 254
        private val FORMAT = Regex("""^[^\s@]+@[^\s@.]+(\.[^\s@.]+)+$""")

        fun create(raw: String): Result<Email> {
            val normalized = raw.trim().lowercase()
            return when {
                normalized.isEmpty() -> failure("email_required")
                normalized.length > MAX_LENGTH -> failure("email_too_long")
                !FORMAT.matches(normalized) -> failure("email_invalid_format")
                else -> Result.success(Email(normalized))
            }
        }

        private fun failure(key: String): Result<Email> = Result.failure(IllegalArgumentException(key))
    }
}

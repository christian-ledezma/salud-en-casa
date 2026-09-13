package bo.saludencasa.core.vo

@JvmInline
value class Email private constructor(
    val value: String,
) {
    companion object {
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

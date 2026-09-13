package bo.saludencasa.core.vo

@JvmInline
value class PersonName private constructor(
    val value: String,
) {
    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 80
        private val WHITESPACE = Regex("""\s+""")

        fun create(raw: String): Result<PersonName> {
            val normalized = raw.trim().replace(WHITESPACE, " ")
            return when {
                normalized.length < MIN_LENGTH -> failure("person_name_too_short")
                normalized.length > MAX_LENGTH -> failure("person_name_too_long")
                normalized.any(Char::isDigit) -> failure("person_name_has_digits")
                else -> Result.success(PersonName(normalized))
            }
        }

        private fun failure(key: String): Result<PersonName> = Result.failure(IllegalArgumentException(key))
    }
}

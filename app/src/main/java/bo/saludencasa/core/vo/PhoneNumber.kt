package bo.saludencasa.core.vo

@JvmInline
value class PhoneNumber private constructor(
    val value: String,
) {
    companion object {
        // Bolivia is the only country the product operates in, so the number is
        // stored already qualified with its country code: a bare eight digit
        // number is ambiguous the day a second country appears, and rewriting
        // stored rows then is worse than qualifying them now.
        private const val COUNTRY_CODE = "591"
        private const val NATIONAL_LENGTH = 8
        private val SEPARATORS = Regex("""[\s\-().]""")
        private val DIGITS = Regex("""^\d+$""")

        fun create(raw: String): Result<PhoneNumber> {
            val compact = raw.trim().removePrefix("+").replace(SEPARATORS, "")
            if (compact.isEmpty()) return failure("phone_required")
            if (!DIGITS.matches(compact)) return failure("phone_invalid_characters")

            // The prefix is only stripped when what remains is a whole national
            // number, so a local number that happens to begin with those digits
            // is not mutilated into an invalid one.
            val national =
                compact
                    .takeIf { it.length == COUNTRY_CODE.length + NATIONAL_LENGTH && it.startsWith(COUNTRY_CODE) }
                    ?.drop(COUNTRY_CODE.length)
                    ?: compact
            return when {
                national.length != NATIONAL_LENGTH -> failure("phone_invalid_length")
                else -> Result.success(PhoneNumber("+$COUNTRY_CODE$national"))
            }
        }

        private fun failure(key: String): Result<PhoneNumber> = Result.failure(IllegalArgumentException(key))
    }
}

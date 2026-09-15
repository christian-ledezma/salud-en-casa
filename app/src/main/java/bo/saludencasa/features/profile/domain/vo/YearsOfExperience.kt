package bo.saludencasa.features.profile.domain.vo

@JvmInline
value class YearsOfExperience private constructor(
    val value: Int,
) {
    companion object {
        private const val MIN_VALUE = 0
        private const val MAX_VALUE = 70

        fun create(raw: Int): Result<YearsOfExperience> =
            when {
                raw < MIN_VALUE || raw > MAX_VALUE -> failure("years_of_experience_out_of_range")
                else -> Result.success(YearsOfExperience(raw))
            }

        fun parse(raw: String): Result<YearsOfExperience> {
            val parsed = raw.trim().toIntOrNull() ?: return failure("years_of_experience_invalid_format")
            return create(parsed)
        }

        private fun failure(key: String): Result<YearsOfExperience> = Result.failure(IllegalArgumentException(key))
    }
}

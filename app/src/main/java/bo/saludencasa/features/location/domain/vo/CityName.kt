package bo.saludencasa.features.location.domain.vo

@JvmInline
value class CityName private constructor(
    val value: String,
) {
    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 80

        fun create(raw: String): Result<CityName> {
            val trimmed = raw.trim()
            return when (trimmed.length) {
                in MIN_LENGTH..MAX_LENGTH -> Result.success(CityName(trimmed))
                else -> Result.failure(IllegalArgumentException("city_name_out_of_range"))
            }
        }
    }
}

package bo.saludencasa.features.location.domain.vo

@JvmInline
value class AddressAlias private constructor(
    val value: String,
) {
    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 60

        fun create(raw: String): Result<AddressAlias> {
            val trimmed = raw.trim()
            return when (trimmed.length) {
                in MIN_LENGTH..MAX_LENGTH -> Result.success(AddressAlias(trimmed))
                else -> Result.failure(IllegalArgumentException("address_alias_out_of_range"))
            }
        }
    }
}

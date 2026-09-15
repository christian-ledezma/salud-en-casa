package bo.saludencasa.features.location.domain.vo

@JvmInline
value class AddressReference private constructor(
    val value: String,
) {
    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 300

        fun create(raw: String): Result<AddressReference> {
            val trimmed = raw.trim()
            return when (trimmed.length) {
                in MIN_LENGTH..MAX_LENGTH -> Result.success(AddressReference(trimmed))
                else -> Result.failure(IllegalArgumentException("address_reference_out_of_range"))
            }
        }
    }
}

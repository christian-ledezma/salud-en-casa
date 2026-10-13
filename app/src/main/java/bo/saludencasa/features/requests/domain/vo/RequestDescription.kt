package bo.saludencasa.features.requests.domain.vo

@JvmInline
value class RequestDescription private constructor(
    val value: String,
) {
    companion object {
        // The creation function raises above this same bound, so a description
        // that passed here never meets it there.
        private const val MAX_LENGTH = 1000

        fun create(raw: String): Result<RequestDescription> {
            val trimmed = raw.trim()
            return when {
                trimmed.isEmpty() -> failure("request_description_blank")
                trimmed.length > MAX_LENGTH -> failure("request_description_too_long")
                else -> Result.success(RequestDescription(trimmed))
            }
        }

        private fun failure(key: String): Result<RequestDescription> = Result.failure(IllegalArgumentException(key))
    }
}

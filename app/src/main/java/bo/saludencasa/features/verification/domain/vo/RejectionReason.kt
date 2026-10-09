package bo.saludencasa.features.verification.domain.vo

@JvmInline
value class RejectionReason private constructor(
    val value: String,
) {
    companion object {
        private const val MIN_LENGTH = 1

        // Same bound as verification_documents_rejection_reason_length.
        private const val MAX_LENGTH = 300

        fun create(raw: String): Result<RejectionReason> {
            val trimmed = raw.trim()
            return when (trimmed.length) {
                in MIN_LENGTH..MAX_LENGTH -> Result.success(RejectionReason(trimmed))
                else -> Result.failure(IllegalArgumentException("rejection_reason_out_of_range"))
            }
        }
    }
}

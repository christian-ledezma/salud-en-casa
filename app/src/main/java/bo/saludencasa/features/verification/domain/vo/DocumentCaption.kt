package bo.saludencasa.features.verification.domain.vo

@JvmInline
value class DocumentCaption private constructor(
    val value: String,
) {
    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 120

        fun create(raw: String): Result<DocumentCaption> {
            val trimmed = raw.trim()
            return when (trimmed.length) {
                in MIN_LENGTH..MAX_LENGTH -> Result.success(DocumentCaption(trimmed))
                else -> Result.failure(IllegalArgumentException("document_caption_out_of_range"))
            }
        }
    }
}

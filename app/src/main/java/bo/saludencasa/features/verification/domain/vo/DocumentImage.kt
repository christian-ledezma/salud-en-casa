package bo.saludencasa.features.verification.domain.vo

@JvmInline
value class DocumentImage private constructor(
    val bytes: ByteArray,
) {
    companion object {
        // Matches the Storage bucket's file_size_limit so the client refuses
        // what the server would refuse anyway, before an upload round trip.
        const val MAX_BYTES: Int = 2 * 1024 * 1024

        fun create(bytes: ByteArray): Result<DocumentImage> =
            when {
                bytes.isEmpty() -> Result.failure(IllegalArgumentException("document_image_empty"))
                bytes.size > MAX_BYTES -> Result.failure(IllegalArgumentException("document_image_too_large"))
                else -> Result.success(DocumentImage(bytes))
            }
    }
}

package bo.saludencasa.features.reviews.domain.vo

@JvmInline
value class ReviewComment private constructor(
    val value: String,
) {
    companion object {
        // Same bound as the check on reviews.comment.
        const val MAX_LENGTH = 1000

        // The engine checks char_length, which counts code points: an emoji is one,
        // while String.length counts it as two UTF-16 units.
        fun lengthOf(raw: String): Int {
            val trimmed = raw.trim()
            return trimmed.codePointCount(0, trimmed.length)
        }

        fun create(raw: String): Result<ReviewComment> =
            when {
                raw.isBlank() -> failure("review_comment_empty")
                lengthOf(raw) > MAX_LENGTH -> failure("review_comment_too_long")
                else -> Result.success(ReviewComment(raw.trim()))
            }

        private fun failure(key: String): Result<ReviewComment> = Result.failure(IllegalArgumentException(key))
    }
}

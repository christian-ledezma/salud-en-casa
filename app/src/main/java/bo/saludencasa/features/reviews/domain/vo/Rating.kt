package bo.saludencasa.features.reviews.domain.vo

@JvmInline
value class Rating private constructor(
    val value: Int,
) {
    companion object {
        const val MIN = 1
        const val MAX = 5

        fun create(raw: Int): Result<Rating> =
            if (raw in MIN..MAX) {
                Result.success(Rating(raw))
            } else {
                Result.failure(IllegalArgumentException("rating_out_of_range"))
            }
    }
}

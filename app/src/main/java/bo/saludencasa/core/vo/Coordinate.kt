package bo.saludencasa.core.vo

@ConsistentCopyVisibility
data class Coordinate private constructor(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0

        fun create(
            latitude: Double,
            longitude: Double,
        ): Result<Coordinate> =
            when {
                // Every comparison below answers false for a not-a-number, so
                // without this branch such a pair would pass the range check and
                // reach the geography column as a point PostGIS cannot parse.
                latitude.isNaN() || longitude.isNaN() -> failure("coordinate_not_a_number")

                latitude.isInfinite() || longitude.isInfinite() -> failure("coordinate_not_finite")

                latitude < MIN_LATITUDE || latitude > MAX_LATITUDE -> failure("latitude_out_of_range")

                longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE -> failure("longitude_out_of_range")

                else -> Result.success(Coordinate(latitude, longitude))
            }

        private fun failure(key: String): Result<Coordinate> = Result.failure(IllegalArgumentException(key))
    }
}

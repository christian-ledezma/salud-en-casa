package bo.saludencasa.features.profile.domain.vo

import java.math.BigDecimal

@JvmInline
value class CoverageRadiusKm private constructor(
    val value: BigDecimal,
) {
    companion object {
        private const val MAX_DECIMALS = 2
        private val MIN_VALUE = BigDecimal.ONE
        private val MAX_VALUE = BigDecimal(50)

        fun create(raw: BigDecimal): Result<CoverageRadiusKm> =
            when {
                raw < MIN_VALUE || raw > MAX_VALUE -> failure("coverage_radius_out_of_range")
                raw.scale() > MAX_DECIMALS -> failure("coverage_radius_max_two_decimals")
                else -> Result.success(CoverageRadiusKm(raw))
            }

        fun parse(raw: String): Result<CoverageRadiusKm> {
            val normalized = raw.trim().replace(',', '.')
            val parsed =
                runCatching { BigDecimal(normalized) }.getOrNull()
                    ?: return failure("coverage_radius_invalid_format")
            return create(parsed)
        }

        private fun failure(key: String): Result<CoverageRadiusKm> = Result.failure(IllegalArgumentException(key))
    }
}

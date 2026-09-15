package bo.saludencasa.core.vo

import java.math.BigDecimal

@JvmInline
value class AmountBob private constructor(
    val value: BigDecimal,
) {
    companion object {
        private const val MAX_DECIMALS = 2

        // numeric(10, 2) in every monetary column of the schema. Beyond it the
        // server refuses the write with an overflow the person cannot act on.
        private val MAX_VALUE = BigDecimal("99999999.99")

        fun create(raw: BigDecimal): Result<AmountBob> =
            when {
                raw <= BigDecimal.ZERO -> failure("amount_must_be_positive")
                raw.scale() > MAX_DECIMALS -> failure("amount_max_two_decimals")
                raw > MAX_VALUE -> failure("amount_too_large")
                else -> Result.success(AmountBob(raw))
            }

        // The decimal keyboard shows the separator of the device locale, which
        // in Spanish is the comma, so a rate typed as 80,50 has to reach the
        // same value as 80.50. Anything that leaves more than one separator
        // behind fails to parse and is refused.
        fun parse(raw: String): Result<AmountBob> {
            val normalized = raw.trim().replace(',', '.')
            val parsed = runCatching { BigDecimal(normalized) }.getOrNull() ?: return failure("amount_invalid_format")
            return create(parsed)
        }

        private fun failure(key: String): Result<AmountBob> = Result.failure(IllegalArgumentException(key))
    }
}

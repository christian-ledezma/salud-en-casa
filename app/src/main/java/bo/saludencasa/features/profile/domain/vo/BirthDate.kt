package bo.saludencasa.features.profile.domain.vo

import java.time.LocalDate

@JvmInline
value class BirthDate private constructor(
    val value: LocalDate,
) {
    companion object {
        private const val MAX_AGE_YEARS = 120L

        fun create(
            raw: LocalDate,
            today: LocalDate = LocalDate.now(),
        ): Result<BirthDate> =
            when {
                !raw.isBefore(today) -> failure("birth_date_not_in_the_past")
                raw.isBefore(today.minusYears(MAX_AGE_YEARS)) -> failure("birth_date_too_far_in_the_past")
                else -> Result.success(BirthDate(raw))
            }

        private fun failure(key: String): Result<BirthDate> = Result.failure(IllegalArgumentException(key))
    }
}

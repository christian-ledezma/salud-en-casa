package bo.saludencasa.features.reviews.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.core.util.Age
import bo.saludencasa.core.util.AgeUnit

@Composable
internal fun relativeAgeText(age: Age): String =
    when (age.unit) {
        // Below an hour the exact count says nothing a reader wants: a comment
        // from eleven minutes ago and one from two are the same comment.
        AgeUnit.MINUTES -> stringResource(R.string.relative_time_just_now)

        AgeUnit.HOURS -> pluralStringResource(R.plurals.relative_time_hours, age.count, age.count)

        AgeUnit.DAYS -> pluralStringResource(R.plurals.relative_time_days, age.count, age.count)

        AgeUnit.MONTHS -> pluralStringResource(R.plurals.relative_time_months, age.count, age.count)

        AgeUnit.YEARS -> pluralStringResource(R.plurals.relative_time_years, age.count, age.count)
    }

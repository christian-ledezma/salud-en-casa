package bo.saludencasa.features.catalog.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R

private const val MINUTES_IN_AN_HOUR = 60

// The short form for a list row: «45 min», «1 h», «1 h 30 min». The long
// sentence the cards used to carry does not fit beside a price.
@Composable
internal fun durationLabel(minutes: Int): String =
    when {
        minutes >= MINUTES_IN_AN_HOUR && minutes % MINUTES_IN_AN_HOUR == 0 -> {
            stringResource(R.string.catalog_duration_hours, minutes / MINUTES_IN_AN_HOUR)
        }

        minutes > MINUTES_IN_AN_HOUR -> {
            stringResource(
                R.string.catalog_duration_hours_minutes,
                minutes / MINUTES_IN_AN_HOUR,
                minutes % MINUTES_IN_AN_HOUR,
            )
        }

        else -> {
            stringResource(R.string.catalog_duration_minutes, minutes)
        }
    }

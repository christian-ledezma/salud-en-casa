package bo.saludencasa.features.availability.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.core.util.formatTime
import bo.saludencasa.features.availability.domain.model.AvailabilityError
import java.time.DayOfWeek
import java.time.LocalTime

// The day name comes from the resources and not from the platform calendar:
// the application is in Spanish whatever the language of the phone, and
// DayOfWeek.getDisplayName follows the phone (docs/decisions.md, 2026-10-15,
// la franja se declara en la pantalla).
@StringRes
internal fun DayOfWeek.labelRes(): Int =
    when (this) {
        DayOfWeek.MONDAY -> R.string.availability_day_monday
        DayOfWeek.TUESDAY -> R.string.availability_day_tuesday
        DayOfWeek.WEDNESDAY -> R.string.availability_day_wednesday
        DayOfWeek.THURSDAY -> R.string.availability_day_thursday
        DayOfWeek.FRIDAY -> R.string.availability_day_friday
        DayOfWeek.SATURDAY -> R.string.availability_day_saturday
        DayOfWeek.SUNDAY -> R.string.availability_day_sunday
    }

@Composable
internal fun formattedTime(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0]
    return formatTime(time, locale)
}

@StringRes
internal fun AvailabilityError.messageRes(): Int =
    when (this) {
        AvailabilityError.EndNotAfterStart -> R.string.availability_error_end_not_after_start
        AvailabilityError.OverlapsExistingSlot -> R.string.availability_error_overlap
        AvailabilityError.NetworkUnavailable -> R.string.error_network_unavailable
        AvailabilityError.Unexpected -> R.string.error_unexpected
    }

@Composable
internal fun stringForError(error: AvailabilityError): String = stringResource(error.messageRes())

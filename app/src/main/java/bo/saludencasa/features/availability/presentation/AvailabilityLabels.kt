package bo.saludencasa.features.availability.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.features.availability.domain.model.AvailabilityError
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

internal fun DayOfWeek.longName(locale: java.util.Locale): String =
    getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.uppercase(locale) }

internal fun DayOfWeek.shortName(locale: java.util.Locale): String =
    getDisplayName(TextStyle.SHORT_STANDALONE, locale).replaceFirstChar { it.uppercase(locale) }

@Composable
internal fun formattedTime(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0]
    return time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}

internal fun AvailabilityError.messageRes(): Int =
    when (this) {
        AvailabilityError.EndNotAfterStart -> R.string.availability_error_end_not_after_start
        AvailabilityError.OverlapsExistingSlot -> R.string.availability_error_overlap
        AvailabilityError.NetworkUnavailable -> R.string.error_network_unavailable
        AvailabilityError.Unexpected -> R.string.error_unexpected
    }

@Composable
internal fun stringForError(error: AvailabilityError): String = stringResource(error.messageRes())

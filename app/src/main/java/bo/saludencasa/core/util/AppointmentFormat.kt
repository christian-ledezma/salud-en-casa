package bo.saludencasa.core.util

import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// docs/decisions.md, 2026-10-15, HU-16, the zone is always America/La_Paz.
val AppointmentZone: ZoneId = ZoneId.of("America/La_Paz")

// .claude/rules/i18n.md: dates and times come from the platform for the device locale.
fun formatAppointment(
    date: LocalDate,
    time: LocalTime,
    locale: Locale,
): String = date.atTime(time).format(skeleton("EEEdMMMjmm", locale))

fun formatAppointment(
    at: Instant,
    locale: Locale,
): String {
    val local = at.atZone(AppointmentZone)
    return formatAppointment(local.toLocalDate(), local.toLocalTime(), locale)
}

fun formatTime(
    time: LocalTime,
    locale: Locale,
): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

fun formatWeekdayShort(
    date: LocalDate,
    locale: Locale,
): String = date.format(skeleton("EEE", locale))

fun formatMonthShort(
    date: LocalDate,
    locale: Locale,
): String = date.format(skeleton("MMM", locale))

fun formatDayOfMonth(
    date: LocalDate,
    locale: Locale,
): String = date.format(skeleton("d", locale))

fun formatFullDate(
    date: LocalDate,
    locale: Locale,
): String = date.format(skeleton("EEEEdMMMM", locale))

private fun skeleton(
    skeleton: String,
    locale: Locale,
): DateTimeFormatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

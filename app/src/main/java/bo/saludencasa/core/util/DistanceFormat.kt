package bo.saludencasa.core.util

import android.content.res.Resources
import bo.saludencasa.R
import java.text.NumberFormat
import java.util.Locale

// .claude/rules/i18n.md: the unit lives inside the translatable string, so this
// only produces the number. One decimal is as fine as it gets: the engine
// rounds the distance to the hundred metres before it leaves the database
// (docs/decisions.md, 2026-10-09).
fun formatKilometres(
    distanceM: Int,
    locale: Locale,
): String =
    NumberFormat
        .getNumberInstance(locale)
        .apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(distanceM / 1000.0)

// The distance the engine already rounded to the hundred metres, so "cerca"
// stands in for the sub-hundred case instead of "a 0 m" (docs/decisions.md,
// 2026-10-09).
fun distanceText(
    resources: Resources,
    distanceM: Int,
    locale: Locale,
): String =
    when {
        distanceM < METRES_IN_A_HUNDRED -> resources.getString(R.string.common_distance_near)
        distanceM < METRES_IN_A_KILOMETRE -> resources.getString(R.string.common_distance_m, distanceM)
        else -> resources.getString(R.string.common_distance_km, formatKilometres(distanceM, locale))
    }

private const val METRES_IN_A_HUNDRED = 100
private const val METRES_IN_A_KILOMETRE = 1_000

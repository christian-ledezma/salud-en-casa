package bo.saludencasa.core.util

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

package bo.saludencasa.core.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// .claude/rules/i18n.md: an amount is rendered by the platform currency
// formatter for the device locale, never by pasting a symbol in front of a
// number. The currency is fixed because the system operates in bolivianos; the
// locale decides where the symbol goes and which separators are used.
private val BOLIVIANO: Currency = Currency.getInstance("BOB")

// The symbol is the one Bolivia uses, whatever the device locale
// (docs/decisions.md, 2026-10-15, the search and profile screens).
private val BOLIVIA: Locale = Locale.forLanguageTag("es-BO")

fun formatBob(
    amount: BigDecimal,
    locale: Locale,
): String {
    val format = NumberFormat.getCurrencyInstance(locale).apply { currency = BOLIVIANO }
    (format as? DecimalFormat)?.let {
        it.decimalFormatSymbols = it.decimalFormatSymbols.apply { currencySymbol = BOLIVIANO.getSymbol(BOLIVIA) }
    }
    if (amount.stripTrailingZeros().scale() <= 0) {
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 0
    }
    return format.format(amount)
}

package bo.saludencasa.core.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// .claude/rules/i18n.md: an amount is rendered by the platform currency
// formatter for the device locale, never by pasting a symbol in front of a
// number. The currency is fixed because the system operates in bolivianos; the
// locale decides where the symbol goes and which separators are used.
private val BOLIVIANO: Currency = Currency.getInstance("BOB")

fun formatBob(
    amount: BigDecimal,
    locale: Locale,
): String =
    NumberFormat
        .getCurrencyInstance(locale)
        .apply { currency = BOLIVIANO }
        .format(amount)

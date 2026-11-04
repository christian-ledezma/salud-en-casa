package bo.saludencasa.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class MoneyFormatTest {
    // A phone set to English or to Spanish from another country used to show
    // the ISO code, «BOB180.00», where any Bolivian reads «Bs».
    @Test
    fun theSymbolIsTheBolivianOneWhateverTheDeviceLocale() {
        listOf(Locale.US, SPAIN, BOLIVIA).forEach { locale ->
            val text = formatBob(BigDecimal("180.00"), locale)

            assertTrue("$locale gave $text", text.contains("Bs"))
            assertFalse("$locale gave $text", text.contains("BOB"))
        }
    }

    // HU-19: «Bs150» in a locale that writes the symbol against the digits. The space
    // cannot break, so the amount never splits across two lines.
    @Test
    fun theSymbolNeverTouchesTheAmount() {
        listOf(Locale.US, BOLIVIA).forEach { locale ->
            val text = formatBob(BigDecimal("150.00"), locale)

            assertTrue("$locale gave $text", Regex("""Bs\s\d""").containsMatchIn(text.replace('\u00A0', ' ')))
            assertFalse("$locale breaks at a plain space: $text", text.contains(' '))
        }
    }

    @Test
    fun aWholeAmountShowsNoDecimals() {
        val text = formatBob(BigDecimal("180.00"), Locale.US)

        assertTrue(text, text.contains("180"))
        assertFalse(text, text.contains("180.00"))
    }

    // Dropping the decimals is for whole amounts only: a price with cents that
    // lost them would be a different price.
    @Test
    fun anAmountWithCentsKeepsBothDecimals() {
        assertTrue(formatBob(BigDecimal("120.50"), Locale.US).contains("120.50"))
        assertTrue(formatBob(BigDecimal("120.50"), BOLIVIA).contains("120,50"))
    }
}

private val SPAIN: Locale = Locale.forLanguageTag("es-ES")
private val BOLIVIA: Locale = Locale.forLanguageTag("es-BO")

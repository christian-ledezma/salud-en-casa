package bo.saludencasa.core.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class AmountBobTest {
    // base_rate_bob carries check (base_rate_bob > 0). Letting a zero through
    // costs a round trip and comes back as a constraint name, not as something
    // the professional can act on.
    @Test
    fun rejectsAmountEqualToZero() {
        assertNull(AmountBob.create(BigDecimal.ZERO).getOrNull())
    }

    @Test
    fun rejectsNegativeAmount() {
        assertNull(AmountBob.create(BigDecimal("-1.00")).getOrNull())
    }

    // The worst of the four. numeric(10, 2) does not refuse a third decimal, it
    // rounds it: a rate typed as 80.999 is stored as 81.00 and the professional
    // charges a boliviano they never declared.
    @Test
    fun rejectsAmountWithMoreThanTwoDecimals() {
        assertNull(AmountBob.create(BigDecimal("80.999")).getOrNull())
    }

    @Test
    fun acceptsTheSmallestAmountTheColumnCanHold() {
        assertEquals(BigDecimal("0.01"), AmountBob.create(BigDecimal("0.01")).getOrNull()?.value)
    }

    // Beyond numeric(10, 2) the server answers with a numeric field overflow,
    // which reaches the screen as the generic unexpected error.
    @Test
    fun rejectsAnAmountLargerThanTheColumnCanHold() {
        assertNull(AmountBob.create(BigDecimal("100000000.00")).getOrNull())
    }

    // The decimal keyboard on a device set to Spanish offers a comma. Without
    // this, every rate typed the way the keyboard invites would be refused.
    @Test
    fun parsesACommaAsTheDecimalSeparator() {
        assertEquals(BigDecimal("80.50"), AmountBob.parse("80,50").getOrNull()?.value)
    }

    @Test
    fun rejectsTextThatIsNotANumber() {
        assertNull(AmountBob.parse("ochenta").getOrNull())
    }

    @Test
    fun rejectsTextWithMoreThanOneSeparator() {
        assertNull(AmountBob.parse("1.234,50").getOrNull())
    }
}

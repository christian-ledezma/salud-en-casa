package bo.saludencasa.features.profile.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class CoverageRadiusKmTest {
    // The criterion of HU-04 fixes the range at 1 to 50 kilometres, and the
    // column carries the same check since the migration of this story. A radius
    // below one kilometre would make a professional invisible to almost every
    // search without anything explaining why.
    @Test
    fun rejectsCoverageRadiusOutOfRange() {
        assertNull(CoverageRadiusKm.create(BigDecimal("0.99")).getOrNull())
        assertNull(CoverageRadiusKm.create(BigDecimal("50.01")).getOrNull())
    }

    @Test
    fun acceptsBothEndsOfTheRange() {
        assertEquals(BigDecimal.ONE, CoverageRadiusKm.create(BigDecimal.ONE).getOrNull()?.value)
        assertEquals(BigDecimal(50), CoverageRadiusKm.create(BigDecimal(50)).getOrNull()?.value)
    }

    // numeric(5, 2) rounds a third decimal instead of refusing it, so the value
    // stored would differ from the value declared.
    @Test
    fun rejectsARadiusWithMoreThanTwoDecimals() {
        assertNull(CoverageRadiusKm.create(BigDecimal("7.555")).getOrNull())
    }

    @Test
    fun parsesACommaAsTheDecimalSeparator() {
        assertEquals(BigDecimal("7.5"), CoverageRadiusKm.parse("7,5").getOrNull()?.value)
    }

    @Test
    fun rejectsAnEmptyRadius() {
        assertNull(CoverageRadiusKm.parse("").getOrNull())
    }
}

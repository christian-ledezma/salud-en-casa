package bo.saludencasa.features.profile.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YearsOfExperienceTest {
    // years_of_experience carries check (years_of_experience between 0 and 70).
    @Test
    fun rejectsNegativeYears() {
        assertNull(YearsOfExperience.create(-1).getOrNull())
    }

    @Test
    fun rejectsMoreYearsThanTheColumnAllows() {
        assertNull(YearsOfExperience.create(71).getOrNull())
    }

    @Test
    fun acceptsBothEndsOfTheRange() {
        assertEquals(0, YearsOfExperience.create(0).getOrNull()?.value)
        assertEquals(70, YearsOfExperience.create(70).getOrNull()?.value)
    }

    // The column cannot hold null, so an emptied field is not an answer of
    // zero: it is a field the person has to fill in before the save can go.
    @Test
    fun rejectsAnEmptyFieldRatherThanReadingItAsZero() {
        assertNull(YearsOfExperience.parse("").getOrNull())
    }

    @Test
    fun rejectsAFractionOfAYear() {
        assertNull(YearsOfExperience.parse("2.5").getOrNull())
    }
}

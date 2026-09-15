package bo.saludencasa.features.location.domain.vo

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// addresses.city is not null and has no default, and the geocoder returns
// nothing for it on a point it cannot name. An empty city has to stop at the
// field rather than at the insert.
class CityNameTest {
    @Test
    fun acceptsACity() {
        assertTrue(CityName.create("La Paz").isSuccess)
    }

    @Test
    fun rejectsACityOfOnlySpaces() {
        assertNull(CityName.create("  ").getOrNull())
    }

    @Test
    fun rejectsOneCharacterPastItsBound() {
        assertNull(CityName.create("a".repeat(81)).getOrNull())
    }
}

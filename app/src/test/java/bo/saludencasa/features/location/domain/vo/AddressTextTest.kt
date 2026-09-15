package bo.saludencasa.features.location.domain.vo

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// addresses.address_text carries check (char_length between 1 and 300), and the
// text usually arrives from the geocoder rather than from the keyboard, so it
// can be longer than anyone would type.
class AddressTextTest {
    @Test
    fun acceptsTheExactUpperBoundOfTheColumn() {
        assertTrue(AddressText.create("a".repeat(300)).isSuccess)
    }

    @Test
    fun rejectsOneCharacterPastTheColumn() {
        assertNull(AddressText.create("a".repeat(301)).getOrNull())
    }

    @Test
    fun rejectsAnEmptyAddress() {
        assertNull(AddressText.create("").getOrNull())
    }
}

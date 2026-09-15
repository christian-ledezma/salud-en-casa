package bo.saludencasa.features.location.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// addresses.alias carries check (char_length(alias) between 1 and 60). A value
// the column refuses arrives as a server error the person cannot act on, so the
// same bound is enforced where the message can name the field.
class AddressAliasTest {
    @Test
    fun acceptsTheExactUpperBoundOfTheColumn() {
        assertTrue(AddressAlias.create("a".repeat(60)).isSuccess)
    }

    @Test
    fun rejectsOneCharacterPastTheColumn() {
        assertNull(AddressAlias.create("a".repeat(61)).getOrNull())
    }

    @Test
    fun rejectsAnAliasOfOnlySpaces() {
        assertNull(AddressAlias.create("   ").getOrNull())
    }

    // The surrounding spaces would count towards the column bound and would be
    // stored as part of the name.
    @Test
    fun trimsWhatIsStored() {
        assertEquals("Casa", AddressAlias.create("  Casa  ").getOrNull()?.value)
    }
}

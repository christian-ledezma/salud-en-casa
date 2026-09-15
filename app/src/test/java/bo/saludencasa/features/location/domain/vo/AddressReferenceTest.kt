package bo.saludencasa.features.location.domain.vo

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// addresses_reference_length bounds the one free text field of the form at 300
// characters, which the migration of this story added.
class AddressReferenceTest {
    @Test
    fun acceptsTheExactUpperBoundOfTheConstraint() {
        assertTrue(AddressReference.create("a".repeat(300)).isSuccess)
    }

    @Test
    fun rejectsOneCharacterPastTheConstraint() {
        assertNull(AddressReference.create("a".repeat(301)).getOrNull())
    }
}

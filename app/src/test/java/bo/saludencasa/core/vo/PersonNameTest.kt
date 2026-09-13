package bo.saludencasa.core.vo

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonNameTest {
    @Test
    fun `collapses the inner whitespace the provider sends`() {
        assertEquals("Ana Maria Quispe", PersonName.create("  Ana   Maria\tQuispe ").getOrThrow().value)
    }

    @Test
    fun `keeps the accents and the apostrophes a name may carry`() {
        assertEquals("Nicolás D'Ambrosio", PersonName.create("Nicolás D'Ambrosio").getOrThrow().value)
    }

    @Test
    fun `rejects an empty name`() {
        assertKeyOfFailure("person_name_too_short", PersonName.create("   "))
    }

    @Test
    fun `accepts a name of exactly the minimum length`() {
        assertEquals("Li", PersonName.create("Li").getOrThrow().value)
    }

    @Test
    fun `rejects a name one character under the minimum length`() {
        assertKeyOfFailure("person_name_too_short", PersonName.create("L"))
    }

    @Test
    fun `accepts a name of exactly the maximum length`() {
        assertEquals(
            80,
            PersonName
                .create("a".repeat(80))
                .getOrThrow()
                .value.length,
        )
    }

    @Test
    fun `rejects a name one character over the maximum length`() {
        assertKeyOfFailure("person_name_too_long", PersonName.create("a".repeat(81)))
    }

    // A name arriving with digits is how an account that holds a handle rather
    // than a person reaches the profile, and the profile is what the other party
    // reads before opening their door.
    @Test
    fun `rejects a name that carries digits`() {
        assertKeyOfFailure("person_name_has_digits", PersonName.create("Ana Quispe 2"))
    }
}

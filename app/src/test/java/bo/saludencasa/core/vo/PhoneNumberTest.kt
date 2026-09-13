package bo.saludencasa.core.vo

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberTest {
    @Test
    fun `qualifies a bare national number with the country code`() {
        assertEquals("+59171234567", PhoneNumber.create("71234567").getOrThrow().value)
    }

    @Test
    fun `removes the separators a person types`() {
        assertEquals("+59171234567", PhoneNumber.create(" 7123-4567 ").getOrThrow().value)
    }

    @Test
    fun `accepts a number already written with its country code`() {
        assertEquals("+59171234567", PhoneNumber.create("+591 7123 4567").getOrThrow().value)
    }

    // 59112345 is eight digits and therefore a whole national number, so the
    // leading digits must not be mistaken for the country code and stripped.
    @Test
    fun `does not mistake the leading digits of a national number for the country code`() {
        assertEquals("+59159112345", PhoneNumber.create("59112345").getOrThrow().value)
    }

    @Test
    fun `rejects an empty number`() {
        assertKeyOfFailure("phone_required", PhoneNumber.create("  "))
    }

    @Test
    fun `rejects a number with letters`() {
        assertKeyOfFailure("phone_invalid_characters", PhoneNumber.create("7123456a"))
    }

    @Test
    fun `rejects a number one digit under the national length`() {
        assertKeyOfFailure("phone_invalid_length", PhoneNumber.create("7123456"))
    }

    @Test
    fun `rejects a number one digit over the national length`() {
        assertKeyOfFailure("phone_invalid_length", PhoneNumber.create("712345678"))
    }
}

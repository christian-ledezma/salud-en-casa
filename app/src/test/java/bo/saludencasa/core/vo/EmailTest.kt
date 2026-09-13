package bo.saludencasa.core.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailTest {
    @Test
    fun `normalizes email to lowercase and trimmed`() {
        assertEquals("ana.quispe@example.com", Email.create("  Ana.Quispe@Example.COM  ").getOrThrow().value)
    }

    @Test
    fun `rejects an empty address`() {
        assertKeyOfFailure("email_required", Email.create("   "))
    }

    @Test
    fun `rejects an address without an at sign`() {
        assertKeyOfFailure("email_invalid_format", Email.create("ana.quispe.example.com"))
    }

    @Test
    fun `rejects an address without a domain suffix`() {
        assertKeyOfFailure("email_invalid_format", Email.create("ana@example"))
    }

    @Test
    fun `rejects an address with inner whitespace`() {
        assertKeyOfFailure("email_invalid_format", Email.create("ana quispe@example.com"))
    }

    // 254 is the limit RFC 5321 sets for the whole address, so it is the exact
    // boundary the two tests below sit on either side of.
    @Test
    fun `accepts an address of exactly the maximum length`() {
        val address = "a".repeat(254 - "@example.com".length) + "@example.com"
        assertEquals(
            254,
            Email
                .create(address)
                .getOrThrow()
                .value.length,
        )
    }

    @Test
    fun `rejects an address one character over the maximum length`() {
        val address = "a".repeat(255 - "@example.com".length) + "@example.com"
        assertKeyOfFailure("email_too_long", Email.create(address))
    }
}

internal fun assertKeyOfFailure(
    expectedKey: String,
    result: Result<*>,
) {
    val exception = result.exceptionOrNull()
    assertTrue("Expected a failure carrying the key $expectedKey, got $result", exception != null)
    assertEquals(expectedKey, exception?.message)
}

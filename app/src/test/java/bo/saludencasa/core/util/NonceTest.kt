package bo.saludencasa.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.SecureRandom

class NonceTest {
    // Vector is the published SHA-256 of "abc". Catches sending Google the raw
    // nonce instead of its hash, or Supabase the hash instead of the raw value.
    @Test
    fun `hashes the nonce with sha256 in lowercase hexadecimal`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Nonce.sha256Hex("abc"),
        )
    }

    @Test
    fun `pairs every raw nonce with its own hash`() {
        val nonce = Nonce.generate()
        assertEquals(Nonce.sha256Hex(nonce.raw), nonce.hashed)
        assertNotEquals(nonce.raw, nonce.hashed)
    }

    @Test
    fun `never generates the same nonce twice`() {
        val random = SecureRandom()
        val generated = List(500) { Nonce.generate(random).raw }
        assertEquals(generated.size, generated.toSet().size)
    }
}

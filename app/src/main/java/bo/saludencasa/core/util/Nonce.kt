package bo.saludencasa.core.util

import java.security.MessageDigest
import java.security.SecureRandom

// Google signs the SHA-256 hash of the nonce into the identity token, while
// Supabase verifies it against the raw value. A type with two named
// properties, instead of one string passed to both, is what stops the two
// from being swapped.
class Nonce private constructor(
    val raw: String,
    val hashed: String,
) {
    companion object {
        private const val RANDOM_BYTES = 32

        fun generate(random: SecureRandom = SecureRandom()): Nonce {
            val bytes = ByteArray(RANDOM_BYTES).also(random::nextBytes)
            val raw = bytes.toHex()
            return Nonce(raw = raw, hashed = sha256Hex(raw))
        }

        fun sha256Hex(value: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .toHex()

        private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }
    }
}

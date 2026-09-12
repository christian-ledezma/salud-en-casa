package bo.saludencasa.core.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The two halves of the nonce used in the Google sign in exchange.
 *
 * Google embeds the SHA-256 hash of the nonce in the identity token it issues,
 * and Supabase verifies that token against the raw value. Handing the same
 * string to both sides produces a token Supabase rejects, and the error it
 * returns does not say why. Keeping the two values in one type, each named for
 * where it goes, is what stops them from being swapped.
 */
class Nonce private constructor(
    /** Sent to Supabase when exchanging the identity token. */
    val raw: String,
    /** Sent to Google when requesting the identity token. */
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

package bo.saludencasa.features.services.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellationReasonTest {
    private fun keyOf(raw: String) = CancellationReason.create(raw).exceptionOrNull()?.message

    // cancellationRequiresReason, on the phone's side: the engine's half is the
    // experiment in plan.md, HU-24.
    @Test
    fun rejectsAnEmptyReason() {
        assertEquals("cancellation_reason_empty", keyOf(""))
    }

    @Test
    fun rejectsAReasonOfOnlySpaces() {
        assertEquals("cancellation_reason_empty", keyOf(" \n\t "))
    }

    @Test
    fun acceptsASingleCharacter() {
        assertEquals("x", CancellationReason.create("x").getOrThrow().value)
    }

    @Test
    fun acceptsExactlyTheMaximum() {
        assertTrue(CancellationReason.create("x".repeat(CancellationReason.MAX_LENGTH)).isSuccess)
    }

    @Test
    fun rejectsOneCharacterOverTheMaximum() {
        assertEquals("cancellation_reason_too_long", keyOf("x".repeat(CancellationReason.MAX_LENGTH + 1)))
    }

    @Test
    fun spacesAroundTheReasonDoNotCountAndAreNotSent() {
        val raw = "  " + "x".repeat(CancellationReason.MAX_LENGTH) + "\n"

        assertEquals("x".repeat(CancellationReason.MAX_LENGTH), CancellationReason.create(raw).getOrThrow().value)
    }

    // The engine checks char_length, which counts code points: counted in UTF-16
    // units, a reason the engine accepts would be refused here.
    @Test
    fun anEmojiCountsAsOneCharacterLikeInTheEngine() {
        val emoji = "\uD83D\uDE00"

        assertTrue(CancellationReason.create(emoji.repeat(CancellationReason.MAX_LENGTH)).isSuccess)
        assertEquals("cancellation_reason_too_long", keyOf(emoji.repeat(CancellationReason.MAX_LENGTH + 1)))
    }
}

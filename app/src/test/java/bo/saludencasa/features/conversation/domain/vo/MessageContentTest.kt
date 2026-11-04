package bo.saludencasa.features.conversation.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageContentTest {
    private fun failureKey(raw: String): String? = MessageContent.create(raw).exceptionOrNull()?.message

    @Test
    fun trimsTheSurroundingWhitespace() {
        assertEquals("Llego en diez minutos", MessageContent.create("  Llego en diez minutos \n").getOrThrow().value)
    }

    @Test
    fun rejectsBlankContent() {
        assertEquals("message_empty", failureKey(""))
        assertEquals("message_empty", failureKey(" \n\t "))
    }

    @Test
    fun acceptsExactlyTheMaximumLength() {
        assertTrue(MessageContent.create("a".repeat(MessageContent.MAX_LENGTH)).isSuccess)
    }

    @Test
    fun rejectsOneCharacterOverTheMaximumLength() {
        assertEquals("message_too_long", failureKey("a".repeat(MessageContent.MAX_LENGTH + 1)))
    }

    // The engine's char_length counts an emoji once; counting UTF-16 units would
    // refuse here a message the engine accepts.
    @Test
    fun countsAnEmojiAsOneCharacterLikeTheEngine() {
        val emojis = "😀".repeat(MessageContent.MAX_LENGTH)

        assertEquals(MessageContent.MAX_LENGTH * 2, emojis.length)
        assertTrue(MessageContent.create(emojis).isSuccess)
    }
}

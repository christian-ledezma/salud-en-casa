package bo.saludencasa.features.conversation.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MessageTest {
    private val at = Instant.parse("2026-11-03T13:00:00Z")

    private fun message(
        id: String,
        createdAt: Instant = at,
        readAt: Instant? = null,
    ) = Message(id, "texto $id", sentByMe = true, createdAt = createdAt, readAt = readAt)

    // This is how "Leído" reaches the sender: the same message, read now.
    @Test
    fun aFetchedRowReplacesTheOneOnScreenWithTheSameId() {
        val merged = listOf(message("a")).mergedWith(listOf(message("a", readAt = at.plusSeconds(5))))

        assertEquals(1, merged.size)
        assertEquals(at.plusSeconds(5), merged.single().readAt)
    }

    @Test
    fun overlappingPagesNeverRepeatAMessage() {
        val first = listOf(message("a", at), message("b", at.plusSeconds(1)))
        val second = listOf(message("b", at.plusSeconds(1)), message("c", at.plusSeconds(2)))

        assertEquals(listOf("a", "b", "c"), first.mergedWith(second).map(Message::id))
    }

    // Messages written in one transaction share the instant; the id orders them
    // the way the engine does, whatever order the pages arrived in.
    @Test
    fun messagesOfTheSameInstantKeepTheEnginesOrder() {
        val ids =
            listOf(
                "0b2e4f00-0000-4000-8000-000000000000",
                "0a2e4f00-0000-4000-8000-000000000000",
                "f02e4f00-0000-4000-8000-000000000000",
            )

        val merged = emptyList<Message>().mergedWith(ids.map { message(it) })

        assertEquals(ids.sorted(), merged.map(Message::id))
    }

    // The anchor of a catch-up that must include the message it starts at.
    @Test
    fun theStartOfAnInstantSitsBeforeEveryMessageOfThatInstant() {
        val start = MessageCursor.startOf(at)

        val lowest = "00000000-0000-4000-8000-000000000001"
        val highest = "ffffffff-ffff-4fff-bfff-ffffffffffff"

        assertTrue(start.id < lowest && start.id < highest)
        assertEquals(at, start.createdAt)
    }
}

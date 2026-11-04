package bo.saludencasa.features.conversation.presentation

import bo.saludencasa.features.conversation.domain.model.Message
import bo.saludencasa.features.conversation.domain.model.MessageError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConversationRowsTest {
    private val today = LocalDate.parse("2026-11-03")

    private fun delivered(
        id: String,
        sentByMe: Boolean,
        at: String,
    ) = ConversationItem.Delivered(Message(id, "texto", sentByMe, Instant.parse(at), readAt = null))

    // "Leído" speaks for the whole conversation, so it hangs under one message:
    // the last I wrote, even when the counterpart answered after it.
    @Test
    fun onlyMyLastMessageCarriesTheReceipt() {
        val rows =
            conversationRows(
                listOf(
                    delivered("a", sentByMe = true, at = "2026-11-03T13:00:00Z"),
                    delivered("b", sentByMe = true, at = "2026-11-03T13:01:00Z"),
                    delivered("c", sentByMe = false, at = "2026-11-03T13:02:00Z"),
                ),
                OlderMessages.EXHAUSTED,
                today,
            )

        val withReceipt = rows.filterIsInstance<ConversationRow.Bubble>().filter { it.showsReceipt }
        assertEquals(listOf("b"), withReceipt.map(ConversationRow::key))
    }

    // A message waiting to be sent is my last one, so it takes the receipt line
    // for its "Enviando…" and the delivered one before it gives it up.
    @Test
    fun aMessageOnItsWayTakesTheReceiptLine() {
        val rows =
            conversationRows(
                listOf(
                    delivered("a", sentByMe = true, at = "2026-11-03T13:00:00Z"),
                    ConversationItem.NotSent("n", "otra", MessageError.NetworkUnavailable),
                ),
                OlderMessages.EXHAUSTED,
                today,
            )

        assertEquals(
            listOf("n"),
            rows.filterIsInstance<ConversationRow.Bubble>().filter { it.showsReceipt }.map { it.key },
        )
    }

    // The list is reversed: newest first, and a day's label right after its
    // oldest message, which puts it above them on screen. The time is La Paz's:
    // 02:00 UTC on the 3rd is still the 2nd there.
    @Test
    fun aDayLabelSitsAboveTheMessagesOfThatDayInLaPaz() {
        val rows =
            conversationRows(
                listOf(
                    delivered("yesterday", sentByMe = false, at = "2026-11-03T02:00:00Z"),
                    delivered("today", sentByMe = true, at = "2026-11-03T13:00:00Z"),
                ),
                OlderMessages.EXHAUSTED,
                today,
            )

        assertEquals(
            listOf("today", "day-2026-11-03", "yesterday", "day-2026-11-02", "start"),
            rows.map(ConversationRow::key),
        )
    }

    // The start of the conversation is only claimed when there is nothing older.
    @Test
    fun theStartIsShownOnlyOnceNothingOlderIsLeft() {
        val items = listOf(delivered("a", sentByMe = true, at = "2026-11-03T13:00:00Z"))

        assertEquals(ConversationRow.Start, conversationRows(items, OlderMessages.EXHAUSTED, today).last())
        assertEquals(
            ConversationRow.Older(OlderMessages.IDLE),
            conversationRows(items, OlderMessages.IDLE, today).last(),
        )
    }

    // No start notice over nothing: the empty state says it instead.
    @Test
    fun anEmptyConversationHasNoRows() {
        assertTrue(conversationRows(emptyList(), OlderMessages.EXHAUSTED, today).isEmpty())
    }
}

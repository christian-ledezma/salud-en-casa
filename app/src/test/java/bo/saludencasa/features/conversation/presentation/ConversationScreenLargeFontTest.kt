package bo.saludencasa.features.conversation.presentation

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.AppointmentZone
import bo.saludencasa.core.util.formatTime
import bo.saludencasa.features.conversation.domain.model.ConversationHeader
import bo.saludencasa.features.conversation.domain.model.Message
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ConversationScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-11-03")
    private val readAt = Instant.parse("2026-11-03T13:16:00Z")

    private val header =
        ConversationHeader(
            counterpartName = "Ana Quispe",
            counterpartPhotoUrl = null,
            serviceTypeName = "Curación de heridas",
            status = ServiceStatus.ASSIGNED,
            canWrite = true,
        )

    private fun message(
        id: String,
        content: String,
        sentByMe: Boolean,
        at: Instant,
    ) = Message(id, content, sentByMe, at, readAt = readAt)

    private fun content(
        messages: List<Message> = emptyList(),
        outbox: List<ConversationItem> = emptyList(),
        header: ConversationHeader = this.header,
    ) = ConversationUiState.Content(
        header = header,
        messages = messages,
        outbox = outbox,
        older = OlderMessages.EXHAUSTED,
        draft = "Llevo suero",
    )

    private fun show(uiState: ConversationUiState) {
        composeRule.setContentAtDoubleFontScale {
            ConversationContent(uiState = uiState, today = today, actions = ConversationActions())
        }
    }

    private fun sendButton() = composeRule.onNodeWithContentDescription(string(R.string.cd_send_message))

    // The composer sits outside the list, so at 200 % it must still fit beside the
    // send button, and the newest message with its "Leído" above it.
    @Test
    fun theComposerAndTheReadReceiptStayOnScreen() {
        show(content(listOf(message("m1", "Tengo gasas y alcohol, pero no tengo suero.", true, readAt))))

        composeRule.assertFitsTheScreenWithoutScrolling(sendButton().assertIsEnabled())
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithContentDescription(string(R.string.cd_message_input, "Ana Quispe")),
        )
        val time = formatTime(readAt.atZone(AppointmentZone).toLocalTime(), Locale.getDefault())
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.conversation_status_read, time)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(composeRule.onNodeWithText("Ana Quispe"))
    }

    // Measured against a short bubble on the same screen (docs/decisions.md,
    // 2026-10-09). Plain "taller" is not enough: clipped to one line, the long
    // text still measured 70 px against 66, since one line varies a few pixels
    // from text to text. A wrapped message is at least a whole line taller.
    @Test
    fun aLongMessageWrapsInsteadOfBeingCut() {
        val long = "Es el edificio azul, tercer piso, departamento 3B. El timbre no funciona: toque la puerta."
        show(
            content(
                listOf(
                    message("m1", long, false, readAt.minusSeconds(60)),
                    message("m2", "Sí", false, readAt),
                ),
            ),
        )

        val longHeight =
            composeRule
                .onNodeWithText(long)
                .fetchSemanticsNode()
                .boundsInRoot.height
        val shortHeight =
            composeRule
                .onNodeWithText("Sí")
                .fetchSemanticsNode()
                .boundsInRoot.height
        assertTrue(
            "The long message is $longHeight tall against $shortHeight: it did not wrap.",
            longHeight >= shortHeight * ONE_MORE_LINE,
        )
    }

    @Test
    fun theEmptyConversationKeepsItsComposer() {
        show(content())

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.conversation_empty_title)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(sendButton())
    }

    // A cancelled attention is read only: the notice takes the composer's place.
    @Test
    fun aCancelledAttentionShowsItsNoticeInsteadOfTheComposer() {
        show(
            content(
                messages = listOf(message("m1", "Disculpe, no voy a poder llegar.", false, readAt)),
                header = header.copy(status = ServiceStatus.CANCELLED, canWrite = false),
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.conversation_closed_notice)),
        )
        sendButton().assertDoesNotExist()
    }

    @Test
    fun aMessageNotSentKeepsItsRetryOnScreen() {
        show(content(outbox = listOf(ConversationItem.NotSent("n1", "Gracias", MessageError.NetworkUnavailable))))

        composeRule.assertFitsTheScreenWithoutScrolling(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    @Test
    fun aFailureKeepsItsRetryOnScreen() {
        show(ConversationUiState.Failed(MessageError.NetworkUnavailable))

        composeRule.assertFitsTheScreenWithoutScrolling(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    private companion object {
        const val ONE_MORE_LINE = 1.5f
    }
}

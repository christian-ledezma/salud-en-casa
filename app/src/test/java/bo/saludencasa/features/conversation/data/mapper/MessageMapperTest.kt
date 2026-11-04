package bo.saludencasa.features.conversation.data.mapper

import bo.saludencasa.features.conversation.data.model.MessageDto
import bo.saludencasa.features.conversation.domain.model.MessageError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageMapperTest {
    private val refused = IllegalStateException("new row violates row-level security policy for table messages")

    @Test
    fun aRefusedMessageMeansTheConversationIsClosed() {
        assertEquals(MessageError.ConversationClosed, refused.toMessageError())
    }

    // docs/decisions.md, 2026-11-03, HU-22: read as closed, it would hide the composer
    // from someone who may still write ordinary messages.
    @Test
    fun aRefusedRecordDoesNotMeanTheConversationIsClosed() {
        assertEquals(MessageError.WorkRecordNotAllowed, refused.toMessageError(isWorkRecord = true))
    }

    @Test
    fun aFailureOtherThanARefusalIsNotBlamedOnTheRecord() {
        assertEquals(MessageError.Unexpected, IllegalStateException("boom").toMessageError(isWorkRecord = true))
    }

    @Test
    fun theMarkComesThroughTheMapper() {
        val dto =
            MessageDto(
                id = "m1",
                senderId = "p1",
                content = "Cambio de apósito",
                createdAt = "2026-11-03T15:40:00+00:00",
                isWorkRecord = true,
            )

        assertTrue(dto.toMessage(signedInUserId = "p2")!!.isWorkRecord)
    }
}

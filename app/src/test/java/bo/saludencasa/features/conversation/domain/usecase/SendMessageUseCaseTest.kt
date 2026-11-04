package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.FakeMessageRepository
import bo.saludencasa.features.conversation.REQUEST_ID
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.conversation.domain.model.SendMessageResult
import bo.saludencasa.features.conversation.domain.vo.MessageContent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendMessageUseCaseTest {
    private val repository = FakeMessageRepository()
    private val send = SendMessageUseCase(repository)

    @Test
    fun blankTextNeverReachesTheEngine() =
        runTest {
            assertEquals(SendMessageResult.Failure(MessageError.Empty), send(REQUEST_ID, "id-1", "   "))
            assertTrue(repository.sent.isEmpty())
        }

    @Test
    fun textOverTheLimitNeverReachesTheEngine() =
        runTest {
            val tooLong = "a".repeat(MessageContent.MAX_LENGTH + 1)

            assertEquals(SendMessageResult.Failure(MessageError.TooLong), send(REQUEST_ID, "id-1", tooLong))
            assertTrue(repository.sent.isEmpty())
        }

    @Test
    fun theEngineReceivesTheTrimmedText() =
        runTest {
            send(REQUEST_ID, "id-1", "  Llego en diez minutos  ")

            assertEquals(listOf("id-1" to "Llego en diez minutos"), repository.sent)
        }
}

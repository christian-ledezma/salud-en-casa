package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.REQUEST_ID
import bo.saludencasa.features.conversation.domain.model.ConversationHeaderResult
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetConversationHeaderUseCaseTest {
    private suspend fun canWrite(status: ServiceStatus): Boolean {
        val services = FakeServiceRepository().apply { result = AttentionResult.Loaded(attention(status = status)) }
        val header = GetConversationHeaderUseCase(GetAttentionUseCase(services))(REQUEST_ID)
        return (header as ConversationHeaderResult.Loaded).header.canWrite
    }

    // docs/decisions.md, 2026-11-03, HU-21: a cancelled attention is read only.
    @Test
    fun aCancelledAttentionIsReadOnly() =
        runTest {
            assertEquals(false, canWrite(ServiceStatus.CANCELLED))
        }

    // The payment is confirmed after completion, and it may need coordinating.
    @Test
    fun aCompletedAttentionStaysOpen() =
        runTest {
            assertEquals(true, canWrite(ServiceStatus.COMPLETED))
        }
}

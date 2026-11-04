package bo.saludencasa.features.conversation.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.conversation.FakeMessageRepository
import bo.saludencasa.features.conversation.REQUEST_ID
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.conversation.domain.usecase.CatchUpMessagesUseCase
import bo.saludencasa.features.conversation.domain.usecase.GetConversationHeaderUseCase
import bo.saludencasa.features.conversation.domain.usecase.GetMessagesUseCase
import bo.saludencasa.features.conversation.domain.usecase.MarkConversationReadUseCase
import bo.saludencasa.features.conversation.domain.usecase.ObserveConversationUseCase
import bo.saludencasa.features.conversation.domain.usecase.SendMessageUseCase
import bo.saludencasa.features.conversation.domain.vo.MessageContent
import bo.saludencasa.features.notifications.FakeNotificationRepository
import bo.saludencasa.features.notifications.domain.usecase.EnterConversationUseCase
import bo.saludencasa.features.notifications.domain.usecase.LeaveConversationUseCase
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConversationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messages = FakeMessageRepository()
    private val notifications = FakeNotificationRepository()
    private val services =
        FakeServiceRepository().apply { result = AttentionResult.Loaded(attention(requestId = REQUEST_ID)) }

    private fun TestScope.viewModel(): ConversationViewModel {
        val getMessages = GetMessagesUseCase(messages)
        val viewModel =
            ConversationViewModel(
                requestId = REQUEST_ID,
                getHeader = GetConversationHeaderUseCase(GetAttentionUseCase(services)),
                getMessages = getMessages,
                catchUpMessages = CatchUpMessagesUseCase(getMessages),
                sendMessage = SendMessageUseCase(messages),
                markConversationRead = MarkConversationReadUseCase(messages),
                observeConversation = ObserveConversationUseCase(messages),
                enterConversation = EnterConversationUseCase(notifications),
                leaveConversation = LeaveConversationUseCase(notifications),
            )
        advanceUntilIdle()
        return viewModel
    }

    private fun ConversationViewModel.content(): ConversationUiState.Content =
        uiState.value as ConversationUiState.Content

    private fun ConversationViewModel.texts(): List<String> =
        content().items.map {
            when (it) {
                is ConversationItem.Delivered -> it.message.content
                is ConversationItem.Sending -> it.text
                is ConversationItem.NotSent -> it.text
            }
        }

    private suspend fun TestScope.signal() {
        messages.signals.emit(Unit)
        advanceUntilIdle()
    }

    // messageIsDeliveredToTheOtherPartyInRealTime.
    @Test
    fun theCounterpartsMessageAppearsWithoutReloading() =
        runTest {
            val viewModel = viewModel()

            messages.receive("Estoy saliendo")
            signal()

            assertEquals(listOf("Estoy saliendo"), viewModel.texts())
        }

    // readReceiptIsVisibleToTheSender.
    @Test
    fun myMessageShowsAsReadOnceTheCounterpartReadsIt() =
        runTest {
            val viewModel = viewModel()
            viewModel.onDraftChange("Tengo gasas")
            viewModel.send()
            advanceUntilIdle()

            messages.counterpartReadsEverything()
            signal()

            assertNotNull(
                viewModel
                    .content()
                    .messages
                    .single()
                    .readAt,
            )
        }

    // Listening starts before the first read: a message written while the first
    // page is on its way is caught up after it, not lost until the next one.
    @Test
    fun aMessageWrittenWhileTheFirstPageLoadsIsNotLost() =
        runTest {
            val hold = CompletableDeferred<Unit>()
            messages.holdFetch = hold
            val viewModel = viewModel()

            messages.holdFetch = null
            messages.receive("Llego en diez")
            signal()
            hold.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf("Llego en diez"), viewModel.texts())
        }

    // The signal of my own insert can arrive before the reply to it; either way
    // the message is on screen once, never as sent and still sending.
    @Test
    fun aSentMessageShowsOnceWhenItsSignalArrivesBeforeTheReply() =
        runTest {
            val hold = CompletableDeferred<Unit>()
            messages.holdSend = hold
            val viewModel = viewModel()

            viewModel.onDraftChange("Hola")
            viewModel.send()
            advanceUntilIdle()
            signal()
            hold.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf("Hola"), viewModel.texts())
            assertTrue(viewModel.content().outbox.isEmpty())
        }

    // A retry after a lost reply reuses the id, so the engine cannot store the
    // message twice.
    @Test
    fun aFailedSendKeepsItsTextAndTheRetryReusesItsId() =
        runTest {
            messages.sendError = MessageError.NetworkUnavailable
            val viewModel = viewModel()
            viewModel.onDraftChange("Hola")
            viewModel.send()
            advanceUntilIdle()

            val notSent = viewModel.content().outbox.single() as ConversationItem.NotSent
            assertEquals("Hola", notSent.text)

            messages.sendError = null
            viewModel.retrySend(notSent.key)
            advanceUntilIdle()

            assertEquals(listOf(notSent.key, notSent.key), messages.sent.map { it.first })
            assertEquals(listOf("Hola"), viewModel.texts())
        }

    // The attention was cancelled while the screen was open: the engine refused,
    // and the composer gives way to the read-only notice.
    @Test
    fun aRefusedSendTurnsTheConversationReadOnly() =
        runTest {
            messages.sendError = MessageError.ConversationClosed
            val viewModel = viewModel()
            viewModel.onDraftChange("Hola")
            viewModel.send()
            advanceUntilIdle()

            assertFalse(viewModel.content().header.canWrite)
            viewModel.onDraftChange("Otra vez")
            assertFalse(viewModel.content().canSend)
        }

    @Test
    fun aMessageOverTheLimitGoesBackToTheField() =
        runTest {
            val viewModel = viewModel()
            val tooLong = "a".repeat(MessageContent.MAX_LENGTH + 1)
            viewModel.onDraftChange(tooLong)
            viewModel.send()
            advanceUntilIdle()

            assertEquals(tooLong, viewModel.content().draft)
            assertEquals(MessageError.TooLong, viewModel.content().draftError)
            assertTrue(viewModel.content().items.isEmpty())
            assertTrue(messages.sent.isEmpty())
        }

    // Read means seen: nothing is marked while the screen is in the background.
    @Test
    fun receivedMessagesAreMarkedReadOnlyWhileTheScreenIsInFront() =
        runTest {
            val viewModel = viewModel()
            messages.receive("Hola")
            signal()
            assertEquals(0, messages.markReadCalls)

            viewModel.onResumed()
            advanceUntilIdle()
            assertEquals(1, messages.markReadCalls)

            viewModel.onPaused()
            messages.receive("¿Sigue ahí?")
            signal()
            assertEquals(1, messages.markReadCalls)
        }

    // HU-21, criterion 5: the push is silenced only while the screen is in front.
    @Test
    fun theConversationCountsAsOpenOnlyWhileInFront() =
        runTest {
            val viewModel = viewModel()

            viewModel.onResumed()
            assertEquals(REQUEST_ID, notifications.openConversation())

            viewModel.onPaused()
            assertNull(notifications.openConversation())
        }

    @Test
    fun olderPagesStopAtTheStartOfTheConversation() =
        runTest {
            val total = GetMessagesUseCase.PAGE_SIZE + 5
            repeat(total) { messages.receive("m$it") }
            val viewModel = viewModel()
            assertEquals(OlderMessages.IDLE, viewModel.content().older)

            viewModel.loadOlder()
            advanceUntilIdle()

            assertEquals(OlderMessages.EXHAUSTED, viewModel.content().older)
            assertEquals((0 until total).map { "m$it" }, viewModel.texts())
        }
}

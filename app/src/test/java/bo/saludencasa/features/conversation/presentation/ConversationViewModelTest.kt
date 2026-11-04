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
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.ServiceStatus
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

    private fun attentionUnderWay(status: ServiceStatus = ServiceStatus.IN_PROGRESS) {
        services.result =
            AttentionResult.Loaded(attention(requestId = REQUEST_ID, status = status, iAmThePatient = false))
    }

    private fun TestScope.viewModel(): ConversationViewModel {
        val getMessages = GetMessagesUseCase(messages)
        val viewModel =
            ConversationViewModel(
                requestId = REQUEST_ID,
                getHeader =
                    GetConversationHeaderUseCase(
                        GetAttentionUseCase(services),
                        GetProfileUseCase(FakeProfileRepository()),
                    ),
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

    // docs/decisions.md, 2026-11-03, HU-22.
    @Test
    fun aRecordIsSentAsARecordAndTheBarGoesBackToMessages() =
        runTest {
            attentionUnderWay()
            val viewModel = viewModel()

            viewModel.onRecordingChange(true)
            viewModel.onDraftChange("Cambio de apósito")
            viewModel.send()
            advanceUntilIdle()
            assertTrue(
                viewModel
                    .content()
                    .messages
                    .single()
                    .isWorkRecord,
            )
            assertFalse(viewModel.content().recording)

            viewModel.onDraftChange("Hasta mañana")
            viewModel.send()
            advanceUntilIdle()

            assertEquals(listOf(true, false), messages.sentAsRecord)
        }

    // The row must not turn into a bubble the moment the engine answers, nor start
    // as one while it is on its way.
    @Test
    fun aRecordOnItsWayAlreadyShowsAsARecord() =
        runTest {
            attentionUnderWay()
            val hold = CompletableDeferred<Unit>()
            messages.holdSend = hold
            val viewModel = viewModel()

            viewModel.onRecordingChange(true)
            viewModel.onDraftChange("Cambio de apósito")
            viewModel.send()
            advanceUntilIdle()

            assertTrue(
                viewModel
                    .content()
                    .items
                    .single()
                    .isWorkRecord,
            )
            hold.complete(Unit)
        }

    // The same id with the mark lost would be a record stored as a common message.
    @Test
    fun aRetriedRecordIsStillARecord() =
        runTest {
            attentionUnderWay()
            messages.sendError = MessageError.NetworkUnavailable
            val viewModel = viewModel()
            viewModel.onRecordingChange(true)
            viewModel.onDraftChange("Cambio de apósito")
            viewModel.send()
            advanceUntilIdle()

            val notSent = viewModel.content().outbox.single() as ConversationItem.NotSent
            assertTrue(notSent.isWorkRecord)

            messages.sendError = null
            viewModel.retrySend(notSent.key)
            advanceUntilIdle()

            assertEquals(listOf(true, true), messages.sentAsRecord)
            assertTrue(
                viewModel
                    .content()
                    .messages
                    .single()
                    .isWorkRecord,
            )
        }

    @Test
    fun aRecordOverTheLimitGoesBackToTheFieldStillARecord() =
        runTest {
            attentionUnderWay()
            val viewModel = viewModel()
            val tooLong = "a".repeat(MessageContent.MAX_LENGTH + 1)
            viewModel.onRecordingChange(true)
            viewModel.onDraftChange(tooLong)
            viewModel.send()
            advanceUntilIdle()

            assertEquals(tooLong, viewModel.content().draft)
            assertTrue(viewModel.content().recording)
        }

    // docs/decisions.md, 2026-11-03, HU-22: ordinary messages may still be allowed.
    @Test
    fun aRefusedRecordDoesNotCloseTheConversation() =
        runTest {
            attentionUnderWay()
            messages.sendError = MessageError.WorkRecordNotAllowed
            val viewModel = viewModel()
            viewModel.onRecordingChange(true)
            viewModel.onDraftChange("Cambio de apósito")
            viewModel.send()
            advanceUntilIdle()

            assertTrue(viewModel.content().header.canWrite)
            val notSent = viewModel.content().outbox.single() as ConversationItem.NotSent
            assertEquals(MessageError.WorkRecordNotAllowed, notSent.error)
        }

    // The record was refused because the attention was cancelled meanwhile: asking
    // again is what closes the conversation, and what stops offering the record.
    @Test
    fun aRefusedRecordAsksAgainWhatTheAttentionAllows() =
        runTest {
            attentionUnderWay()
            messages.sendError = MessageError.WorkRecordNotAllowed
            val viewModel = viewModel()
            viewModel.onRecordingChange(true)
            viewModel.onDraftChange("Cambio de apósito")

            attentionUnderWay(ServiceStatus.CANCELLED)
            viewModel.send()
            advanceUntilIdle()

            assertFalse(viewModel.content().header.canWrite)
            assertFalse(viewModel.content().header.canRecord)
        }

    @Test
    fun aPatientCannotTurnOnTheRecord() =
        runTest {
            services.result =
                AttentionResult.Loaded(
                    attention(requestId = REQUEST_ID, status = ServiceStatus.IN_PROGRESS, iAmThePatient = true),
                )
            val viewModel = viewModel()

            viewModel.onRecordingChange(true)
            viewModel.onDraftChange("Muchas gracias")
            viewModel.send()
            advanceUntilIdle()

            assertEquals(listOf(false), messages.sentAsRecord)
        }

    // A resume brings a header that no longer allows it: what was asked for stops
    // counting, so the next send is not a record the engine would refuse.
    @Test
    fun theRecordModeEndsWhenTheAttentionNoLongerAllowsIt() =
        runTest {
            attentionUnderWay()
            val viewModel = viewModel()
            viewModel.onRecordingChange(true)
            assertTrue(viewModel.content().recording)

            attentionUnderWay(ServiceStatus.CANCELLED)
            viewModel.onResumed()
            advanceUntilIdle()

            assertFalse(viewModel.content().recording)
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

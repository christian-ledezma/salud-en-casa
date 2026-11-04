package bo.saludencasa.features.conversation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.conversation.domain.model.ConversationHeader
import bo.saludencasa.features.conversation.domain.model.ConversationHeaderResult
import bo.saludencasa.features.conversation.domain.model.MarkReadResult
import bo.saludencasa.features.conversation.domain.model.Message
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.conversation.domain.model.MessagePageResult
import bo.saludencasa.features.conversation.domain.model.MessageQuery
import bo.saludencasa.features.conversation.domain.model.MessagesResult
import bo.saludencasa.features.conversation.domain.model.SendMessageResult
import bo.saludencasa.features.conversation.domain.model.cursor
import bo.saludencasa.features.conversation.domain.model.mergedWith
import bo.saludencasa.features.conversation.domain.usecase.CatchUpMessagesUseCase
import bo.saludencasa.features.conversation.domain.usecase.GetConversationHeaderUseCase
import bo.saludencasa.features.conversation.domain.usecase.GetMessagesUseCase
import bo.saludencasa.features.conversation.domain.usecase.MarkConversationReadUseCase
import bo.saludencasa.features.conversation.domain.usecase.ObserveConversationUseCase
import bo.saludencasa.features.conversation.domain.usecase.SendMessageUseCase
import bo.saludencasa.features.notifications.domain.usecase.EnterConversationUseCase
import bo.saludencasa.features.notifications.domain.usecase.LeaveConversationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class OlderMessages {
    IDLE,
    LOADING,
    EXHAUSTED,
    FAILED,
}

sealed interface ConversationItem {
    val key: String
    val sentByMe: Boolean
    val isWorkRecord: Boolean

    data class Delivered(
        val message: Message,
    ) : ConversationItem {
        override val key: String get() = message.id
        override val sentByMe: Boolean get() = message.sentByMe
        override val isWorkRecord: Boolean get() = message.isWorkRecord
    }

    data class Sending(
        override val key: String,
        val text: String,
        override val isWorkRecord: Boolean = false,
    ) : ConversationItem {
        override val sentByMe: Boolean get() = true
    }

    data class NotSent(
        override val key: String,
        val text: String,
        val error: MessageError,
        override val isWorkRecord: Boolean = false,
    ) : ConversationItem {
        override val sentByMe: Boolean get() = true
    }
}

sealed interface ConversationUiState {
    data object Loading : ConversationUiState

    data object NotFound : ConversationUiState

    data class Failed(
        val error: MessageError,
    ) : ConversationUiState

    // No messages yet is not a state of its own: the composer is still there.
    data class Content(
        val header: ConversationHeader,
        val messages: List<Message>,
        val outbox: List<ConversationItem> = emptyList(),
        val older: OlderMessages,
        val draft: String = "",
        val draftError: MessageError? = null,
        val recordRequested: Boolean = false,
    ) : ConversationUiState {
        // Oldest first; what is still on its way goes after what the engine has.
        val items: List<ConversationItem>
            get() = messages.map(ConversationItem::Delivered) + outbox

        val canSend: Boolean get() = header.canWrite && draft.isNotBlank()

        val recording: Boolean get() = recordRequested && header.canRecord
    }
}

class ConversationViewModel(
    private val requestId: String,
    private val getHeader: GetConversationHeaderUseCase,
    private val getMessages: GetMessagesUseCase,
    private val catchUpMessages: CatchUpMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
    private val markConversationRead: MarkConversationReadUseCase,
    private val observeConversation: ObserveConversationUseCase,
    private val enterConversation: EnterConversationUseCase,
    private val leaveConversation: LeaveConversationUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<ConversationUiState>(ConversationUiState.Loading)
    val uiState: StateFlow<ConversationUiState> = state.asStateFlow()

    // Signals coalesce: ten that arrive during one catch-up ask for one more.
    private val catchUps = Channel<Unit>(Channel.CONFLATED)
    private var loadJob: Job? = null
    private var resumed = false
    private val askedToMarkRead = mutableSetOf<String>()

    init {
        // Listening starts before the first read, and the signals wait behind it,
        // so a message written while the first page loads is caught up after it.
        viewModelScope.launch { observeConversation(requestId).collect { catchUps.trySend(Unit) } }
        loadJob = viewModelScope.launch { load() }
        viewModelScope.launch {
            loadJob?.join()
            for (signal in catchUps) catchUp()
        }
    }

    fun retry() {
        if (state.value !is ConversationUiState.Failed) return
        state.value = ConversationUiState.Loading
        loadJob =
            viewModelScope.launch {
                load()
                catchUps.trySend(Unit)
            }
    }

    fun onResumed() {
        resumed = true
        enterConversation(requestId)
        catchUps.trySend(Unit)
        viewModelScope.launch {
            refreshHeader()
            markReadIfNeeded()
        }
    }

    fun onPaused() {
        resumed = false
        leaveConversation(requestId)
    }

    fun onDraftChange(text: String) {
        updateContent { it.copy(draft = text, draftError = null) }
    }

    fun onRecordingChange(enabled: Boolean) {
        updateContent { if (it.header.canRecord) it.copy(recordRequested = enabled) else it }
    }

    fun send() {
        val content = state.value as? ConversationUiState.Content ?: return
        if (!content.canSend) return
        val key = UUID.randomUUID().toString()
        val text = content.draft
        val isWorkRecord = content.recording
        updateContent {
            it.copy(
                draft = "",
                recordRequested = false,
                outbox = it.outbox + ConversationItem.Sending(key, text, isWorkRecord),
            )
        }
        deliver(key, text, isWorkRecord)
    }

    fun retrySend(key: String) {
        val content = state.value as? ConversationUiState.Content ?: return
        val failed = content.outbox.firstOrNull { it.key == key } as? ConversationItem.NotSent ?: return
        updateContent { current ->
            current.copy(
                outbox =
                    current.outbox.map {
                        if (it.key ==
                            key
                        ) {
                            ConversationItem.Sending(key, failed.text, failed.isWorkRecord)
                        } else {
                            it
                        }
                    },
            )
        }
        deliver(key, failed.text, failed.isWorkRecord)
    }

    fun loadOlder() {
        val content = state.value as? ConversationUiState.Content ?: return
        if (content.older != OlderMessages.IDLE && content.older != OlderMessages.FAILED) return
        val oldest = content.messages.firstOrNull()
        if (oldest == null) {
            updateContent { it.copy(older = OlderMessages.EXHAUSTED) }
            return
        }
        updateContent { it.copy(older = OlderMessages.LOADING) }
        viewModelScope.launch {
            when (val page = getMessages(requestId, MessageQuery.OlderThan(oldest.cursor()))) {
                is MessagePageResult.Loaded -> {
                    updateContent {
                        it.withMessages(page.messages).copy(
                            older = if (page.full) OlderMessages.IDLE else OlderMessages.EXHAUSTED,
                        )
                    }
                }

                is MessagePageResult.Failure -> {
                    updateContent { it.copy(older = OlderMessages.FAILED) }
                }
            }
        }
    }

    private suspend fun load() {
        val header =
            when (val result = getHeader(requestId)) {
                is ConversationHeaderResult.Loaded -> {
                    result.header
                }

                ConversationHeaderResult.NotFound -> {
                    state.value = ConversationUiState.NotFound
                    return
                }

                is ConversationHeaderResult.Failure -> {
                    state.value = ConversationUiState.Failed(result.error)
                    return
                }
            }

        when (val page = getMessages(requestId, MessageQuery.Newest)) {
            is MessagePageResult.Loaded -> {
                state.value =
                    ConversationUiState.Content(
                        header = header,
                        messages = page.messages,
                        older = if (page.full) OlderMessages.IDLE else OlderMessages.EXHAUSTED,
                    )
                markReadIfNeeded()
            }

            is MessagePageResult.Failure -> {
                state.value = ConversationUiState.Failed(page.error)
            }
        }
    }

    // A failed catch-up leaves the screen as it was; the next signal tries again.
    private suspend fun catchUp() {
        val content = state.value as? ConversationUiState.Content ?: return
        val result = catchUpMessages(requestId, content.messages)
        if (result is MessagesResult.Loaded) {
            updateContent { it.withMessages(result.messages) }
            markReadIfNeeded()
        }
    }

    private suspend fun refreshHeader() {
        val result = getHeader(requestId)
        if (result is ConversationHeaderResult.Loaded) updateContent { it.copy(header = result.header) }
    }

    private fun deliver(
        key: String,
        text: String,
        isWorkRecord: Boolean,
    ) {
        viewModelScope.launch {
            when (val result = sendMessage(requestId, key, text, isWorkRecord)) {
                is SendMessageResult.Sent -> {
                    updateContent { it.withMessages(listOf(result.message)) }
                }

                is SendMessageResult.Failure -> {
                    onSendFailed(key, text, isWorkRecord, result.error)
                    if (result.error == MessageError.WorkRecordNotAllowed) refreshHeader()
                }
            }
        }
    }

    private fun onSendFailed(
        key: String,
        text: String,
        isWorkRecord: Boolean,
        error: MessageError,
    ) {
        updateContent { current ->
            when (error) {
                // The text never left: it goes back to the field to be shortened.
                MessageError.Empty, MessageError.TooLong -> {
                    current.copy(
                        outbox = current.outbox.filterNot { it.key == key },
                        draft = text,
                        draftError = error,
                        recordRequested = isWorkRecord,
                    )
                }

                else -> {
                    val notSent = ConversationItem.NotSent(key, text, error, isWorkRecord)
                    current.copy(
                        outbox = current.outbox.map { if (it.key == key) notSent else it },
                        header =
                            if (error == MessageError.ConversationClosed) {
                                current.header.copy(canWrite = false)
                            } else {
                                current.header
                            },
                    )
                }
            }
        }
    }

    private suspend fun markReadIfNeeded() {
        if (!resumed) return
        val content = state.value as? ConversationUiState.Content ?: return
        val unread = content.messages.filter { !it.sentByMe && it.readAt == null }.map(Message::id)
        if (unread.isEmpty() || askedToMarkRead.containsAll(unread)) return
        askedToMarkRead += unread
        if (markConversationRead(requestId) is MarkReadResult.Failure) askedToMarkRead -= unread.toSet()
    }

    private fun updateContent(transform: (ConversationUiState.Content) -> ConversationUiState.Content) {
        state.update { current -> if (current is ConversationUiState.Content) transform(current) else current }
    }

    // A message the engine returned leaves the outbox, whichever arrived first:
    // the reply to the insert or the signal that followed it.
    private fun ConversationUiState.Content.withMessages(incoming: List<Message>): ConversationUiState.Content {
        val merged = messages.mergedWith(incoming)
        val delivered = merged.mapTo(HashSet(), Message::id)
        return copy(messages = merged, outbox = outbox.filterNot { it.key in delivered })
    }
}

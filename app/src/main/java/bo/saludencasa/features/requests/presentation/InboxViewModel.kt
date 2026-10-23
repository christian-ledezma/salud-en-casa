package bo.saludencasa.features.requests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.requests.domain.model.InboxPageResult
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.InboxSearchResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.usecase.FindInboxRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetInboxUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveInboxSignalsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface InboxUiState {
    data object Loading : InboxUiState

    data object Empty : InboxUiState

    data class Content(
        val requests: List<InboxRequest>,
        val loadingMore: Boolean,
        val endReached: Boolean,
        val notice: RequestError?,
    ) : InboxUiState

    data class Failed(
        val error: RequestError,
    ) : InboxUiState
}

// The request a tapped notification points at (docs/decisions.md, 2026-10-16).
sealed interface InboxHighlight {
    data object None : InboxHighlight

    data class Row(
        val requestId: String,
    ) : InboxHighlight

    data object Unavailable : InboxHighlight
}

class InboxViewModel(
    highlightRequestId: String?,
    private val getInbox: GetInboxUseCase,
    private val findInboxRequest: FindInboxRequestUseCase,
    observeInboxSignals: ObserveInboxSignalsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val uiState: StateFlow<InboxUiState> = state.asStateFlow()

    private val highlightState = MutableStateFlow<InboxHighlight>(InboxHighlight.None)
    val highlight: StateFlow<InboxHighlight> = highlightState.asStateFlow()

    private var pendingHighlight: String? = highlightRequestId

    private var loadMoreJob: Job? = null

    init {
        load()
        // The broadcast says "the inbox changed"; the screen answers by reading
        // the first page again, so a request published in range appears without
        // the professional reloading (RF-07.4). The collection lives for the view
        // model's lifetime and the data source unsubscribes when it ends.
        viewModelScope.launch {
            observeInboxSignals().collect { refresh() }
        }
    }

    fun load() {
        loadMoreJob?.cancel()
        state.value = InboxUiState.Loading
        viewModelScope.launch {
            state.value = firstPageState(getInbox(offset = 0), onFailure = InboxUiState::Failed)
            locateHighlight()
        }
    }

    private suspend fun locateHighlight() {
        val requestId = pendingHighlight ?: return
        val (loaded, endReached) =
            when (val current = state.value) {
                is InboxUiState.Content -> current.requests to current.endReached
                InboxUiState.Empty -> emptyList<InboxRequest>() to true
                else -> return
            }

        val result = findInboxRequest(requestId, loaded, endReached)

        if (result.requests.isNotEmpty()) {
            state.value =
                InboxUiState.Content(
                    requests = result.requests,
                    loadingMore = false,
                    endReached = result.endReached,
                    notice = null,
                )
        }
        when (result) {
            is InboxSearchResult.Found -> concludeHighlight(InboxHighlight.Row(requestId))
            is InboxSearchResult.NotInInbox -> concludeHighlight(InboxHighlight.Unavailable)
            is InboxSearchResult.Failure -> Unit
        }
    }

    private fun concludeHighlight(highlight: InboxHighlight) {
        pendingHighlight = null
        highlightState.value = highlight
    }

    fun onDismissUnavailable() {
        highlightState.value = InboxHighlight.None
    }

    // A signal-driven refresh never flashes the loading spinner or blanks the
    // screen: a dropped background refetch leaves the current list in place, and
    // the next signal or a manual retry recovers it.
    private fun refresh() {
        viewModelScope.launch {
            val result = getInbox(offset = 0)
            if (result is InboxPageResult.Loaded) {
                state.value =
                    if (result.requests.isEmpty()) {
                        InboxUiState.Empty
                    } else {
                        InboxUiState.Content(
                            requests = result.requests,
                            loadingMore = false,
                            endReached = result.endReached,
                            notice = null,
                        )
                    }
            }
        }
    }

    fun onReachedEnd() {
        val content = state.value as? InboxUiState.Content ?: return
        if (content.endReached || content.loadingMore) return

        state.update { content.copy(loadingMore = true, notice = null) }
        loadMoreJob =
            viewModelScope.launch {
                val result = getInbox(offset = content.requests.size)
                val current = state.value as? InboxUiState.Content ?: return@launch
                state.update {
                    when (result) {
                        is InboxPageResult.Loaded -> {
                            current.copy(
                                // A repeated key throws a LazyColumn, and a tie in
                                // created_at can bring a row back on the next offset.
                                requests = (current.requests + result.requests).distinctBy(InboxRequest::id),
                                loadingMore = false,
                                endReached = result.endReached,
                                notice = null,
                            )
                        }

                        is InboxPageResult.Failure -> {
                            current.copy(loadingMore = false, notice = result.error)
                        }
                    }
                }
            }
    }

    fun onRetry() {
        load()
    }

    private inline fun firstPageState(
        result: InboxPageResult,
        onFailure: (RequestError) -> InboxUiState,
    ): InboxUiState =
        when (result) {
            is InboxPageResult.Loaded -> {
                if (result.requests.isEmpty()) {
                    InboxUiState.Empty
                } else {
                    InboxUiState.Content(
                        requests = result.requests,
                        loadingMore = false,
                        endReached = result.endReached,
                        notice = null,
                    )
                }
            }

            is InboxPageResult.Failure -> {
                onFailure(result.error)
            }
        }
}

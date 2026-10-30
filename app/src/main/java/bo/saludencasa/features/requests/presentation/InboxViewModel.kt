package bo.saludencasa.features.requests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.requests.domain.model.InboxPageResult
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestError
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

class InboxViewModel(
    private val getInbox: GetInboxUseCase,
    observeInboxSignals: ObserveInboxSignalsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val uiState: StateFlow<InboxUiState> = state.asStateFlow()

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
        }
    }

    // A signal-driven refresh never flashes the loading spinner or blanks the
    // screen: a dropped background refetch leaves the current list in place, and
    // the next signal or a manual retry recovers it. The screen also calls it on
    // every resume, now that the inbox is a tab of the home screen.
    fun refresh() {
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

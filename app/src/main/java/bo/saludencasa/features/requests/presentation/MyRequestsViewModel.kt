package bo.saludencasa.features.requests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.model.MyRequestsPageResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import bo.saludencasa.features.requests.domain.usecase.CancelRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetMyRequestsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MyRequestsUiState {
    data object Loading : MyRequestsUiState

    data object Empty : MyRequestsUiState

    data class Content(
        val requests: List<ServiceRequest>,
        val loadingMore: Boolean,
        val endReached: Boolean,
        val notice: RequestError?,
        val cancellingId: String?,
    ) : MyRequestsUiState

    data class Failed(
        val error: RequestError,
    ) : MyRequestsUiState
}

class MyRequestsViewModel(
    private val getMyRequests: GetMyRequestsUseCase,
    private val cancelRequest: CancelRequestUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<MyRequestsUiState>(MyRequestsUiState.Loading)
    val uiState: StateFlow<MyRequestsUiState> = state.asStateFlow()

    private var loadMoreJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadMoreJob?.cancel()
        state.value = MyRequestsUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getMyRequests(offset = 0)) {
                    is MyRequestsPageResult.Loaded -> {
                        if (result.requests.isEmpty()) {
                            MyRequestsUiState.Empty
                        } else {
                            MyRequestsUiState.Content(
                                requests = result.requests,
                                loadingMore = false,
                                endReached = result.endReached,
                                notice = null,
                                cancellingId = null,
                            )
                        }
                    }

                    is MyRequestsPageResult.Failure -> {
                        MyRequestsUiState.Failed(result.error)
                    }
                }
        }
    }

    fun onReachedEnd() {
        val content = state.value as? MyRequestsUiState.Content ?: return
        if (content.endReached || content.loadingMore) return

        state.update { content.copy(loadingMore = true, notice = null) }
        loadMoreJob =
            viewModelScope.launch {
                val result = getMyRequests(offset = content.requests.size)
                val current = state.value as? MyRequestsUiState.Content ?: return@launch
                state.update {
                    when (result) {
                        is MyRequestsPageResult.Loaded -> {
                            current.copy(
                                // A repeated key throws a LazyColumn, and a tie
                                // in created_at can bring a row back on the next
                                // offset.
                                requests = (current.requests + result.requests).distinctBy(ServiceRequest::id),
                                loadingMore = false,
                                endReached = result.endReached,
                                notice = null,
                            )
                        }

                        is MyRequestsPageResult.Failure -> {
                            current.copy(loadingMore = false, notice = result.error)
                        }
                    }
                }
            }
    }

    fun onCancel(requestId: String) {
        val content = state.value as? MyRequestsUiState.Content ?: return
        if (content.cancellingId != null) return

        state.update { content.copy(cancellingId = requestId, notice = null) }
        viewModelScope.launch {
            when (val result = cancelRequest(requestId)) {
                // The list is read again rather than patched: the sweep or the
                // other party may have moved more than the one row since it was
                // read, and reloading is what keeps the screen honest.
                CancelRequestResult.Success -> {
                    load()
                }

                is CancelRequestResult.Failure -> {
                    val current = state.value as? MyRequestsUiState.Content ?: return@launch
                    state.update { current.copy(cancellingId = null, notice = result.error) }
                }
            }
        }
    }

    fun onRetry() {
        load()
    }

    // On every resume, now that the list is a tab of the home screen and lives as
    // long as it does. Like the inbox, a refresh never blanks the screen: a read
    // that fails leaves the list in place, and so does one that lands mid-cancel.
    fun refresh() {
        val current = state.value
        if (current is MyRequestsUiState.Loading) return
        if (current is MyRequestsUiState.Content && current.cancellingId != null) return

        viewModelScope.launch {
            val result = getMyRequests(offset = 0)
            if (result !is MyRequestsPageResult.Loaded) return@launch
            if ((state.value as? MyRequestsUiState.Content)?.cancellingId != null) return@launch
            loadMoreJob?.cancel()
            state.value =
                if (result.requests.isEmpty()) {
                    MyRequestsUiState.Empty
                } else {
                    MyRequestsUiState.Content(
                        requests = result.requests,
                        loadingMore = false,
                        endReached = result.endReached,
                        notice = null,
                        cancellingId = null,
                    )
                }
        }
    }
}

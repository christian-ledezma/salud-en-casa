package bo.saludencasa.features.disputes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.model.DisputesPageResult
import bo.saludencasa.features.disputes.domain.model.PaymentDispute
import bo.saludencasa.features.disputes.domain.usecase.GetDisputesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DisputeQueueUiState {
    data object Loading : DisputeQueueUiState

    data object Empty : DisputeQueueUiState

    data class Content(
        val disputes: List<PaymentDispute>,
        val loadingMore: Boolean = false,
        val endReached: Boolean,
        val notice: DisputeError? = null,
    ) : DisputeQueueUiState

    data class Failed(
        val error: DisputeError,
    ) : DisputeQueueUiState
}

class DisputeQueueViewModel(
    private val getDisputes: GetDisputesUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<DisputeQueueUiState>(DisputeQueueUiState.Loading)
    val uiState: StateFlow<DisputeQueueUiState> = state.asStateFlow()

    private var loadMoreJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadMoreJob?.cancel()
        state.value = DisputeQueueUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val page = getDisputes(offset = 0)) {
                    is DisputesPageResult.Loaded -> page.firstPageState()
                    is DisputesPageResult.Failure -> DisputeQueueUiState.Failed(page.error)
                }
        }
    }

    // On every resume, so a dispute just resolved leaves the list. A failed read
    // leaves the list on screen as it was (docs/decisions.md, 2026-10-30, HT-25).
    fun refresh() {
        viewModelScope.launch {
            val page = getDisputes(offset = 0)
            if (page !is DisputesPageResult.Loaded) return@launch
            loadMoreJob?.cancel()
            state.value = page.firstPageState()
        }
    }

    fun onLoadMore() {
        val content = state.value as? DisputeQueueUiState.Content ?: return
        if (content.endReached || content.loadingMore) return

        state.value = content.copy(loadingMore = true, notice = null)
        loadMoreJob =
            viewModelScope.launch {
                val page = getDisputes(offset = content.disputes.size)
                state.update { current ->
                    if (current !is DisputeQueueUiState.Content) return@update current
                    when (page) {
                        is DisputesPageResult.Loaded -> {
                            current.copy(
                                // A dispute resolved meanwhile moves every offset by
                                // one, which brings a row back, and a repeated key
                                // throws a LazyColumn.
                                disputes = (current.disputes + page.disputes).distinctBy(PaymentDispute::paymentId),
                                loadingMore = false,
                                endReached = page.endReached,
                            )
                        }

                        is DisputesPageResult.Failure -> {
                            current.copy(loadingMore = false, notice = page.error)
                        }
                    }
                }
            }
    }

    private fun DisputesPageResult.Loaded.firstPageState(): DisputeQueueUiState =
        if (disputes.isEmpty()) {
            DisputeQueueUiState.Empty
        } else {
            DisputeQueueUiState.Content(disputes = disputes, endReached = endReached)
        }
}

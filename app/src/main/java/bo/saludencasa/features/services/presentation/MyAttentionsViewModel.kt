package bo.saludencasa.features.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.core.util.ListRefresh
import bo.saludencasa.core.util.ListRefresher
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.AttentionSummary
import bo.saludencasa.features.services.domain.model.MyAttentionsPageResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.usecase.GetMyAttentionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MyAttentionsUiState {
    data object Loading : MyAttentionsUiState

    data object Empty : MyAttentionsUiState

    data class Content(
        val attentions: List<AttentionSummary>,
        val loadingMore: Boolean = false,
        val endReached: Boolean,
        val notice: ServiceError? = null,
    ) : MyAttentionsUiState

    data class Failed(
        val error: ServiceError,
    ) : MyAttentionsUiState
}

class MyAttentionsViewModel(
    private val side: AttentionSide,
    private val getMyAttentions: GetMyAttentionsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<MyAttentionsUiState>(MyAttentionsUiState.Loading)
    val uiState: StateFlow<MyAttentionsUiState> = state.asStateFlow()

    private val refresher = ListRefresher(viewModelScope)
    val listRefresh: StateFlow<ListRefresh> = refresher.state

    private var loadMoreJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadMoreJob?.cancel()
        state.value = MyAttentionsUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val page = getMyAttentions(side, offset = 0)) {
                    is MyAttentionsPageResult.Loaded -> page.firstPageState()
                    is MyAttentionsPageResult.Failure -> MyAttentionsUiState.Failed(page.error)
                }
        }
    }

    // On every resume: a failed read leaves the list on screen as it was, the rule
    // the inbox follows (docs/decisions.md, 2026-10-30, HT-25).
    fun refresh() {
        viewModelScope.launch {
            if (readFirstPage()) refresher.clearFailure()
        }
    }

    fun onPullToRefresh() {
        refresher.pull(::readFirstPage)
    }

    private suspend fun readFirstPage(): Boolean {
        val page = getMyAttentions(side, offset = 0)
        if (page !is MyAttentionsPageResult.Loaded) return false
        loadMoreJob?.cancel()
        state.value = page.firstPageState()
        return true
    }

    fun onLoadMore() {
        val content = state.value as? MyAttentionsUiState.Content ?: return
        if (content.endReached || content.loadingMore) return

        state.value = content.copy(loadingMore = true, notice = null)
        loadMoreJob =
            viewModelScope.launch {
                val page = getMyAttentions(side, offset = content.attentions.size)
                state.update { current ->
                    if (current !is MyAttentionsUiState.Content) return@update current
                    when (page) {
                        is MyAttentionsPageResult.Loaded -> {
                            current.copy(
                                // A new attention moves every offset by one, which
                                // brings a row back, and a repeated key throws a LazyColumn.
                                attentions =
                                    (current.attentions + page.attentions).distinctBy(
                                        AttentionSummary::requestId,
                                    ),
                                loadingMore = false,
                                endReached = page.endReached,
                            )
                        }

                        is MyAttentionsPageResult.Failure -> {
                            current.copy(loadingMore = false, notice = page.error)
                        }
                    }
                }
            }
    }

    private fun MyAttentionsPageResult.Loaded.firstPageState(): MyAttentionsUiState =
        if (attentions.isEmpty()) {
            MyAttentionsUiState.Empty
        } else {
            MyAttentionsUiState.Content(attentions = attentions, endReached = endReached)
        }
}

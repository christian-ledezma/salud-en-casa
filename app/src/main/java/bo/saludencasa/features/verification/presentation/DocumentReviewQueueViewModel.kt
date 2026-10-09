package bo.saludencasa.features.verification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.verification.domain.model.PendingReviewPageResult
import bo.saludencasa.features.verification.domain.model.PendingReviewQuery
import bo.saludencasa.features.verification.domain.model.PendingReviewSubject
import bo.saludencasa.features.verification.domain.model.ReviewQueueOrder
import bo.saludencasa.features.verification.domain.model.ReviewRoleFilter
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.GetPendingReviewSubjectsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface QueueListState {
    data object Loading : QueueListState

    data object Empty : QueueListState

    data class Content(
        val subjects: List<PendingReviewSubject>,
        val loadingMore: Boolean,
        val endReached: Boolean,
        val notice: VerificationError?,
    ) : QueueListState

    data class Failed(
        val error: VerificationError,
    ) : QueueListState
}

// The search field and the filters stay on screen whatever the list is doing,
// including when it is empty or failed, so the criteria live beside the list
// state instead of inside each of its cases.
data class DocumentReviewQueueUiState(
    val query: PendingReviewQuery = PendingReviewQuery(),
    val list: QueueListState = QueueListState.Loading,
)

class DocumentReviewQueueViewModel(
    private val getPendingSubjects: GetPendingReviewSubjectsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(DocumentReviewQueueUiState())

    val uiState: StateFlow<DocumentReviewQueueUiState> = state.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    fun refresh() {
        reload(resetList = false)
    }

    // Typing is not a reason to query: a seven-letter name would spend seven
    // requests to show the answer to the last one.
    fun onSearchChanged(value: String) {
        state.update { it.copy(query = it.query.copy(search = value)) }
        reload(resetList = true, after = SEARCH_DEBOUNCE_MILLIS)
    }

    fun onRoleChanged(role: ReviewRoleFilter) {
        state.update { it.copy(query = it.query.copy(role = role)) }
        reload(resetList = true)
    }

    fun onOrderChanged(order: ReviewQueueOrder) {
        state.update { it.copy(query = it.query.copy(order = order)) }
        reload(resetList = true)
    }

    fun onClearFilters() {
        state.update { it.copy(query = PendingReviewQuery(order = it.query.order)) }
        reload(resetList = true)
    }

    fun onReachedEnd() {
        val list = state.value.list as? QueueListState.Content ?: return
        if (list.loadingMore || list.endReached) return

        val query = state.value.query
        state.update { it.copy(list = list.copy(loadingMore = true, notice = null)) }
        loadMoreJob =
            viewModelScope.launch {
                val result = getPendingSubjects(query, offset = list.subjects.size)
                val current = state.value.list as? QueueListState.Content ?: return@launch
                state.update {
                    it.copy(
                        list =
                            when (result) {
                                is PendingReviewPageResult.Loaded -> {
                                    current.copy(
                                        subjects = (current.subjects + result.subjects).distinctBy { s -> s.profileId },
                                        loadingMore = false,
                                        endReached = result.endReached,
                                    )
                                }

                                is PendingReviewPageResult.Failure -> {
                                    current.copy(loadingMore = false, notice = result.error)
                                }
                            },
                    )
                }
            }
    }

    // resetList clears the rows on screen because they answer criteria that no
    // longer hold; a plain refresh keeps them so coming back from the detail
    // does not blank the list. Both cancel whatever is in flight: a reply to the
    // previous criteria landing last would fill the list with rows the chips no
    // longer describe.
    private fun reload(
        resetList: Boolean,
        after: Long = 0,
    ) {
        loadMoreJob?.cancel()
        loadJob?.cancel()
        if (resetList || state.value.list !is QueueListState.Content) {
            state.update { it.copy(list = QueueListState.Loading) }
        }
        loadJob =
            viewModelScope.launch {
                if (after > 0) delay(after)
                val query = state.value.query
                when (val result = getPendingSubjects(query, offset = 0)) {
                    is PendingReviewPageResult.Loaded -> {
                        state.update {
                            it.copy(
                                list =
                                    if (result.subjects.isEmpty()) {
                                        QueueListState.Empty
                                    } else {
                                        QueueListState.Content(
                                            subjects = result.subjects,
                                            loadingMore = false,
                                            endReached = result.endReached,
                                            notice = null,
                                        )
                                    },
                            )
                        }
                    }

                    is PendingReviewPageResult.Failure -> {
                        state.update {
                            val current = it.list
                            it.copy(
                                list =
                                    if (current is QueueListState.Content) {
                                        current.copy(loadingMore = false, notice = result.error)
                                    } else {
                                        QueueListState.Failed(result.error)
                                    },
                            )
                        }
                    }
                }
            }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MILLIS: Long = 300
    }
}

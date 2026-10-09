package bo.saludencasa.features.verification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.verification.domain.model.PendingDocumentReview
import bo.saludencasa.features.verification.domain.model.PendingReviewPageResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.GetPendingDocumentReviewsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DocumentReviewQueueUiState {
    data object Loading : DocumentReviewQueueUiState

    data object Empty : DocumentReviewQueueUiState

    data class Content(
        val reviews: List<PendingDocumentReview>,
        val loadingMore: Boolean,
        val endReached: Boolean,
        val notice: VerificationError?,
    ) : DocumentReviewQueueUiState

    data class Failed(
        val error: VerificationError,
    ) : DocumentReviewQueueUiState
}

internal val PendingDocumentReview.key: String get() = "$profileId/${type.name}"

class DocumentReviewQueueViewModel(
    private val getPendingReviews: GetPendingDocumentReviewsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<DocumentReviewQueueUiState>(DocumentReviewQueueUiState.Loading)

    val uiState: StateFlow<DocumentReviewQueueUiState> = state.asStateFlow()

    private var loadMoreJob: Job? = null

    fun refresh() {
        // docs/decisions.md, 2026-10-06, queue pagination.
        loadMoreJob?.cancel()
        if (state.value !is DocumentReviewQueueUiState.Content) {
            state.value = DocumentReviewQueueUiState.Loading
        }
        viewModelScope.launch {
            when (val result = getPendingReviews(offset = 0)) {
                is PendingReviewPageResult.Loaded -> {
                    state.value =
                        if (result.reviews.isEmpty()) {
                            DocumentReviewQueueUiState.Empty
                        } else {
                            DocumentReviewQueueUiState.Content(
                                reviews = result.reviews,
                                loadingMore = false,
                                endReached = result.endReached,
                                notice = null,
                            )
                        }
                }

                is PendingReviewPageResult.Failure -> {
                    val current = state.value
                    state.value =
                        if (current is DocumentReviewQueueUiState.Content) {
                            current.copy(loadingMore = false, notice = result.error)
                        } else {
                            DocumentReviewQueueUiState.Failed(result.error)
                        }
                }
            }
        }
    }

    fun onReachedEnd() {
        val content = state.value as? DocumentReviewQueueUiState.Content ?: return
        if (content.loadingMore || content.endReached) return

        state.value = content.copy(loadingMore = true, notice = null)
        loadMoreJob =
            viewModelScope.launch {
                val result = getPendingReviews(offset = content.reviews.size)
                val current = state.value as? DocumentReviewQueueUiState.Content ?: return@launch
                state.value =
                    when (result) {
                        is PendingReviewPageResult.Loaded -> {
                            current.copy(
                                reviews = (current.reviews + result.reviews).distinctBy { it.key },
                                loadingMore = false,
                                endReached = result.endReached,
                            )
                        }

                        is PendingReviewPageResult.Failure -> {
                            current.copy(loadingMore = false, notice = result.error)
                        }
                    }
            }
    }
}

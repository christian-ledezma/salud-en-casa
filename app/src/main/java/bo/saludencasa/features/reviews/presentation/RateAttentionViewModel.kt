package bo.saludencasa.features.reviews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewResult
import bo.saludencasa.features.reviews.domain.model.toReviewError
import bo.saludencasa.features.reviews.domain.usecase.SubmitReviewUseCase
import bo.saludencasa.features.reviews.domain.vo.Rating
import bo.saludencasa.features.reviews.domain.vo.ReviewComment
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RateAttentionUiState {
    data object Loading : RateAttentionUiState

    // Not there, not theirs, not completed or already rated: one screen for all of
    // them, because the person can do nothing different in each.
    data class Unavailable(
        val error: ReviewError,
    ) : RateAttentionUiState

    data class Content(
        val attention: Attention,
        val rating: Int,
        val comment: String = "",
        val confirming: Boolean = false,
        val sending: Boolean = false,
        val error: SubmitReviewError? = null,
    ) : RateAttentionUiState {
        val canSubmit: Boolean get() = rating >= Rating.MIN && !sending
    }

    data object Submitted : RateAttentionUiState
}

class RateAttentionViewModel(
    private val requestId: String,
    initialRating: Int,
    private val getAttention: GetAttentionUseCase,
    private val submitReview: SubmitReviewUseCase,
) : ViewModel() {
    // The star tapped on the attention arrives already chosen; anything outside 1..5
    // is no star at all.
    private val startingRating = initialRating.takeIf { it in Rating.MIN..Rating.MAX } ?: 0

    private val state = MutableStateFlow<RateAttentionUiState>(RateAttentionUiState.Loading)
    val uiState: StateFlow<RateAttentionUiState> = state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        state.value = RateAttentionUiState.Loading
        load()
    }

    fun onRatingChanged(rating: Int) {
        updateContent { it.copy(rating = rating, error = null) }
    }

    fun onCommentChanged(comment: String) {
        updateContent { it.copy(comment = comment, error = null) }
    }

    fun onSubmitRequested() {
        updateContent {
            val tooLong = ReviewComment.lengthOf(it.comment) > ReviewComment.MAX_LENGTH
            when {
                !it.canSubmit -> it
                tooLong -> it.copy(error = SubmitReviewError.CommentTooLong)
                else -> it.copy(confirming = true, error = null)
            }
        }
    }

    fun onConfirmDismissed() {
        updateContent { it.copy(confirming = false) }
    }

    fun onConfirmed() {
        val content = state.value as? RateAttentionUiState.Content ?: return
        if (!content.confirming || content.sending) return

        updateContent { it.copy(confirming = false, sending = true, error = null) }
        viewModelScope.launch {
            when (val result = submitReview(requestId, content.rating, content.comment)) {
                SubmitReviewResult.Submitted -> state.value = RateAttentionUiState.Submitted
                is SubmitReviewResult.Failure -> updateContent { it.copy(sending = false, error = result.error) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            state.value =
                when (val result = getAttention(requestId)) {
                    is AttentionResult.Loaded -> {
                        val attention = result.attention
                        when {
                            attention.myReview != null -> {
                                RateAttentionUiState.Unavailable(ReviewError.AlreadyRated)
                            }

                            !attention.canRate -> {
                                RateAttentionUiState.Unavailable(ReviewError.NotAllowed)
                            }

                            else -> {
                                RateAttentionUiState.Content(attention = attention, rating = startingRating)
                            }
                        }
                    }

                    AttentionResult.NotFound -> {
                        RateAttentionUiState.Unavailable(ReviewError.NotAllowed)
                    }

                    is AttentionResult.Failure -> {
                        RateAttentionUiState.Unavailable(result.error.toReviewError())
                    }
                }
        }
    }

    private fun updateContent(transform: (RateAttentionUiState.Content) -> RateAttentionUiState.Content) {
        state.update { if (it is RateAttentionUiState.Content) transform(it) else it }
    }
}

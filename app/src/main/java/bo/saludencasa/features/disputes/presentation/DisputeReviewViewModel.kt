package bo.saludencasa.features.disputes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.model.DisputeResolution
import bo.saludencasa.features.disputes.domain.model.DisputeResult
import bo.saludencasa.features.disputes.domain.model.PaymentDispute
import bo.saludencasa.features.disputes.domain.model.ResolveDisputeResult
import bo.saludencasa.features.disputes.domain.usecase.GetDisputeUseCase
import bo.saludencasa.features.disputes.domain.usecase.ResolveDisputeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DisputeReviewUiState {
    data object Loading : DisputeReviewUiState

    // Resolved already, by this administrator on another screen or by another.
    data object Gone : DisputeReviewUiState

    data class Failed(
        val error: DisputeError,
    ) : DisputeReviewUiState

    data class Content(
        val dispute: PaymentDispute,
        val draft: ResolveDraft? = null,
        val resolved: Boolean = false,
    ) : DisputeReviewUiState
}

// The resolution is chosen by the button pressed; the dialog only asks to
// confirm it, because it cannot be undone.
data class ResolveDraft(
    val resolution: DisputeResolution,
    val error: DisputeError? = null,
    val sending: Boolean = false,
)

class DisputeReviewViewModel(
    private val paymentId: String,
    private val getDispute: GetDisputeUseCase,
    private val resolveDispute: ResolveDisputeUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<DisputeReviewUiState>(DisputeReviewUiState.Loading)
    val uiState: StateFlow<DisputeReviewUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = DisputeReviewUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getDispute(paymentId)) {
                    is DisputeResult.Loaded -> DisputeReviewUiState.Content(result.dispute)
                    DisputeResult.NotFound -> DisputeReviewUiState.Gone
                    is DisputeResult.Failure -> DisputeReviewUiState.Failed(result.error)
                }
        }
    }

    fun onResolveRequested(resolution: DisputeResolution) {
        updateContent { if (it.resolved) it else it.copy(draft = ResolveDraft(resolution)) }
    }

    fun onResolveDismissed() {
        updateContent { if (it.draft?.sending == true) it else it.copy(draft = null) }
    }

    fun onResolveConfirmed() {
        val content = state.value as? DisputeReviewUiState.Content ?: return
        val draft = content.draft ?: return
        if (draft.sending) return

        updateDraft { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            when (val result = resolveDispute(paymentId, draft.resolution)) {
                ResolveDisputeResult.Resolved -> {
                    updateContent { it.copy(draft = null, resolved = true) }
                }

                is ResolveDisputeResult.Failure -> {
                    if (result.error == DisputeError.AlreadyResolved) {
                        state.value = DisputeReviewUiState.Gone
                    } else {
                        updateDraft { it.copy(sending = false, error = result.error) }
                    }
                }
            }
        }
    }

    private fun updateDraft(transform: (ResolveDraft) -> ResolveDraft) {
        updateContent { content -> content.copy(draft = content.draft?.let(transform)) }
    }

    private fun updateContent(transform: (DisputeReviewUiState.Content) -> DisputeReviewUiState.Content) {
        state.update { current -> if (current is DisputeReviewUiState.Content) transform(current) else current }
    }
}

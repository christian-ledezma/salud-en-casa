package bo.saludencasa.features.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.services.domain.model.AdvanceAttentionResult
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.CancelAttentionResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.usecase.AdvanceAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.CancelAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.ObserveAttentionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AttentionUiState {
    data object Loading : AttentionUiState

    data class Content(
        val attention: Attention,
        val confirming: AttentionStep? = null,
        val advancing: Boolean = false,
        val notice: ServiceError? = null,
        val cancelDraft: CancelDraft? = null,
    ) : AttentionUiState

    // The service is not there for this caller: the acceptance did not land, or
    // they are not one of its two parties.
    data object NotFound : AttentionUiState

    data class Failed(
        val error: ServiceError,
    ) : AttentionUiState
}

data class CancelDraft(
    val reason: String = "",
    val error: CancelAttentionError? = null,
    val sending: Boolean = false,
)

class AttentionViewModel(
    private val requestId: String,
    private val getAttention: GetAttentionUseCase,
    private val advanceAttention: AdvanceAttentionUseCase,
    private val observeAttention: ObserveAttentionUseCase,
    private val cancelAttention: CancelAttentionUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<AttentionUiState>(AttentionUiState.Loading)
    val uiState: StateFlow<AttentionUiState> = state.asStateFlow()

    init {
        load()
        viewModelScope.launch { observeAttention(requestId).collect { reread() } }
    }

    fun refresh() = load()

    fun onResumed() {
        viewModelScope.launch { reread() }
    }

    fun onMarkRequested() {
        updateContent { it.copy(confirming = it.attention.nextStepForMe, notice = null) }
    }

    fun onMarkDismissed() {
        updateContent { it.copy(confirming = null) }
    }

    fun onMarkConfirmed() {
        val content = state.value as? AttentionUiState.Content ?: return
        val step = content.confirming ?: return
        if (content.advancing) return

        updateContent { it.copy(confirming = null, advancing = true) }
        viewModelScope.launch {
            val notice =
                when (val result = advanceAttention(requestId, step)) {
                    AdvanceAttentionResult.Advanced -> {
                        reread()
                        null
                    }

                    is AdvanceAttentionResult.Failure -> {
                        // docs/decisions.md, 2026-11-04, HU-23.
                        val reread = result.error == ServiceError.AttentionChanged && reread()
                        result.error.takeUnless { reread }
                    }
                }
            updateContent { it.copy(advancing = false, notice = notice) }
        }
    }

    fun onCancelRequested() {
        updateContent { if (it.attention.canCancel) it.copy(cancelDraft = CancelDraft(), notice = null) else it }
    }

    fun onCancelReasonChanged(reason: String) {
        updateDraft { it.copy(reason = reason, error = null) }
    }

    fun onCancelDismissed() {
        updateContent { if (it.cancelDraft?.sending == true) it else it.copy(cancelDraft = null) }
    }

    fun onCancelConfirmed() {
        val content = state.value as? AttentionUiState.Content ?: return
        val draft = content.cancelDraft ?: return
        if (draft.sending || content.advancing) return

        updateDraft { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            when (val result = cancelAttention(requestId, draft.reason)) {
                CancelAttentionResult.Cancelled -> {
                    updateContent { it.copy(cancelDraft = null) }
                    reread()
                }

                is CancelAttentionResult.Failure -> {
                    // docs/decisions.md, 2026-11-04, HU-23: reread closes the dialog
                    // when the attention stopped being cancellable.
                    if (result.error == CancelAttentionError.Service(ServiceError.AttentionChanged)) reread()
                    updateDraft { it.copy(sending = false, error = result.error) }
                }
            }
        }
    }

    private fun load() {
        state.value = AttentionUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getAttention(requestId)) {
                    is AttentionResult.Loaded -> AttentionUiState.Content(result.attention)
                    AttentionResult.NotFound -> AttentionUiState.NotFound
                    is AttentionResult.Failure -> AttentionUiState.Failed(result.error)
                }
        }
    }

    // docs/decisions.md, 2026-10-30, HT-25: a failed read leaves the screen as it was.
    private suspend fun reread(): Boolean {
        val result = getAttention(requestId) as? AttentionResult.Loaded ?: return false
        state.update { current ->
            if (current is AttentionUiState.Content) {
                // A step that is no longer the next one must not be confirmed: the
                // answer given was to a question the screen stopped asking.
                current.copy(
                    attention = result.attention,
                    confirming = current.confirming?.takeIf { it == result.attention.nextStepForMe },
                    cancelDraft = current.cancelDraft?.takeIf { result.attention.canCancel },
                )
            } else {
                AttentionUiState.Content(result.attention)
            }
        }
        return true
    }

    private fun updateDraft(transform: (CancelDraft) -> CancelDraft) {
        updateContent { content -> content.copy(cancelDraft = content.cancelDraft?.let(transform)) }
    }

    private fun updateContent(transform: (AttentionUiState.Content) -> AttentionUiState.Content) {
        state.update { current -> if (current is AttentionUiState.Content) transform(current) else current }
    }
}

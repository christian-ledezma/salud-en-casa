package bo.saludencasa.features.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AttentionUiState {
    data object Loading : AttentionUiState

    data class Content(
        val attention: Attention,
    ) : AttentionUiState

    // The service is not there for this caller: the acceptance did not land, or
    // they are not one of its two parties.
    data object NotFound : AttentionUiState

    data class Failed(
        val error: ServiceError,
    ) : AttentionUiState
}

class AttentionViewModel(
    private val requestId: String,
    private val getAttention: GetAttentionUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<AttentionUiState>(AttentionUiState.Loading)
    val uiState: StateFlow<AttentionUiState> = state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

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
}

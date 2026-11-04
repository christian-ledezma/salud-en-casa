package bo.saludencasa.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.requests.domain.model.ActiveRequestResult
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import bo.saludencasa.features.requests.domain.usecase.GetActiveImmediateRequestUseCase
import bo.saludencasa.features.services.domain.model.ActiveAttentionResult
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.AttentionSummary
import bo.saludencasa.features.services.domain.usecase.GetActiveAttentionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PatientHomeUiState {
    data object Loading : PatientHomeUiState

    data class Content(
        val activeAttention: AttentionSummary?,
        val activeRequest: ServiceRequest?,
    ) : PatientHomeUiState
}

// docs/decisions.md, 2026-10-30, HT-25, the home screen reads again on every resume.
class PatientHomeViewModel(
    private val getActiveImmediateRequest: GetActiveImmediateRequestUseCase,
    private val getActiveAttention: GetActiveAttentionUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<PatientHomeUiState>(PatientHomeUiState.Loading)
    val uiState: StateFlow<PatientHomeUiState> = state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val attention = getActiveAttention(AttentionSide.PATIENT)
            val request = getActiveImmediateRequest()
            val shown = state.value as? PatientHomeUiState.Content
            // docs/decisions.md, 2026-10-30, HT-25, a shortcut that fails hides.
            state.value =
                PatientHomeUiState.Content(
                    activeAttention = attention.orShown(shown?.activeAttention),
                    activeRequest =
                        when (request) {
                            is ActiveRequestResult.Found -> request.request
                            ActiveRequestResult.None -> null
                            is ActiveRequestResult.Failure -> shown?.activeRequest
                        },
                )
        }
    }
}

internal fun ActiveAttentionResult.orShown(shown: AttentionSummary?): AttentionSummary? =
    when (this) {
        is ActiveAttentionResult.Found -> attention
        ActiveAttentionResult.None -> null
        is ActiveAttentionResult.Failure -> shown
    }

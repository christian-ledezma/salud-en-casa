package bo.saludencasa.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.requests.domain.model.ActiveRequestResult
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import bo.saludencasa.features.requests.domain.usecase.GetActiveImmediateRequestUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PatientHomeUiState {
    data object Loading : PatientHomeUiState

    data object NothingInProgress : PatientHomeUiState

    data class InProgress(
        val request: ServiceRequest,
    ) : PatientHomeUiState
}

// docs/decisions.md, 2026-10-30, HT-25, the home screen reads again on every resume.
class PatientHomeViewModel(
    private val getActiveImmediateRequest: GetActiveImmediateRequestUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<PatientHomeUiState>(PatientHomeUiState.Loading)
    val uiState: StateFlow<PatientHomeUiState> = state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            state.value =
                when (val result = getActiveImmediateRequest()) {
                    is ActiveRequestResult.Found -> {
                        PatientHomeUiState.InProgress(result.request)
                    }

                    ActiveRequestResult.None -> {
                        PatientHomeUiState.NothingInProgress
                    }

                    // docs/decisions.md, 2026-10-30, HT-25, a shortcut that fails hides.
                    is ActiveRequestResult.Failure -> {
                        state.value.takeUnless { it is PatientHomeUiState.Loading }
                            ?: PatientHomeUiState.NothingInProgress
                    }
                }
        }
    }
}

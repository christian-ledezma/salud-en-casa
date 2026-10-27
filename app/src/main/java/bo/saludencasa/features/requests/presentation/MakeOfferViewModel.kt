package bo.saludencasa.features.requests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferableRequest
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.usecase.GetOfferableRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.SubmitOfferUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MakeOfferUiState {
    data object Loading : MakeOfferUiState

    data class Content(
        val request: OfferableRequest,
        val amount: String,
        val message: String,
        val submitting: Boolean,
        val error: OfferError?,
    ) : MakeOfferUiState {
        val canSubmit: Boolean
            get() = !request.alreadyOffered && !submitting
    }

    data object Sent : MakeOfferUiState

    // The request is not visible to this professional: accepted, cancelled, gone.
    data object Gone : MakeOfferUiState

    data class Failed(
        val error: OfferError,
    ) : MakeOfferUiState
}

class MakeOfferViewModel(
    private val requestId: String,
    private val getOfferableRequest: GetOfferableRequestUseCase,
    private val submitOffer: SubmitOfferUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<MakeOfferUiState>(MakeOfferUiState.Loading)
    val uiState: StateFlow<MakeOfferUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = MakeOfferUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getOfferableRequest(requestId)) {
                    is OfferableRequestResult.Loaded -> {
                        MakeOfferUiState.Content(
                            request = result.request,
                            amount = "",
                            message = "",
                            submitting = false,
                            error = null,
                        )
                    }

                    OfferableRequestResult.NotFound -> {
                        MakeOfferUiState.Gone
                    }

                    is OfferableRequestResult.Failure -> {
                        MakeOfferUiState.Failed(result.error)
                    }
                }
        }
    }

    fun onAmountChanged(value: String) {
        val content = editableContent() ?: return
        state.value = content.copy(amount = value, error = null)
    }

    fun onMessageChanged(value: String) {
        val content = editableContent() ?: return
        state.value = content.copy(message = value, error = null)
    }

    fun onSubmit() {
        val content = editableContent() ?: return
        if (!content.canSubmit) return

        state.value = content.copy(submitting = true, error = null)
        viewModelScope.launch {
            when (val result = submitOffer(requestId, content.amount, content.message)) {
                OfferSubmitResult.Success -> {
                    state.value = MakeOfferUiState.Sent
                }

                is OfferSubmitResult.Failure -> {
                    val current = state.value as? MakeOfferUiState.Content ?: return@launch
                    state.value = current.copy(submitting = false, error = result.error)
                }
            }
        }
    }

    private fun editableContent(): MakeOfferUiState.Content? =
        (state.value as? MakeOfferUiState.Content)?.takeUnless { it.submitting }
}

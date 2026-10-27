package bo.saludencasa.features.requests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.RequestOffer
import bo.saludencasa.features.requests.domain.model.RequestOffersResult
import bo.saludencasa.features.requests.domain.usecase.GetRequestOffersUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveMyOfferSignalsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NegotiationUiState {
    data object Loading : NegotiationUiState

    data object Empty : NegotiationUiState

    data class Content(
        val offers: List<RequestOffer>,
    ) : NegotiationUiState

    data class Failed(
        val error: OfferError,
    ) : NegotiationUiState
}

class NegotiationViewModel(
    private val requestId: String,
    private val getRequestOffers: GetRequestOffersUseCase,
    private val observeMyOfferSignals: ObserveMyOfferSignalsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<NegotiationUiState>(NegotiationUiState.Loading)
    val uiState: StateFlow<NegotiationUiState> = state.asStateFlow()

    init {
        load(showLoading = true)
        // Criterion 2: an offer landing re-queries the thread without a reload
        // (docs/decisions.md, 2026-10-23). A signal never shows the spinner again.
        viewModelScope.launch {
            observeMyOfferSignals().collect { load(showLoading = false) }
        }
    }

    fun refresh() = load(showLoading = true)

    private fun load(showLoading: Boolean) {
        if (showLoading) state.value = NegotiationUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getRequestOffers(requestId)) {
                    is RequestOffersResult.Loaded -> {
                        if (result.offers.isEmpty()) {
                            NegotiationUiState.Empty
                        } else {
                            NegotiationUiState.Content(result.offers)
                        }
                    }

                    is RequestOffersResult.Failure -> {
                        NegotiationUiState.Failed(result.error)
                    }
                }
        }
    }
}

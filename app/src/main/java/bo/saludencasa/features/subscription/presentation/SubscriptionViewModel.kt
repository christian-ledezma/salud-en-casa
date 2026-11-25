package bo.saludencasa.features.subscription.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.subscription.domain.model.Subscription
import bo.saludencasa.features.subscription.domain.model.SubscriptionError
import bo.saludencasa.features.subscription.domain.model.SubscriptionResult
import bo.saludencasa.features.subscription.domain.usecase.GetMySubscriptionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SubscriptionUiState {
    data object Loading : SubscriptionUiState

    // The requirement went off, or there is no period, since the row that led here.
    data object NotRequired : SubscriptionUiState

    data class Failed(
        val error: SubscriptionError,
    ) : SubscriptionUiState

    data class Content(
        val subscription: Subscription,
    ) : SubscriptionUiState
}

class SubscriptionViewModel(
    private val getMySubscription: GetMySubscriptionUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val uiState: StateFlow<SubscriptionUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = SubscriptionUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getMySubscription()) {
                    is SubscriptionResult.Found -> SubscriptionUiState.Content(result.subscription)
                    SubscriptionResult.None -> SubscriptionUiState.NotRequired
                    is SubscriptionResult.Failure -> SubscriptionUiState.Failed(result.error)
                }
        }
    }
}

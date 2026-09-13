package bo.saludencasa.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

class StartupViewModel(
    observeSession: ObserveSessionUseCase,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> =
        observeSession().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = SessionState.Loading,
        )
}

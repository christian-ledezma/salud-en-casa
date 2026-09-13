package bo.saludencasa.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

// The stored session is restored asynchronously on start up, so the destination
// is not decided by a single read but by the first state the session reports
// that is no longer Loading (plan.md, HT-05).
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

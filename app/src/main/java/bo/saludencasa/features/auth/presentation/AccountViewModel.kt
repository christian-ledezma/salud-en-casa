package bo.saludencasa.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.model.SignOutResult
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import bo.saludencasa.features.auth.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

sealed interface AccountUiState {
    data object Loading : AccountUiState

    data object SignedOut : AccountUiState

    data class Content(
        val session: AuthSession,
    ) : AccountUiState

    data class Error(
        val error: AuthError,
    ) : AccountUiState
}

class AccountViewModel(
    observeSession: ObserveSessionUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {
    private val signOutError = MutableStateFlow<AuthError?>(null)

    val uiState: StateFlow<AccountUiState> =
        combine(observeSession(), signOutError) { session, error ->
            error?.let(AccountUiState::Error) ?: session.toUiState()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = AccountUiState.Loading,
        )

    fun signOut() {
        viewModelScope.launch {
            signOutError.value =
                when (val result = signOut.invoke()) {
                    SignOutResult.Success -> null
                    is SignOutResult.Failure -> result.error
                }
        }
    }

    fun dismissError() {
        signOutError.value = null
    }
}

private fun SessionState.toUiState(): AccountUiState =
    when (this) {
        SessionState.Loading -> AccountUiState.Loading
        SessionState.SignedOut -> AccountUiState.SignedOut
        is SessionState.SignedIn -> AccountUiState.Content(session)
    }

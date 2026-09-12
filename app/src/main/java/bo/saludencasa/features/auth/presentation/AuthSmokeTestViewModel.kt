package bo.saludencasa.features.auth.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.core.network.GoogleAuthClient
import bo.saludencasa.core.network.SessionState
import bo.saludencasa.core.network.SignInError
import bo.saludencasa.core.network.SignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthSmokeTestUiState {
    data object Loading : AuthSmokeTestUiState

    data object SignedOut : AuthSmokeTestUiState

    data class SignedIn(
        val userId: String,
    ) : AuthSmokeTestUiState

    data class Error(
        val error: SignInError,
    ) : AuthSmokeTestUiState
}

// Temporary: HU-01 replaces this (plan.md, HT-05).
// Consumes GoogleAuthClient directly instead of a use case: an accepted
// deviation for scaffolding with no domain behind it yet (plan.md, HT-05).
class AuthSmokeTestViewModel(
    private val googleAuthClient: GoogleAuthClient,
) : ViewModel() {
    // Overrides the session flow while an attempt is running or has just failed.
    private val attempt = MutableStateFlow<AuthSmokeTestUiState?>(null)

    val uiState: StateFlow<AuthSmokeTestUiState> =
        combine(googleAuthClient.sessionState, attempt) { session, pending ->
            pending ?: when (session) {
                SessionState.Loading -> AuthSmokeTestUiState.Loading
                SessionState.SignedOut -> AuthSmokeTestUiState.SignedOut
                is SessionState.SignedIn -> AuthSmokeTestUiState.SignedIn(session.userId)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = AuthSmokeTestUiState.Loading,
        )

    // Credential Manager needs the activity to show the account chooser, which
    // is why the context arrives as a parameter instead of being held.
    fun signIn(activityContext: Context) {
        attempt.value = AuthSmokeTestUiState.Loading
        viewModelScope.launch {
            attempt.value =
                when (val result = googleAuthClient.signInWithGoogle(activityContext)) {
                    // Success hands control back to the session flow, which is
                    // the only thing that knows who is signed in.
                    is SignInResult.Success -> null

                    is SignInResult.Failure -> AuthSmokeTestUiState.Error(result.error)
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            attempt.value = null
            googleAuthClient.signOut()
        }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

package bo.saludencasa.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthResult
import bo.saludencasa.features.auth.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WelcomeUiState {
    data object Idle : WelcomeUiState

    data object SigningIn : WelcomeUiState

    data object SignedIn : WelcomeUiState

    data class Error(
        val error: AuthError,
    ) : WelcomeUiState
}

class WelcomeViewModel(
    private val signInWithGoogle: SignInWithGoogleUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WelcomeUiState>(WelcomeUiState.Idle)
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    // The credential request arrives as a lambda because Credential Manager
    // needs an activity, and holding one here would outlive it. The screen owns
    // the context; this class stays free of Android and testable on the JVM.
    fun signIn(requestCredential: suspend () -> GoogleCredentialResult) {
        _uiState.value = WelcomeUiState.SigningIn
        viewModelScope.launch {
            _uiState.value =
                when (val credential = requestCredential()) {
                    is GoogleCredentialResult.Failure -> credential.error.toUiState()
                    is GoogleCredentialResult.Success -> exchange(credential)
                }
        }
    }

    private suspend fun exchange(credential: GoogleCredentialResult.Success): WelcomeUiState =
        when (val result = signInWithGoogle(idToken = credential.idToken, rawNonce = credential.rawNonce)) {
            is AuthResult.Success -> WelcomeUiState.SignedIn
            is AuthResult.Failure -> result.error.toUiState()
        }

    // Dismissing the account chooser is a decision, not a failure: RF-01 asks
    // that the screen come back untouched and ready for another attempt.
    private fun AuthError.toUiState(): WelcomeUiState =
        if (this == AuthError.Cancelled) WelcomeUiState.Idle else WelcomeUiState.Error(this)
}

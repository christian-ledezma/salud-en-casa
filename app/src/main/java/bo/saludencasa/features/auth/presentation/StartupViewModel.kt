package bo.saludencasa.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetRoleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

sealed interface StartupDestination {
    data object Loading : StartupDestination

    data object SignIn : StartupDestination

    data object ChooseRole : StartupDestination

    data class Home(
        val role: UserRole,
    ) : StartupDestination

    data class Error(
        val error: ProfileError,
    ) : StartupDestination
}

class StartupViewModel(
    observeSession: ObserveSessionUseCase,
    private val getRole: GetRoleUseCase,
) : ViewModel() {
    private val attempts = MutableStateFlow(0)

    val destination: StateFlow<StartupDestination> =
        combine(observeSession(), attempts) { session, _ -> session }
            .map { it.toDestination() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = StartupDestination.Loading,
            )

    fun retry() {
        attempts.value += 1
    }

    private suspend fun SessionState.toDestination(): StartupDestination =
        when (this) {
            SessionState.Loading -> StartupDestination.Loading
            SessionState.SignedOut -> StartupDestination.SignIn
            is SessionState.SignedIn -> resolveRole()
        }

    private suspend fun resolveRole(): StartupDestination =
        when (val result = getRole()) {
            is RoleResult.Assigned -> StartupDestination.Home(result.role)
            RoleResult.Unassigned -> StartupDestination.ChooseRole
            is RoleResult.Failure -> StartupDestination.Error(result.error)
        }
}

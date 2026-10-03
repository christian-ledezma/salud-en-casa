package bo.saludencasa.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.usecase.GetRolesUseCase
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

    data object Home : StartupDestination

    data class Error(
        val error: ProfileError,
    ) : StartupDestination
}

class StartupViewModel(
    observeSession: ObserveSessionUseCase,
    private val getRoles: GetRolesUseCase,
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

    // Home carries no role: the switch lives in "Mi cuenta", which is the single
    // home for every role until the per-role screens of Sprint 4 exist
    // (docs/decisions.md, 2026-09-14). A role that cannot be read is still not a
    // role that is missing, which is why the failure stops here with a retry
    // instead of repeating the question to someone who already answered it.
    private suspend fun resolveRole(): StartupDestination =
        when (val result = getRoles()) {
            is RoleResult.Loaded -> {
                if (result.roles.hasNoRole) StartupDestination.ChooseRole else StartupDestination.Home
            }

            is RoleResult.Failure -> {
                StartupDestination.Error(result.error)
            }
        }
}

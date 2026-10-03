package bo.saludencasa.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.model.SignOutResult
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import bo.saludencasa.features.auth.domain.usecase.SignOutUseCase
import bo.saludencasa.features.profile.domain.model.AddRoleResult
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileRoles
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.AddRoleUseCase
import bo.saludencasa.features.profile.domain.usecase.GetRolesUseCase
import bo.saludencasa.features.profile.domain.usecase.SwitchActiveRoleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

// roles stays ProfileRoles.none until the read answers, and a person with no
// roles has no control to draw either, so the same value covers both without a
// second nullable. A failed write leaves roles untouched and fills error, which
// is what keeps the person on the role they were actually in.
data class RoleSection(
    val roles: ProfileRoles = ProfileRoles.none,
    val busy: Boolean = false,
    val error: ProfileError? = null,
)

sealed interface AccountUiState {
    data object Loading : AccountUiState

    data object SignedOut : AccountUiState

    data class Content(
        val session: AuthSession,
        val roleSection: RoleSection,
    ) : AccountUiState

    data class Error(
        val error: AuthError,
    ) : AccountUiState
}

class AccountViewModel(
    observeSession: ObserveSessionUseCase,
    private val signOut: SignOutUseCase,
    private val getRoles: GetRolesUseCase,
    private val switchActiveRole: SwitchActiveRoleUseCase,
    private val addRole: AddRoleUseCase,
) : ViewModel() {
    private val signOutError = MutableStateFlow<AuthError?>(null)
    private val roleSection = MutableStateFlow(RoleSection())

    val uiState: StateFlow<AccountUiState> =
        combine(observeSession(), signOutError, roleSection) { session, error, roles ->
            error?.let(AccountUiState::Error) ?: session.toUiState(roles)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = AccountUiState.Loading,
        )

    init {
        loadRoles()
    }

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

    fun switchTo(role: UserRole) {
        val current = roleSection.value
        if (current.busy || current.roles.active == role) return

        roleSection.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            when (val result = switchActiveRole(role)) {
                is SwitchRoleResult.Success -> {
                    loadRoles()
                }

                is SwitchRoleResult.Failure -> {
                    roleSection.value = current.copy(busy = false, error = result.error)
                }
            }
        }
    }

    fun activate(role: AssignableRole) {
        val current = roleSection.value
        if (current.busy) return

        roleSection.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            when (val result = addRole(role)) {
                is AddRoleResult.Success -> {
                    loadRoles()
                }

                is AddRoleResult.Failure -> {
                    roleSection.value = current.copy(busy = false, error = result.error)
                }
            }
        }
    }

    // Reading back instead of assuming which role is now active, for the same
    // reason marking an address as primary re-reads the list: the server decides,
    // and a client that calculates the result can diverge from it
    // (docs/decisions.md, 2026-09-15).
    private fun loadRoles() {
        viewModelScope.launch {
            roleSection.value =
                when (val result = getRoles()) {
                    is RoleResult.Loaded -> RoleSection(roles = result.roles)
                    is RoleResult.Failure -> roleSection.value.copy(busy = false, error = result.error)
                }
        }
    }
}

private fun SessionState.toUiState(roles: RoleSection): AccountUiState =
    when (this) {
        SessionState.Loading -> AccountUiState.Loading
        SessionState.SignedOut -> AccountUiState.SignedOut
        is SessionState.SignedIn -> AccountUiState.Content(session, roles)
    }

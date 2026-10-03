package bo.saludencasa.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.profile.domain.model.AddRoleResult
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.AddRoleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RoleSelectionUiState {
    data class Choosing(
        val selected: AssignableRole?,
    ) : RoleSelectionUiState

    data class Saving(
        val selected: AssignableRole,
    ) : RoleSelectionUiState

    data class Error(
        val selected: AssignableRole,
        val error: ProfileError,
    ) : RoleSelectionUiState

    data class Assigned(
        val role: UserRole,
    ) : RoleSelectionUiState
}

class RoleSelectionViewModel(
    private val addRole: AddRoleUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<RoleSelectionUiState>(RoleSelectionUiState.Choosing(selected = null))

    val uiState: StateFlow<RoleSelectionUiState> = state.asStateFlow()

    fun select(role: AssignableRole) {
        if (state.value is RoleSelectionUiState.Saving) return
        state.value = RoleSelectionUiState.Choosing(selected = role)
    }

    fun confirm() {
        val current = state.value
        if (current is RoleSelectionUiState.Saving) return
        val selected = current.selectedOrNull() ?: return

        state.value = RoleSelectionUiState.Saving(selected = selected)
        viewModelScope.launch {
            state.value =
                when (val result = addRole(selected)) {
                    is AddRoleResult.Success -> RoleSelectionUiState.Assigned(result.role)
                    is AddRoleResult.Failure -> RoleSelectionUiState.Error(selected, result.error)
                }
        }
    }
}

private fun RoleSelectionUiState.selectedOrNull(): AssignableRole? =
    when (this) {
        is RoleSelectionUiState.Choosing -> selected
        is RoleSelectionUiState.Saving -> selected
        is RoleSelectionUiState.Error -> selected
        is RoleSelectionUiState.Assigned -> null
    }

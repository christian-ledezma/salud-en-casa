package bo.saludencasa.features.location.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.model.DeleteAddressResult
import bo.saludencasa.features.location.domain.model.SetPrimaryAddressResult
import bo.saludencasa.features.location.domain.model.SetProfessionalBaseResult
import bo.saludencasa.features.location.domain.usecase.DeleteAddressUseCase
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.location.domain.usecase.SetPrimaryAddressUseCase
import bo.saludencasa.features.location.domain.usecase.SetProfessionalBaseAddressUseCase
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetRolesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AddressListUiState {
    data object Loading : AddressListUiState

    data object Empty : AddressListUiState

    data class Failed(
        val error: AddressError,
    ) : AddressListUiState

    data class Content(
        val addresses: List<Address>,
        // The id under a mark-as-primary or a delete in flight. Only one
        // action runs at a time, so the row it belongs to is the only thing
        // that needs to say so.
        val pendingId: String?,
        val confirmingDeleteId: String?,
        val notice: AddressError?,
        // A patient has no coverage area, so the base is not something they can
        // declare. It depends on holding the professional role and not on it
        // being active: the address is the same wherever they are working from.
        val canDeclareProfessionalBase: Boolean,
    ) : AddressListUiState
}

class AddressListViewModel(
    private val getMyAddresses: GetMyAddressesUseCase,
    private val setPrimaryAddress: SetPrimaryAddressUseCase,
    private val setProfessionalBaseAddress: SetProfessionalBaseAddressUseCase,
    private val deleteAddress: DeleteAddressUseCase,
    private val getRoles: GetRolesUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<AddressListUiState>(AddressListUiState.Loading)

    val uiState: StateFlow<AddressListUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = AddressListUiState.Loading
        viewModelScope.launch {
            // A role read that fails costs the base action, not the list. The
            // addresses are what this screen is for, and hiding one control is a
            // smaller loss than refusing to show them.
            val isProfessional =
                (getRoles() as? RoleResult.Loaded)?.roles?.has(UserRole.PROFESSIONAL) == true

            state.value =
                when (val result = getMyAddresses()) {
                    is AddressListResult.Success -> {
                        if (result.addresses.isEmpty()) {
                            AddressListUiState.Empty
                        } else {
                            AddressListUiState.Content(
                                addresses = result.addresses,
                                pendingId = null,
                                confirmingDeleteId = null,
                                notice = null,
                                canDeclareProfessionalBase = isProfessional,
                            )
                        }
                    }

                    is AddressListResult.Failure -> {
                        AddressListUiState.Failed(result.error)
                    }
                }
        }
    }

    fun onSetProfessionalBaseClick(id: String) {
        val content = currentContent() ?: return
        if (content.pendingId != null || !content.canDeclareProfessionalBase) return

        state.value = content.copy(pendingId = id, notice = null)
        viewModelScope.launch {
            when (val result = setProfessionalBaseAddress(id)) {
                // Re-read for the same reason marking a primary address does:
                // which row stops being the base is decided by
                // addresses_unmark_previous_professional_base, a trigger, and
                // repeating that rule here is how the two copies diverge.
                SetProfessionalBaseResult.Success -> {
                    load()
                }

                is SetProfessionalBaseResult.Failure -> {
                    val current = currentContent() ?: return@launch
                    state.value = current.copy(pendingId = null, notice = result.error)
                }
            }
        }
    }

    fun onSetPrimaryClick(id: String) {
        val content = currentContent() ?: return
        if (content.pendingId != null) return

        state.value = content.copy(pendingId = id, notice = null)
        viewModelScope.launch {
            when (val result = setPrimaryAddress(id)) {
                SetPrimaryAddressResult.Success -> {
                    // The list is re-read instead of patched locally: the
                    // primary flag moving is a trigger's doing
                    // (addresses_unmark_previous_primary), and re-reading is
                    // what shows its effect on every other row without
                    // repeating that rule on this side.
                    load()
                }

                is SetPrimaryAddressResult.Failure -> {
                    val current = currentContent() ?: return@launch
                    state.value = current.copy(pendingId = null, notice = result.error)
                }
            }
        }
    }

    fun onDeleteClick(id: String) {
        val content = currentContent() ?: return
        state.value = content.copy(confirmingDeleteId = id)
    }

    fun onDismissDeleteConfirmation() {
        val content = currentContent() ?: return
        state.value = content.copy(confirmingDeleteId = null)
    }

    fun onConfirmDelete() {
        val content = currentContent() ?: return
        val id = content.confirmingDeleteId ?: return
        if (content.pendingId != null) return

        state.value = content.copy(pendingId = id, confirmingDeleteId = null, notice = null)
        viewModelScope.launch {
            when (val result = deleteAddress(id)) {
                DeleteAddressResult.Success -> {
                    load()
                }

                is DeleteAddressResult.Failure -> {
                    val current = currentContent() ?: return@launch
                    state.value = current.copy(pendingId = null, notice = result.error)
                }
            }
        }
    }

    private fun currentContent(): AddressListUiState.Content? = state.value as? AddressListUiState.Content
}

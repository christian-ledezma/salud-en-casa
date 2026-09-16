package bo.saludencasa.features.location.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.model.DeleteAddressResult
import bo.saludencasa.features.location.domain.model.SetPrimaryAddressResult
import bo.saludencasa.features.location.domain.usecase.DeleteAddressUseCase
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.location.domain.usecase.SetPrimaryAddressUseCase
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
    ) : AddressListUiState
}

class AddressListViewModel(
    private val getMyAddresses: GetMyAddressesUseCase,
    private val setPrimaryAddress: SetPrimaryAddressUseCase,
    private val deleteAddress: DeleteAddressUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<AddressListUiState>(AddressListUiState.Loading)

    val uiState: StateFlow<AddressListUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = AddressListUiState.Loading
        viewModelScope.launch {
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
                            )
                        }
                    }

                    is AddressListResult.Failure -> {
                        AddressListUiState.Failed(result.error)
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

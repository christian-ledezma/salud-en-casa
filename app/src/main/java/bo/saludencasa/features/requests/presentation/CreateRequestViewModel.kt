package bo.saludencasa.features.requests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.domain.model.ServiceTypesResult
import bo.saludencasa.features.catalog.domain.usecase.GetServiceTypeCatalogUseCase
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.usecase.CreateImmediateRequestUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CreateRequestUiState {
    data object Loading : CreateRequestUiState

    // The request is published around an address, so without one there is
    // nowhere to send it and the only way forward is to register one. It travels
    // as its own answer, the way the search screen treats the missing origin.
    data object NoAddress : CreateRequestUiState

    data class Failed(
        val error: RequestError,
    ) : CreateRequestUiState

    data class Content(
        val serviceTypes: List<ServiceType>,
        val addresses: List<Address>,
        val selectedServiceTypeId: String?,
        val selectedAddressId: String?,
        val description: String,
        val budget: String,
        val submitting: Boolean,
        val submitError: RequestError?,
    ) : CreateRequestUiState {
        val canSubmit: Boolean
            get() = selectedServiceTypeId != null && selectedAddressId != null && !submitting
    }

    data object Submitted : CreateRequestUiState
}

class CreateRequestViewModel(
    private val getServiceTypeCatalog: GetServiceTypeCatalogUseCase,
    private val getMyAddresses: GetMyAddressesUseCase,
    private val createImmediateRequest: CreateImmediateRequestUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<CreateRequestUiState>(CreateRequestUiState.Loading)
    val uiState: StateFlow<CreateRequestUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = CreateRequestUiState.Loading
        viewModelScope.launch {
            val addressesResult = getMyAddresses()
            val addresses =
                when (addressesResult) {
                    is AddressListResult.Success -> {
                        addressesResult.addresses
                    }

                    is AddressListResult.Failure -> {
                        state.value = CreateRequestUiState.Failed(addressesResult.error.toRequestError())
                        return@launch
                    }
                }

            if (addresses.isEmpty()) {
                state.value = CreateRequestUiState.NoAddress
                return@launch
            }

            when (val typesResult = getServiceTypeCatalog()) {
                is ServiceTypesResult.Loaded -> {
                    state.value =
                        CreateRequestUiState.Content(
                            serviceTypes = typesResult.types,
                            addresses = addresses,
                            selectedServiceTypeId = null,
                            // The primary address is first (the data source sorts
                            // it there), so it is the one offered by default.
                            selectedAddressId = addresses.firstOrNull { it.isPrimary }?.id ?: addresses.first().id,
                            description = "",
                            budget = "",
                            submitting = false,
                            submitError = null,
                        )
                }

                is ServiceTypesResult.Failure -> {
                    state.value = CreateRequestUiState.Failed(typesResult.error.toRequestError())
                }
            }
        }
    }

    fun onServiceTypeSelected(serviceTypeId: String) {
        updateContent { it.copy(selectedServiceTypeId = serviceTypeId, submitError = null) }
    }

    fun onAddressSelected(addressId: String) {
        updateContent { it.copy(selectedAddressId = addressId, submitError = null) }
    }

    fun onDescriptionChanged(value: String) {
        updateContent { it.copy(description = value, submitError = null) }
    }

    fun onBudgetChanged(value: String) {
        updateContent { it.copy(budget = value, submitError = null) }
    }

    fun onSubmit() {
        val content = currentContent() ?: return
        val serviceTypeId = content.selectedServiceTypeId ?: return
        val addressId = content.selectedAddressId ?: return
        if (content.submitting) return

        state.value = content.copy(submitting = true, submitError = null)
        viewModelScope.launch {
            val result =
                createImmediateRequest(
                    serviceTypeId = serviceTypeId,
                    addressId = addressId,
                    description = content.description,
                    budget = content.budget,
                )
            state.value =
                when (result) {
                    is RequestResult.Created -> {
                        CreateRequestUiState.Submitted
                    }

                    is RequestResult.Failure -> {
                        val current = currentContent() ?: return@launch
                        current.copy(submitting = false, submitError = result.error)
                    }
                }
        }
    }

    private fun updateContent(change: (CreateRequestUiState.Content) -> CreateRequestUiState.Content) {
        val content = currentContent() ?: return
        if (content.submitting) return
        state.update { change(content) }
    }

    private fun currentContent(): CreateRequestUiState.Content? = state.value as? CreateRequestUiState.Content
}

package bo.saludencasa.features.requests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.domain.model.ServiceTypesResult
import bo.saludencasa.features.catalog.domain.usecase.GetServiceTypeCatalogUseCase
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.requests.domain.model.ActiveRequestResult
import bo.saludencasa.features.requests.domain.model.CancelRequestResult
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.OfferThreadsResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import bo.saludencasa.features.requests.domain.usecase.CancelRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.CreateImmediateRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.FindProfessionalsAvailableNowUseCase
import bo.saludencasa.features.requests.domain.usecase.GetActiveImmediateRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetOfferThreadsUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveOfferSignalsUseCase
import bo.saludencasa.features.search.domain.model.NearbyProfessional
import bo.saludencasa.features.search.domain.model.NearbyProfessionalsPageResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface NearbyState {
    data object Searching : NearbyState

    data class Found(
        val professionals: List<NearbyProfessional>,
        val more: Boolean,
    ) : NearbyState

    // The map is a help and not a precondition: a search that fails leaves the
    // form working, the way the catalog filter of the list does.
    data object Unavailable : NearbyState
}

sealed interface ImmediateCareUiState {
    data object Loading : ImmediateCareUiState

    // The request is published around an address, so without one there is
    // nowhere to send it and the only way forward is to register one.
    data object NoAddress : ImmediateCareUiState

    data class Failed(
        val error: RequestError,
    ) : ImmediateCareUiState

    data class Form(
        val serviceTypes: List<ServiceType>,
        val addresses: List<Address>,
        val selectedServiceTypeId: String?,
        val selectedAddressId: String,
        val typeListExpanded: Boolean,
        val description: String,
        val budget: String,
        val nearby: NearbyState,
        val selectedProfessionalId: String?,
        val submitting: Boolean,
        val submitError: RequestError?,
    ) : ImmediateCareUiState {
        val selectedAddress: Address
            get() = addresses.first { it.id == selectedAddressId }

        val selectedServiceType: ServiceType?
            get() = serviceTypes.firstOrNull { it.id == selectedServiceTypeId }

        val canSubmit: Boolean
            get() = selectedServiceTypeId != null && !submitting
    }

    data class Waiting(
        val request: ServiceRequest,
        val cancelling: Boolean,
        val cancelError: RequestError?,
        // The live offers, one per professional, shown as cards under the request
        // and updated from the offer signal. A rejected negotiation leaves the
        // list, as in InDrive (docs/decisions.md, 2026-10-27, HT-24).
        val threads: List<OfferThread> = emptyList(),
    ) : ImmediateCareUiState
}

class ImmediateCareViewModel(
    private val getServiceTypeCatalog: GetServiceTypeCatalogUseCase,
    private val getMyAddresses: GetMyAddressesUseCase,
    private val getActiveImmediateRequest: GetActiveImmediateRequestUseCase,
    private val createImmediateRequest: CreateImmediateRequestUseCase,
    private val cancelRequest: CancelRequestUseCase,
    private val findProfessionalsAvailableNow: FindProfessionalsAvailableNowUseCase,
    private val getOfferThreads: GetOfferThreadsUseCase,
    private val observeOfferSignals: ObserveOfferSignalsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<ImmediateCareUiState>(ImmediateCareUiState.Loading)
    val uiState: StateFlow<ImmediateCareUiState> = state.asStateFlow()

    private var loadJob: Job? = null
    private var nearbyJob: Job? = null

    init {
        // While waiting, an offer landing re-queries the offers so its card shows
        // up without the patient reloading (docs/decisions.md, 2026-10-23 and
        // 2026-10-27, HT-24).
        viewModelScope.launch {
            observeOfferSignals(asProfessional = false).collect {
                (state.value as? ImmediateCareUiState.Waiting)?.let { loadOffers(it.request.id) }
            }
        }
    }

    // The form as it was when it was published, so cancelling brings back the
    // same choices instead of a blank page.
    private var publishedForm: ImmediateCareUiState.Form? = null

    // Called on every resume rather than from init: a request that expired or
    // was cancelled from «Mis solicitudes» while the screen was away has to stop
    // showing as waiting, and coming back from registering an address has to
    // load the form.
    fun refresh() {
        if (isBusy()) return
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                when (val active = getActiveImmediateRequest()) {
                    is ActiveRequestResult.Found -> {
                        nearbyJob?.cancel()
                        enterWaiting(active.request)
                    }

                    ActiveRequestResult.None -> {
                        // A form on screen keeps what the patient was typing, and
                        // only its addresses are read again.
                        if (state.value is ImmediateCareUiState.Form) refreshAddresses() else showForm()
                    }

                    // A form or a wait already on screen stays: a read that failed
                    // on coming back says nothing about either.
                    is ActiveRequestResult.Failure -> {
                        val onScreen = state.value
                        if (onScreen !is ImmediateCareUiState.Form && onScreen !is ImmediateCareUiState.Waiting) {
                            state.value = ImmediateCareUiState.Failed(active.error)
                        }
                    }
                }
            }
    }

    fun onTypeListToggle() {
        val form = editableForm() ?: return
        state.value = form.copy(typeListExpanded = !form.typeListExpanded)
    }

    fun onServiceTypeSelected(serviceTypeId: String) {
        val form = editableForm() ?: return
        val changed = form.selectedServiceTypeId != serviceTypeId
        state.value = form.copy(selectedServiceTypeId = serviceTypeId, typeListExpanded = false, submitError = null)
        if (changed) searchNearby()
    }

    fun onAddressSelected(addressId: String) {
        val form = editableForm() ?: return
        if (form.selectedAddressId == addressId) return
        state.value = form.copy(selectedAddressId = addressId, submitError = null)
        searchNearby()
    }

    fun onDescriptionChanged(value: String) {
        val form = editableForm() ?: return
        state.value = form.copy(description = value, submitError = null)
    }

    fun onBudgetChanged(value: String) {
        val form = editableForm() ?: return
        state.value = form.copy(budget = value, submitError = null)
    }

    fun onProfessionalSelected(professionalId: String?) {
        val form = state.value as? ImmediateCareUiState.Form ?: return
        state.value = form.copy(selectedProfessionalId = professionalId)
    }

    fun onSubmit() {
        val form = editableForm() ?: return
        val serviceTypeId = form.selectedServiceTypeId ?: return

        state.value = form.copy(submitting = true, submitError = null, typeListExpanded = false)
        viewModelScope.launch {
            val result =
                createImmediateRequest(
                    serviceTypeId = serviceTypeId,
                    addressId = form.selectedAddressId,
                    description = form.description,
                    budget = form.budget,
                )
            when (result) {
                is RequestResult.Created -> {
                    publishedForm = form
                    nearbyJob?.cancel()
                    showPublished()
                }

                is RequestResult.Failure -> {
                    val current = state.value as? ImmediateCareUiState.Form ?: return@launch
                    state.value = current.copy(submitting = false, submitError = result.error)
                }
            }
        }
    }

    fun onCancel() {
        val waiting = state.value as? ImmediateCareUiState.Waiting ?: return
        if (waiting.cancelling) return

        state.value = waiting.copy(cancelling = true, cancelError = null)
        viewModelScope.launch {
            when (val result = cancelRequest(waiting.request.id)) {
                CancelRequestResult.Success -> {
                    showForm()
                }

                is CancelRequestResult.Failure -> {
                    state.value =
                        waiting.copy(cancelling = false, cancelError = result.error)
                }
            }
        }
    }

    // The request exists by now, so a failed read must not bring the form back:
    // publishing again from it would duplicate the request. The retry of the
    // failure is refresh, which finds it.
    private suspend fun showPublished() {
        when (val active = getActiveImmediateRequest()) {
            is ActiveRequestResult.Found -> {
                enterWaiting(active.request)
            }

            is ActiveRequestResult.Failure -> {
                state.value = ImmediateCareUiState.Failed(active.error)
            }

            // Expired or cancelled in the instant between the two calls.
            ActiveRequestResult.None -> {
                showForm()
            }
        }
    }

    // Coming back from a thread lands here with the same request: its cards stay
    // while they are read again, so they do not blink out and the radar back in.
    private fun enterWaiting(request: ServiceRequest) {
        val onScreen = state.value as? ImmediateCareUiState.Waiting
        state.value =
            if (onScreen?.request?.id == request.id) {
                onScreen.copy(request = request)
            } else {
                ImmediateCareUiState.Waiting(request, cancelling = false, cancelError = null)
            }
        viewModelScope.launch { loadOffers(request.id) }
    }

    // A failed read leaves the cards as they were: the panel is about the wait,
    // and an empty list would say no one has offered when someone has.
    private suspend fun loadOffers(requestId: String) {
        val result = getOfferThreads(requestId)
        if (result is OfferThreadsResult.Loaded) {
            state.update { current ->
                if (current is ImmediateCareUiState.Waiting && current.request.id == requestId) {
                    current.copy(threads = result.threads.filterNot { it.isClosed })
                } else {
                    current
                }
            }
        }
    }

    // Coming back from «Agregar otra dirección»: the list is read again, the rest
    // of the form stays, and the address just added is the one chosen.
    private suspend fun refreshAddresses() {
        val result = getMyAddresses() as? AddressListResult.Success ?: return
        if (result.addresses.isEmpty()) return
        val form = state.value as? ImmediateCareUiState.Form ?: return

        val selected = result.addresses.selectionAfterRefresh(form.addresses, form.selectedAddressId)
        state.value = form.copy(addresses = result.addresses, selectedAddressId = selected)
        if (selected != form.selectedAddressId) searchNearby()
    }

    private suspend fun showForm() {
        val remembered = publishedForm
        if (remembered != null) {
            publishedForm = null
            state.value = remembered.copy(submitting = false, submitError = null)
            searchNearby()
            return
        }

        val addresses =
            when (val result = getMyAddresses()) {
                is AddressListResult.Success -> {
                    result.addresses
                }

                is AddressListResult.Failure -> {
                    state.value = ImmediateCareUiState.Failed(result.error.toRequestError())
                    return
                }
            }
        if (addresses.isEmpty()) {
            state.value = ImmediateCareUiState.NoAddress
            return
        }

        when (val types = getServiceTypeCatalog()) {
            is ServiceTypesResult.Loaded -> {
                state.value =
                    ImmediateCareUiState.Form(
                        serviceTypes = types.types,
                        addresses = addresses,
                        selectedServiceTypeId = null,
                        // The primary address is the home where the person
                        // receives care (RN-02), so it is the one offered first.
                        selectedAddressId = addresses.firstOrNull { it.isPrimary }?.id ?: addresses.first().id,
                        typeListExpanded = false,
                        description = "",
                        budget = "",
                        nearby = NearbyState.Searching,
                        selectedProfessionalId = null,
                        submitting = false,
                        submitError = null,
                    )
                searchNearby()
            }

            is ServiceTypesResult.Failure -> {
                state.value = ImmediateCareUiState.Failed(types.error.toRequestError())
            }
        }
    }

    // Cancelled before it starts again: a reply about the address or the
    // service the patient just left, landing last, would draw people who do not
    // answer the question on screen.
    private fun searchNearby() {
        val form = state.value as? ImmediateCareUiState.Form ?: return
        nearbyJob?.cancel()
        state.value = form.copy(nearby = NearbyState.Searching, selectedProfessionalId = null)

        nearbyJob =
            viewModelScope.launch {
                val result = findProfessionalsAvailableNow(form.selectedAddress.coordinate, form.selectedServiceTypeId)
                val nearby =
                    when (result) {
                        is NearbyProfessionalsPageResult.Loaded -> {
                            NearbyState.Found(result.professionals, more = !result.endReached)
                        }

                        is NearbyProfessionalsPageResult.Failure -> {
                            NearbyState.Unavailable
                        }
                    }
                state.update { current ->
                    if (current is ImmediateCareUiState.Form) current.copy(nearby = nearby) else current
                }
            }
    }

    private fun isBusy(): Boolean =
        when (val current = state.value) {
            is ImmediateCareUiState.Form -> current.submitting
            is ImmediateCareUiState.Waiting -> current.cancelling
            else -> false
        }

    private fun editableForm(): ImmediateCareUiState.Form? =
        (state.value as? ImmediateCareUiState.Form)?.takeUnless { it.submitting }
}

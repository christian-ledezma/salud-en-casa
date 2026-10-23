package bo.saludencasa.features.requests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ProfessionalServicesResult
import bo.saludencasa.features.catalog.domain.usecase.GetProfessionalServicesUseCase
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.usecase.GetPublicProfileUseCase
import bo.saludencasa.features.requests.domain.model.BookableStart
import bo.saludencasa.features.requests.domain.model.BookableTimes
import bo.saludencasa.features.requests.domain.model.BookableTimesResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.usecase.CreateScheduledRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetBookableTimesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface TimesState {
    data object Loading : TimesState

    data class Loaded(
        val times: BookableTimes,
    ) : TimesState

    data class Failed(
        val error: RequestError,
    ) : TimesState
}

sealed interface ScheduleAppointmentUiState {
    data object Loading : ScheduleAppointmentUiState

    data object NotBookable : ScheduleAppointmentUiState

    data object NoAddress : ScheduleAppointmentUiState

    data class Failed(
        val error: RequestError,
    ) : ScheduleAppointmentUiState

    data class Content(
        val professional: PublicProfile,
        val services: List<ProfessionalService>,
        val addresses: List<Address>,
        val selectedServiceTypeId: String,
        val times: TimesState,
        val selectedDate: LocalDate?,
        val selectedStart: BookableStart?,
        val selectedAddressId: String,
        val description: String,
        val budget: String,
        val submitting: Boolean,
        val submitError: RequestError?,
    ) : ScheduleAppointmentUiState {
        val selectedService: ProfessionalService?
            get() = services.firstOrNull { it.type.id == selectedServiceTypeId }

        val canSubmit: Boolean
            get() = selectedStart != null && !submitting
    }

    data object Submitted : ScheduleAppointmentUiState
}

class ScheduleAppointmentViewModel(
    private val professionalId: String,
    // The service the patient filtered the list by, if any (docs/decisions.md,
    // 2026-10-23, HT-23).
    private val preferredServiceTypeId: String?,
    private val getPublicProfile: GetPublicProfileUseCase,
    private val getProfessionalServices: GetProfessionalServicesUseCase,
    private val getMyAddresses: GetMyAddressesUseCase,
    private val getBookableTimes: GetBookableTimesUseCase,
    private val createScheduledRequest: CreateScheduledRequestUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<ScheduleAppointmentUiState>(ScheduleAppointmentUiState.Loading)
    val uiState: StateFlow<ScheduleAppointmentUiState> = state.asStateFlow()

    private var timesJob: Job? = null

    init {
        load()
    }

    fun load() {
        state.value = ScheduleAppointmentUiState.Loading
        viewModelScope.launch {
            state.value = initialState()
            (state.value as? ScheduleAppointmentUiState.Content)?.let { loadTimes(it.selectedServiceTypeId) }
        }
    }

    fun onServiceSelected(serviceTypeId: String) {
        val content = editableContent() ?: return
        if (content.selectedServiceTypeId == serviceTypeId) return

        state.value =
            content.copy(
                selectedServiceTypeId = serviceTypeId,
                times = TimesState.Loading,
                selectedStart = null,
                submitError = null,
            )
        loadTimes(serviceTypeId)
    }

    fun onDateSelected(date: LocalDate) {
        val content = editableContent() ?: return
        val times = (content.times as? TimesState.Loaded)?.times ?: return
        if (!times.hasTimesOn(date)) return

        state.value =
            content.copy(
                selectedDate = date,
                selectedStart = content.selectedStart?.takeIf { it.date == date },
                submitError = null,
            )
    }

    fun onTimeSelected(start: BookableStart) {
        val content = editableContent() ?: return
        state.value = content.copy(selectedDate = start.date, selectedStart = start, submitError = null)
    }

    fun onAddressSelected(addressId: String) {
        val content = editableContent() ?: return
        state.value = content.copy(selectedAddressId = addressId, submitError = null)
    }

    fun onDescriptionChanged(value: String) {
        val content = editableContent() ?: return
        state.value = content.copy(description = value, submitError = null)
    }

    fun onBudgetChanged(value: String) {
        val content = editableContent() ?: return
        state.value = content.copy(budget = value, submitError = null)
    }

    fun onRetryTimes() {
        val content = editableContent() ?: return
        state.value = content.copy(times = TimesState.Loading)
        loadTimes(content.selectedServiceTypeId)
    }

    fun onSubmit() {
        val content = editableContent() ?: return
        val start = content.selectedStart ?: return

        state.value = content.copy(submitting = true, submitError = null)
        viewModelScope.launch {
            val result =
                createScheduledRequest(
                    professionalId = professionalId,
                    serviceTypeId = content.selectedServiceTypeId,
                    addressId = content.selectedAddressId,
                    start = start,
                    description = content.description,
                    budget = content.budget,
                )

            when (result) {
                is RequestResult.Created -> {
                    state.value = ScheduleAppointmentUiState.Submitted
                }

                is RequestResult.Failure -> {
                    val current = state.value as? ScheduleAppointmentUiState.Content ?: return@launch
                    if (result.error == RequestError.TimeNotBookable) {
                        state.value =
                            current.copy(
                                submitting = false,
                                submitError = result.error,
                                selectedStart = null,
                                times = TimesState.Loading,
                            )
                        loadTimes(current.selectedServiceTypeId)
                    } else {
                        state.value = current.copy(submitting = false, submitError = result.error)
                    }
                }
            }
        }
    }

    private suspend fun initialState(): ScheduleAppointmentUiState =
        coroutineScope {
            val profileRead = async { getPublicProfile(professionalId) }
            val servicesRead = async { getProfessionalServices(professionalId) }
            val addressesRead = async { getMyAddresses() }

            val professional =
                when (val result = profileRead.await()) {
                    is PublicProfileResult.Success -> result.profile

                    PublicProfileResult.NotPublished -> return@coroutineScope ScheduleAppointmentUiState.NotBookable

                    is PublicProfileResult.Failure -> return@coroutineScope ScheduleAppointmentUiState.Failed(
                        result.error.toRequestError(),
                    )
                }

            val services =
                when (val result = servicesRead.await()) {
                    is ProfessionalServicesResult.Loaded -> {
                        result.services
                    }

                    is ProfessionalServicesResult.Failure -> {
                        return@coroutineScope ScheduleAppointmentUiState.Failed(result.error.toRequestError())
                    }
                }
            if (services.isEmpty()) return@coroutineScope ScheduleAppointmentUiState.NotBookable

            val addresses =
                when (val result = addressesRead.await()) {
                    is AddressListResult.Success -> {
                        result.addresses
                    }

                    is AddressListResult.Failure -> {
                        return@coroutineScope ScheduleAppointmentUiState.Failed(result.error.toRequestError())
                    }
                }
            if (addresses.isEmpty()) return@coroutineScope ScheduleAppointmentUiState.NoAddress

            ScheduleAppointmentUiState.Content(
                professional = professional,
                services = services,
                addresses = addresses,
                selectedServiceTypeId =
                    services.firstOrNull { it.type.id == preferredServiceTypeId }?.type?.id ?: services.first().type.id,
                times = TimesState.Loading,
                selectedDate = null,
                selectedStart = null,
                selectedAddressId = addresses.firstOrNull { it.isPrimary }?.id ?: addresses.first().id,
                description = "",
                budget = "",
                submitting = false,
                submitError = null,
            )
        }

    private fun loadTimes(serviceTypeId: String) {
        timesJob?.cancel()
        timesJob =
            viewModelScope.launch {
                val result = getBookableTimes(professionalId, serviceTypeId)
                state.update { current ->
                    if (current !is ScheduleAppointmentUiState.Content) return@update current
                    when (result) {
                        is BookableTimesResult.Loaded -> current.withTimes(result.times)
                        is BookableTimesResult.Failure -> current.copy(times = TimesState.Failed(result.error))
                    }
                }
            }
    }

    private fun editableContent(): ScheduleAppointmentUiState.Content? =
        (state.value as? ScheduleAppointmentUiState.Content)?.takeUnless { it.submitting }
}

private fun ScheduleAppointmentUiState.Content.withTimes(times: BookableTimes): ScheduleAppointmentUiState.Content {
    val date = selectedDate?.takeIf(times::hasTimesOn) ?: times.firstDayWithTimes()
    val start = selectedStart?.takeIf { picked -> times.starts.any { it.startAt == picked.startAt } }
    return copy(times = TimesState.Loaded(times), selectedDate = date, selectedStart = start)
}

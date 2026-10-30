package bo.saludencasa.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.profile.domain.model.AvailabilityResult
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.SetAvailabilityUseCase
import bo.saludencasa.features.requests.domain.model.InboxPageResult
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.usecase.GetInboxUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveInboxSignalsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val NEARBY_PREVIEW_SIZE = 3

sealed interface NearbyRequests {
    data object None : NearbyRequests

    data class Some(
        val requests: List<InboxRequest>,
    ) : NearbyRequests

    data class Failed(
        val error: RequestError,
    ) : NearbyRequests
}

sealed interface ProfessionalHomeUiState {
    data object Loading : ProfessionalHomeUiState

    data class Content(
        val availableNow: Boolean,
        val availabilityError: ProfileError?,
        val verified: Boolean,
        val nearby: NearbyRequests,
    ) : ProfessionalHomeUiState

    data class Failed(
        val error: ProfileError,
    ) : ProfessionalHomeUiState
}

class ProfessionalHomeViewModel(
    private val getProfile: GetProfileUseCase,
    private val setAvailability: SetAvailabilityUseCase,
    private val getInbox: GetInboxUseCase,
    observeInboxSignals: ObserveInboxSignalsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<ProfessionalHomeUiState>(ProfessionalHomeUiState.Loading)
    val uiState: StateFlow<ProfessionalHomeUiState> = state.asStateFlow()

    init {
        // The same signal the inbox answers, so a request published in range
        // reaches the home screen too without the professional reloading.
        viewModelScope.launch {
            observeInboxSignals().collect { refreshNearby() }
        }
    }

    // A refresh that fails leaves what is on screen in place, the way the inbox
    // treats a dropped background refetch.
    fun refresh() {
        viewModelScope.launch {
            val profile = getProfile()
            val nearby = getInbox(offset = 0).toNearby()
            state.value =
                when (profile) {
                    is ProfileResult.Success -> {
                        val professional = profile.profile.professional
                        ProfessionalHomeUiState.Content(
                            availableNow = professional?.availableNow == true,
                            availabilityError = null,
                            verified = professional?.isVerified == true,
                            nearby = nearby,
                        )
                    }

                    is ProfileResult.Failure -> {
                        state.value as? ProfessionalHomeUiState.Content
                            ?: ProfessionalHomeUiState.Failed(profile.error)
                    }
                }
        }
    }

    // RF-02.5 is a statement about this moment, so the switch writes on its own
    // and keeps the value the server accepted, as on the profile screen.
    fun onAvailabilityChange(availableNow: Boolean) {
        val content = state.value as? ProfessionalHomeUiState.Content ?: return

        state.value = content.copy(availableNow = availableNow, availabilityError = null)
        viewModelScope.launch {
            val result = setAvailability(availableNow)
            if (result is AvailabilityResult.Failure) {
                state.update { current ->
                    (current as? ProfessionalHomeUiState.Content)
                        ?.copy(availableNow = content.availableNow, availabilityError = result.error)
                        ?: current
                }
            }
        }
    }

    private fun refreshNearby() {
        viewModelScope.launch {
            val nearby = getInbox(offset = 0).toNearby()
            if (nearby is NearbyRequests.Failed) return@launch
            state.update { current ->
                (current as? ProfessionalHomeUiState.Content)?.copy(nearby = nearby) ?: current
            }
        }
    }
}

private fun InboxPageResult.toNearby(): NearbyRequests =
    when (this) {
        is InboxPageResult.Loaded -> {
            if (requests.isEmpty()) NearbyRequests.None else NearbyRequests.Some(requests.take(NEARBY_PREVIEW_SIZE))
        }

        is InboxPageResult.Failure -> {
            NearbyRequests.Failed(error)
        }
    }

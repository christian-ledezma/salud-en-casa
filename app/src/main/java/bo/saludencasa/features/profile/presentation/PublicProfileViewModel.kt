package bo.saludencasa.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.usecase.GetPublicProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PublicProfileUiState {
    data object Loading : PublicProfileUiState

    data object NotPublished : PublicProfileUiState

    data class Content(
        val profile: PublicProfile,
    ) : PublicProfileUiState

    data class Failed(
        val error: ProfileError,
    ) : PublicProfileUiState
}

class PublicProfileViewModel(
    private val professionalId: String,
    private val getPublicProfile: GetPublicProfileUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<PublicProfileUiState>(PublicProfileUiState.Loading)

    val uiState: StateFlow<PublicProfileUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = PublicProfileUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getPublicProfile(professionalId)) {
                    is PublicProfileResult.Success -> PublicProfileUiState.Content(result.profile)
                    PublicProfileResult.NotPublished -> PublicProfileUiState.NotPublished
                    is PublicProfileResult.Failure -> PublicProfileUiState.Failed(result.error)
                }
        }
    }
}

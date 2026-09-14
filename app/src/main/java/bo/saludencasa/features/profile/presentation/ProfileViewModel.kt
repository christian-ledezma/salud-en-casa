package bo.saludencasa.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.profile.domain.model.PatientDraft
import bo.saludencasa.features.profile.domain.model.ProfileDraft
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.UserProfile
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.SaveProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProfileForm(
    val fullName: String,
    val phone: String,
    val birthDate: LocalDate?,
    val emergencyContact: String,
    val medicalNotes: String,
)

data class ProfileHeader(
    val photoUrl: String?,
    val role: UserRole?,
)

sealed interface SaveStatus {
    data object Idle : SaveStatus

    data object Saving : SaveStatus

    data object Saved : SaveStatus

    data class Failed(
        val error: ProfileError,
    ) : SaveStatus
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Failed(
        val error: ProfileError,
    ) : ProfileUiState

    data class Content(
        val header: ProfileHeader,
        val form: ProfileForm,
        val status: SaveStatus,
    ) : ProfileUiState
}

class ProfileViewModel(
    private val getProfile: GetProfileUseCase,
    private val saveProfile: SaveProfileUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)

    val uiState: StateFlow<ProfileUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = ProfileUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getProfile()) {
                    is ProfileResult.Success -> result.profile.toContent(SaveStatus.Idle)
                    is ProfileResult.Failure -> ProfileUiState.Failed(result.error)
                }
        }
    }

    fun onFormChange(form: ProfileForm) {
        val content = state.value as? ProfileUiState.Content ?: return
        if (content.status is SaveStatus.Saving) return
        state.value = content.copy(form = form, status = SaveStatus.Idle)
    }

    fun save() {
        val content = state.value as? ProfileUiState.Content ?: return
        if (content.status is SaveStatus.Saving) return

        state.value = content.copy(status = SaveStatus.Saving)
        viewModelScope.launch {
            val draft = content.form.toDraft(isPatient = content.header.role == UserRole.PATIENT)
            state.value =
                when (val result = saveProfile(draft)) {
                    is ProfileResult.Success -> result.profile.toContent(SaveStatus.Saved)
                    is ProfileResult.Failure -> content.copy(status = SaveStatus.Failed(result.error))
                }
        }
    }
}

private fun UserProfile.toContent(status: SaveStatus): ProfileUiState.Content =
    ProfileUiState.Content(
        header = ProfileHeader(photoUrl = photoUrl, role = role),
        form =
            ProfileForm(
                fullName = fullName,
                phone = phone?.value.orEmpty(),
                birthDate = patient?.birthDate?.value,
                emergencyContact = patient?.emergencyContact.orEmpty(),
                medicalNotes = patient?.medicalNotes.orEmpty(),
            ),
        status = status,
    )

private fun ProfileForm.toDraft(isPatient: Boolean): ProfileDraft =
    ProfileDraft(
        fullName = fullName,
        phone = phone,
        patient =
            if (isPatient) {
                PatientDraft(
                    birthDate = birthDate,
                    emergencyContact = emergencyContact,
                    medicalNotes = medicalNotes,
                )
            } else {
                null
            },
    )

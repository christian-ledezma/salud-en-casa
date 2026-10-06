package bo.saludencasa.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.profile.domain.model.AvailabilityResult
import bo.saludencasa.features.profile.domain.model.PatientDraft
import bo.saludencasa.features.profile.domain.model.ProfessionalDraft
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileDraft
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.UserProfile
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.SaveProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.SetAvailabilityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

data class ProfileForm(
    val fullName: String,
    val phone: String,
    val birthDate: LocalDate?,
    val emergencyContact: String,
    val medicalNotes: String,
    val professionalType: ProfessionalType?,
    val specialty: String,
    val biography: String,
    val yearsOfExperience: String,
    val baseRateBob: String,
    val coverageRadiusKm: String,
)

data class ProfileHeader(
    val userId: String,
    val photoUrl: String?,
    val activeRole: UserRole?,
    val isVerified: Boolean,
)

// Immediate availability is not part of the form. RF-02.5 is a statement about
// this moment, so the switch writes on its own instead of waiting for the save
// button, and it keeps the value the server accepted rather than the one the
// finger asked for.
data class AvailabilityState(
    val availableNow: Boolean,
    val error: ProfileError? = null,
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
        val availability: AvailabilityState?,
    ) : ProfileUiState
}

class ProfileViewModel(
    private val getProfile: GetProfileUseCase,
    private val saveProfile: SaveProfileUseCase,
    private val setAvailability: SetAvailabilityUseCase,
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
            val draft = content.form.toDraft(content.header.activeRole)
            state.value =
                when (val result = saveProfile(draft)) {
                    is ProfileResult.Success -> result.profile.toContent(SaveStatus.Saved)
                    is ProfileResult.Failure -> content.copy(status = SaveStatus.Failed(result.error))
                }
        }
    }

    fun onAvailabilityChange(availableNow: Boolean) {
        val content = state.value as? ProfileUiState.Content ?: return
        val previous = content.availability ?: return

        state.value = content.copy(availability = AvailabilityState(availableNow = availableNow))
        viewModelScope.launch {
            val result = setAvailability(availableNow)
            if (result is AvailabilityResult.Failure) {
                val current = state.value as? ProfileUiState.Content ?: return@launch
                state.value = current.copy(availability = previous.copy(error = result.error))
            }
        }
    }
}

private fun UserProfile.toContent(status: SaveStatus): ProfileUiState.Content =
    ProfileUiState.Content(
        header =
            ProfileHeader(
                userId = userId,
                photoUrl = photoUrl,
                activeRole = activeRole,
                isVerified = activeRole == UserRole.PROFESSIONAL && professional?.isVerified == true,
            ),
        form =
            ProfileForm(
                fullName = fullName,
                phone = phone?.value.orEmpty(),
                birthDate = patient?.birthDate?.value,
                emergencyContact = patient?.emergencyContact.orEmpty(),
                medicalNotes = patient?.medicalNotes.orEmpty(),
                professionalType = professional?.professionalType,
                specialty = professional?.specialty.orEmpty(),
                biography = professional?.biography.orEmpty(),
                yearsOfExperience = professional?.yearsOfExperience?.toString().orEmpty(),
                baseRateBob = professional?.baseRateBob?.toEditableText().orEmpty(),
                coverageRadiusKm = professional?.coverageRadiusKm?.toEditableText().orEmpty(),
            ),
        status = status,
        // Gated on the active role and not merely on the professional row
        // existing. Someone who holds both roles has that row while working as a
        // patient, and RF-02.5 is a statement a patient has no way to make.
        availability =
            professional
                ?.takeIf { activeRole == UserRole.PROFESSIONAL }
                ?.let { AvailabilityState(availableNow = it.availableNow) },
    )

// Only the active role's section travels. For someone who holds both roles this
// is what keeps an edit made as a patient from writing professional columns, and
// save_my_profile dispatches on the same active role on the server, so the two
// cannot disagree.
private fun ProfileForm.toDraft(activeRole: UserRole?): ProfileDraft =
    ProfileDraft(
        fullName = fullName,
        phone = phone,
        patient =
            if (activeRole == UserRole.PATIENT) {
                PatientDraft(
                    birthDate = birthDate,
                    emergencyContact = emergencyContact,
                    medicalNotes = medicalNotes,
                )
            } else {
                null
            },
        professional =
            if (activeRole == UserRole.PROFESSIONAL) {
                ProfessionalDraft(
                    professionalType = professionalType,
                    specialty = specialty,
                    biography = biography,
                    yearsOfExperience = yearsOfExperience,
                    baseRateBob = baseRateBob,
                    coverageRadiusKm = coverageRadiusKm,
                )
            } else {
                null
            },
    )

// 5.00 is what the column stores and "5" is what the person typed. Showing the
// stored scale back would make every visit to the screen look like an edit.
private fun BigDecimal.toEditableText(): String = stripTrailingZeros().toPlainString()

package bo.saludencasa.features.profile.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.ProfileError

@StringRes
internal fun ProfileError.messageRes(): Int =
    when (this) {
        ProfileError.NotSignedIn -> R.string.error_profile_not_signed_in
        ProfileError.ProfileNotFound -> R.string.error_profile_not_found
        ProfileError.RoleAlreadyHeld -> R.string.error_profile_role_already_held
        ProfileError.RoleNotHeld -> R.string.error_profile_role_not_held
        ProfileError.InvalidName -> R.string.error_profile_invalid_name
        ProfileError.InvalidPhone -> R.string.error_profile_invalid_phone
        ProfileError.InvalidBirthDate -> R.string.error_profile_invalid_birth_date
        ProfileError.InvalidBaseRate -> R.string.error_profile_invalid_base_rate
        ProfileError.InvalidCoverageRadius -> R.string.error_profile_invalid_coverage_radius
        ProfileError.InvalidYearsOfExperience -> R.string.error_profile_invalid_years_of_experience
        ProfileError.NetworkUnavailable -> R.string.error_network_unavailable
        ProfileError.Unexpected -> R.string.error_unexpected
    }

// An error that names a field is drawn under that field; every other one is
// about the form as a whole and goes next to the save button.
internal fun ProfileError.isAboutAField(): Boolean =
    this == ProfileError.InvalidName ||
        this == ProfileError.InvalidPhone ||
        this == ProfileError.InvalidBirthDate ||
        this == ProfileError.InvalidBaseRate ||
        this == ProfileError.InvalidCoverageRadius ||
        this == ProfileError.InvalidYearsOfExperience

@Composable
internal fun ProfileError?.messageFor(field: ProfileError): String? =
    if (this == field) stringResource(messageRes()) else null

package bo.saludencasa.features.profile.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.ProfileError

@StringRes
internal fun ProfileError.messageRes(): Int =
    when (this) {
        ProfileError.NotSignedIn -> R.string.error_profile_not_signed_in
        ProfileError.ProfileNotFound -> R.string.error_profile_not_found
        ProfileError.RoleAlreadyAssigned -> R.string.error_profile_role_already_assigned
        ProfileError.NetworkUnavailable -> R.string.error_network_unavailable
        ProfileError.Unexpected -> R.string.error_unexpected
    }

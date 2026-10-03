package bo.saludencasa.features.profile.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.UserRole

@StringRes
internal fun UserRole.labelRes(): Int =
    when (this) {
        UserRole.PATIENT -> R.string.profile_role_patient
        UserRole.PROFESSIONAL -> R.string.profile_role_professional
        UserRole.ADMIN -> R.string.profile_role_admin
    }

// The segmented control holds two options across the width of a phone, and
// docs/design-system.md section 5 warns that long labels clip there -- it is why
// the professional type uses a radio group instead. "Profesional de salud" broke
// out of its segment at 200 % font size on a device; these shorter labels are
// unambiguous under the heading that already says "Estás usando la aplicación
// como". ADMIN never reaches the control, but the when has to be exhaustive.
@StringRes
internal fun UserRole.shortLabelRes(): Int =
    when (this) {
        UserRole.PATIENT -> R.string.auth_account_role_short_patient
        UserRole.PROFESSIONAL -> R.string.auth_account_role_short_professional
        UserRole.ADMIN -> R.string.profile_role_admin
    }

@StringRes
internal fun AssignableRole.activateRes(): Int =
    when (this) {
        AssignableRole.PATIENT -> R.string.auth_account_activate_patient
        AssignableRole.PROFESSIONAL -> R.string.auth_account_activate_professional
    }

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

// "Profesional de salud" broke out of the old segmented control at 200 % font
// size on a device. The short labels stayed for the pill beside the greeting and
// the account menu, which already says "Usar la aplicación como"
// (docs/decisions.md, 2026-10-30, HT-25).
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

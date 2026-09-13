package bo.saludencasa.features.auth.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.auth.domain.model.AuthError

// The domain carries an error type and never a phrase, so the translation to a
// resource key happens here, where the screen that shows it lives
// (.claude/rules/i18n.md).
@StringRes
internal fun AuthError.messageRes(): Int =
    when (this) {
        AuthError.MissingConfiguration -> R.string.error_sign_in_missing_configuration

        AuthError.NoGoogleAccount -> R.string.error_sign_in_no_google_account

        AuthError.CredentialProviderFailure -> R.string.error_sign_in_credential_provider_failure

        AuthError.TokenRejected -> R.string.error_sign_in_token_rejected

        AuthError.NetworkUnavailable -> R.string.error_network_unavailable

        // Dismissing the chooser never reaches a message: the screen returns to
        // its resting state instead (WelcomeViewModel).
        AuthError.Cancelled -> R.string.error_unexpected

        AuthError.Unexpected -> R.string.error_unexpected
    }

package bo.saludencasa.features.auth.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.auth.domain.model.AuthError

@StringRes
internal fun AuthError.messageRes(): Int =
    when (this) {
        AuthError.MissingConfiguration -> R.string.error_sign_in_missing_configuration
        AuthError.NoGoogleAccount -> R.string.error_sign_in_no_google_account
        AuthError.CredentialProviderFailure -> R.string.error_sign_in_credential_provider_failure
        AuthError.TokenRejected -> R.string.error_sign_in_token_rejected
        AuthError.NetworkUnavailable -> R.string.error_network_unavailable
        AuthError.Cancelled -> R.string.error_unexpected
        AuthError.Unexpected -> R.string.error_unexpected
    }

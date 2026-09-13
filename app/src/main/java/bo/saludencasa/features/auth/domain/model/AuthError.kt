package bo.saludencasa.features.auth.domain.model

sealed interface AuthError {
    data object MissingConfiguration : AuthError

    data object Cancelled : AuthError

    data object NoGoogleAccount : AuthError

    data object CredentialProviderFailure : AuthError

    data object TokenRejected : AuthError

    data object NetworkUnavailable : AuthError

    data object Unexpected : AuthError
}

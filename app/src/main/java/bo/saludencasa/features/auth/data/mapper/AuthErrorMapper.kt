package bo.saludencasa.features.auth.data.mapper

import bo.saludencasa.features.auth.domain.model.AuthError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException

internal fun Throwable.toAuthError(): AuthError =
    when (this) {
        is HttpRequestTimeoutException, is HttpRequestException -> AuthError.NetworkUnavailable
        is RestException -> AuthError.TokenRejected
        else -> AuthError.Unexpected
    }

package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.features.profile.domain.model.ProfileError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException

internal fun Throwable.toProfileError(): ProfileError =
    when (this) {
        is HttpRequestTimeoutException, is HttpRequestException -> ProfileError.NetworkUnavailable
        else -> ProfileError.Unexpected
    }

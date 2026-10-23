package bo.saludencasa.features.notifications.data.mapper

import bo.saludencasa.features.notifications.domain.model.NotificationError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

internal fun Throwable.toNotificationError(): NotificationError =
    when {
        message?.contains(NOT_SIGNED_IN) == true -> {
            NotificationError.NotSignedIn
        }

        this is HttpRequestTimeoutException || this is HttpRequestException || this is IOException -> {
            NotificationError.NetworkUnavailable
        }

        else -> {
            NotificationError.Unexpected
        }
    }

private const val NOT_SIGNED_IN = "not_signed_in"

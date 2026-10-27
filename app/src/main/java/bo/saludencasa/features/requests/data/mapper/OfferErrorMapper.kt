package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.domain.model.OfferError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

// The insert policy refuses with the platform's row-level security error, which
// names no reason the person can act on: a closed request, an ineligible
// professional and one's own request all look the same here. MakeOfferScreen
// shows those proactively; this maps the refusal to a single NotAllowed.
internal fun Throwable.toOfferError(): OfferError {
    message?.let { body ->
        if (body.contains(ROW_LEVEL_SECURITY) || body.contains(LIFECYCLE_GUARD)) {
            return OfferError.NotAllowed
        }
    }

    return when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> OfferError.NetworkUnavailable
        else -> OfferError.Unexpected
    }
}

private const val ROW_LEVEL_SECURITY = "row-level security"
private const val LIFECYCLE_GUARD = "request_status_change_requires_the_atomic_operation"

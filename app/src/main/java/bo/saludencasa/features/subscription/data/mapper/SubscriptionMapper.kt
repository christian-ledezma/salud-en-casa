package bo.saludencasa.features.subscription.data.mapper

import bo.saludencasa.features.subscription.data.model.SubscriptionDto
import bo.saludencasa.features.subscription.domain.model.Subscription
import bo.saludencasa.features.subscription.domain.model.SubscriptionError
import bo.saludencasa.features.subscription.domain.model.SubscriptionState
import bo.saludencasa.features.subscription.domain.model.SubscriptionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.time.OffsetDateTime

// null when the row cannot be read: a status this version does not know, or a
// period without a usable end. Reading it as no period would hide an expiry.
internal fun SubscriptionDto.toSubscriptionState(): SubscriptionState? {
    if (status == null) return SubscriptionState(required = required, subscription = null)

    val parsedStatus = SubscriptionStatus.entries.firstOrNull { it.name == status } ?: return null
    // PostgREST sends the offset as +00:00, which Instant.parse does not take.
    val end = endsAt?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() } ?: return null

    return SubscriptionState(required = required, subscription = Subscription(parsedStatus, end))
}

internal fun Throwable.toSubscriptionError(): SubscriptionError =
    when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> {
            SubscriptionError.NetworkUnavailable
        }

        else -> {
            SubscriptionError.Unexpected
        }
    }

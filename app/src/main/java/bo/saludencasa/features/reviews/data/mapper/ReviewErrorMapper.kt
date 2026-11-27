package bo.saludencasa.features.reviews.data.mapper

import bo.saludencasa.features.reviews.domain.model.ReviewError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

internal fun Throwable.toReviewError(): ReviewError {
    val reason = message.orEmpty()
    // The unique key on (service_id, author_id): two taps, or two phones.
    if (ALREADY_RATED in reason) return ReviewError.AlreadyRated
    // The insert policy says no more than this for an attention that is not theirs,
    // is not completed, or names another recipient.
    if (ROW_LEVEL_SECURITY in reason) return ReviewError.NotAllowed

    return when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> {
            ReviewError.NetworkUnavailable
        }

        else -> {
            ReviewError.Unexpected
        }
    }
}

private const val ALREADY_RATED = "reviews_service_id_author_id_key"
private const val ROW_LEVEL_SECURITY = "row-level security"

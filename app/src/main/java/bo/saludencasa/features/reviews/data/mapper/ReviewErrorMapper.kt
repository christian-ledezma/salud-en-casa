package bo.saludencasa.features.reviews.data.mapper

import bo.saludencasa.features.reviews.domain.model.ReviewError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

internal fun Throwable.toReviewError(): ReviewError =
    when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> {
            ReviewError.NetworkUnavailable
        }

        else -> {
            ReviewError.Unexpected
        }
    }

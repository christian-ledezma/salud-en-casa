package bo.saludencasa.features.search.data.mapper

import bo.saludencasa.features.search.domain.model.SearchError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

internal fun Throwable.toSearchError(): SearchError =
    when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> {
            SearchError.NetworkUnavailable
        }

        else -> {
            SearchError.Unexpected
        }
    }

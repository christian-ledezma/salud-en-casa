package bo.saludencasa.features.location.data.mapper

import bo.saludencasa.features.location.domain.model.AddressError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

internal fun Throwable.toAddressError(): AddressError =
    when (this) {
        // The platform geocoder reports "no network" as a plain IOException,
        // and it is the failure a person on a bus hits most often.
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> AddressError.NetworkUnavailable

        is SecurityException -> AddressError.LocationPermissionDenied

        else -> AddressError.Unexpected
    }

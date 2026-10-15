package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.domain.model.RequestError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

// The creation functions raise their refusals with a bare label, and
// supabase-kt carries the response body as the exception message: matching the
// label is the only hold there is on it, the same way the catalog reads a unique
// violation.
internal fun Throwable.toRequestError(): RequestError {
    message?.let { body ->
        when {
            body.contains(NOT_SIGNED_IN) -> return RequestError.NotSignedIn
            body.contains(ADDRESS_NOT_OWNED) -> return RequestError.AddressNotOwned
            body.contains(SERVICE_TYPE_UNAVAILABLE) -> return RequestError.ServiceTypeUnavailable
            body.contains(DESCRIPTION_TOO_LONG) -> return RequestError.InvalidDescription
            body.contains(CANNOT_SCHEDULE_WITH_YOURSELF) -> return RequestError.CannotScheduleWithYourself
            body.contains(PROFESSIONAL_UNAVAILABLE) -> return RequestError.ProfessionalUnavailable
            body.contains(SERVICE_NOT_OFFERED) -> return RequestError.ServiceNotOffered
            body.contains(OUTSIDE_COVERAGE) -> return RequestError.OutsideCoverage
            body.contains(TIME_NOT_BOOKABLE) -> return RequestError.TimeNotBookable
        }
    }

    return when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> RequestError.NetworkUnavailable
        else -> RequestError.Unexpected
    }
}

private const val NOT_SIGNED_IN = "not_signed_in"
private const val ADDRESS_NOT_OWNED = "address_not_owned"
private const val SERVICE_TYPE_UNAVAILABLE = "service_type_unavailable"
private const val DESCRIPTION_TOO_LONG = "description_too_long"
private const val CANNOT_SCHEDULE_WITH_YOURSELF = "cannot_schedule_with_yourself"
private const val PROFESSIONAL_UNAVAILABLE = "professional_unavailable"
private const val SERVICE_NOT_OFFERED = "service_not_offered"
private const val OUTSIDE_COVERAGE = "outside_coverage"
private const val TIME_NOT_BOOKABLE = "time_not_bookable"

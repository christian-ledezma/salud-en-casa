package bo.saludencasa.features.catalog.data.mapper

import bo.saludencasa.features.catalog.domain.model.CatalogError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

internal fun Throwable.toCatalogError(): CatalogError {
    // The unique index on (professional_id, service_type_id) is what guarantees
    // HU-10's third criterion. PostgREST answers a unique violation with the
    // constraint name inside the response body, and supabase-kt carries that
    // body as the exception message: matching the name is the only hold there
    // is on it. The use case refuses the duplicate before the write, so what
    // reaches here is the race between its read and this one.
    if (message?.contains(DECLARED_SERVICE_IS_UNIQUE) == true) return CatalogError.ServiceAlreadyDeclared

    return when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> CatalogError.NetworkUnavailable
        else -> CatalogError.Unexpected
    }
}

private const val DECLARED_SERVICE_IS_UNIQUE = "professional_services_professional_id_service_type_id_key"

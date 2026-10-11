package bo.saludencasa.features.catalog.data.mapper

import bo.saludencasa.features.catalog.domain.model.CatalogError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class CatalogErrorMapperTest {
    // The unique index on (professional_id, service_type_id) is what guarantees
    // HU-10's third criterion, and PostgREST names it in the body of the 409.
    // The use case refuses the duplicate first, so this path is the race
    // between its read and the insert; falling through to Unexpected would tell
    // the professional "something failed" when the answer is "you already offer
    // that one".
    @Test
    fun theUniqueViolationOfADeclaredServiceBecomesItsOwnError() {
        val raised =
            RuntimeException(
                "duplicate key value violates unique constraint " +
                    "\"professional_services_professional_id_service_type_id_key\"",
            )

        assertEquals(CatalogError.ServiceAlreadyDeclared, raised.toCatalogError())
    }

    // The price check of the same table is a different refusal and must not be
    // mistaken for the duplicate: the value object refuses it before the write,
    // so one reaching here is a defect and reads as unexpected.
    @Test
    fun thePriceCheckOfTheSameTableIsNotTheDuplicateError() {
        val raised =
            RuntimeException(
                "new row violates check constraint \"professional_services_reference_price_bob_check\"",
            )

        assertEquals(CatalogError.Unexpected, raised.toCatalogError())
    }

    @Test
    fun timeoutTransportAndIoExceptionsAllMapToNetworkUnavailable() {
        val timeout =
            HttpRequestTimeoutException("https://example.supabase.co/rest/v1/professional_services", 10_000L)
        val request =
            HttpRequestBuilder().apply { url("https://example.supabase.co/rest/v1/professional_services") }

        assertEquals(CatalogError.NetworkUnavailable, timeout.toCatalogError())
        assertEquals(CatalogError.NetworkUnavailable, HttpRequestException("reset", request).toCatalogError())
        assertEquals(CatalogError.NetworkUnavailable, IOException("boom").toCatalogError())
    }

    @Test
    fun anythingElseIsMappedToUnexpected() {
        assertEquals(CatalogError.Unexpected, IllegalStateException("boom").toCatalogError())
    }
}

package bo.saludencasa.features.search.data.mapper

import bo.saludencasa.features.search.domain.model.SearchError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class SearchErrorMapperTest {
    // The search is the first screen a patient opens, often on mobile data.
    // "No connection" sends them to fix their network; "something failed"
    // sends them nowhere.
    @Test
    fun timeoutTransportAndIoExceptionsAllMapToNetworkUnavailable() {
        val url = "https://example.supabase.co/rest/v1/rpc/search_nearby_professionals"
        val request = HttpRequestBuilder().apply { url(url) }

        assertEquals(SearchError.NetworkUnavailable, HttpRequestTimeoutException(url, 10_000L).toSearchError())
        assertEquals(SearchError.NetworkUnavailable, HttpRequestException("reset", request).toSearchError())
        assertEquals(SearchError.NetworkUnavailable, IOException("boom").toSearchError())
    }

    @Test
    fun anythingElseIsMappedToUnexpected() {
        assertEquals(SearchError.Unexpected, IllegalStateException("boom").toSearchError())
    }
}

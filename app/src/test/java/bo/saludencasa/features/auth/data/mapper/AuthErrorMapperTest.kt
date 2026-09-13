package bo.saludencasa.features.auth.data.mapper

import bo.saludencasa.features.auth.domain.model.AuthError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMapperTest {
    // supabase-kt rethrows a timed out request untouched and wraps only the
    // other transport failures in its own exception, so a timeout reaches this
    // mapper as a Ktor type. Reading it as an unexpected failure tells somebody
    // on a weak connection that something inexplicable happened, and it also
    // skips the offline clean up the sign out depends on.
    @Test
    fun `reports a request that timed out as a network failure`() {
        val timedOut = HttpRequestTimeoutException("https://example.supabase.co/auth/v1/token", 10_000L)

        assertEquals(AuthError.NetworkUnavailable, timedOut.toAuthError())
    }

    @Test
    fun `reports a transport failure as a network failure`() {
        val request = HttpRequestBuilder().apply { url("https://example.supabase.co/auth/v1/token") }
        val unreachable = HttpRequestException("connection reset", request)

        assertEquals(AuthError.NetworkUnavailable, unreachable.toAuthError())
    }

    @Test
    fun `reports anything it does not recognise as unexpected`() {
        assertEquals(AuthError.Unexpected, IllegalStateException("boom").toAuthError())
    }
}

package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.features.profile.domain.model.ProfileError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileErrorMapperTest {
    // Same trap as the sign in mapper: supabase-kt rethrows a timed out request
    // untouched and wraps only the other transport failures. A start up that
    // reads a timeout as unexpected offers no useful advice to somebody whose
    // connection is merely slow (docs/decisions.md, 2026-09-13).
    @Test
    fun `reports a request that timed out as a network failure`() {
        val timedOut = HttpRequestTimeoutException("https://example.supabase.co/rest/v1/profiles", 10_000L)

        assertEquals(ProfileError.NetworkUnavailable, timedOut.toProfileError())
    }

    @Test
    fun `reports a transport failure as a network failure`() {
        val request = HttpRequestBuilder().apply { url("https://example.supabase.co/rest/v1/profiles") }
        val unreachable = HttpRequestException("connection reset", request)

        assertEquals(ProfileError.NetworkUnavailable, unreachable.toProfileError())
    }

    @Test
    fun `reports anything it does not recognise as unexpected`() {
        assertEquals(ProfileError.Unexpected, IllegalStateException("boom").toProfileError())
    }
}

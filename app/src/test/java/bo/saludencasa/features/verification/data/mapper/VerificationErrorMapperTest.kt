package bo.saludencasa.features.verification.data.mapper

import bo.saludencasa.features.verification.domain.model.VerificationError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class VerificationErrorMapperTest {
    // The guard_document_review trigger raises this when the owner edits a
    // reviewed row. The mapper surfaces it as the frozen-by-review error so
    // the screen says exactly that, rather than a generic unexpected failure.
    @Test
    fun `trigger refusal becomes the frozen-by-review error`() {
        val raised =
            RuntimeException("Error running query: document_review_is_written_by_an_administrator")

        val error = raised.toVerificationError()

        assertEquals(VerificationError.DocumentFrozenByReview, error)
    }

    // supabase-kt rethrows timeouts unwrapped on top of its own transport
    // exception, so both have to map to network-unavailable or a user on a
    // bus never sees the retry button.
    @Test
    fun `timeout, transport and io exceptions all map to network unavailable`() {
        val timeout = HttpRequestTimeoutException("https://example.supabase.co/storage/v1/object/upload", 10_000L)
        val request = HttpRequestBuilder().apply { url("https://example.supabase.co/storage/v1/object/upload") }
        val transport = HttpRequestException("connection reset", request)
        val io = IOException("boom")

        assertEquals(VerificationError.NetworkUnavailable, timeout.toVerificationError())
        assertEquals(VerificationError.NetworkUnavailable, transport.toVerificationError())
        assertEquals(VerificationError.NetworkUnavailable, io.toVerificationError())
    }

    @Test
    fun `anything else is mapped to Unexpected`() {
        assertEquals(VerificationError.Unexpected, IllegalStateException("boom").toVerificationError())
    }

    // approve_professional_verification raises these as stable keys. Falling
    // through to Unexpected would show the administrator "something failed"
    // when the reason is a missing document they can act on.
    @Test
    fun theEngineRefusalsMapToTheirOwnErrors() {
        assertEquals(
            VerificationError.NotAuthorized,
            RuntimeException("P0001: not_authorized").toVerificationError(),
        )
        assertEquals(
            VerificationError.RequiredDocumentsNotApproved,
            RuntimeException("P0001: required_documents_not_approved").toVerificationError(),
        )
        assertEquals(
            VerificationError.ProfessionalProfileIncomplete,
            RuntimeException("P0001: professional_profile_incomplete").toVerificationError(),
        )
    }
}

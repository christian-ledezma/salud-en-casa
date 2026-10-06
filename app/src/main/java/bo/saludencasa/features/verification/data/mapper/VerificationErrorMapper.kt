package bo.saludencasa.features.verification.data.mapper

import bo.saludencasa.features.verification.domain.model.VerificationError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

internal fun Throwable.toVerificationError(): VerificationError {
    // The guard_document_review trigger raises this when a reviewed document
    // is edited by its owner. PostgREST carries the plpgsql raise text as the
    // message; matching the name is the only hold there is on it.
    if (message?.contains(DOCUMENT_REVIEW_BLOCK) == true) return VerificationError.DocumentFrozenByReview

    return when (this) {
        // The platform compressor reports a decode failure as a plain
        // IOException, same text as "no network" but a different cause. The
        // caller distinguishes them before this mapper runs; everything that
        // reaches here is a transport failure.
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> VerificationError.NetworkUnavailable

        else -> VerificationError.Unexpected
    }
}

private const val DOCUMENT_REVIEW_BLOCK = "document_review_is_written_by_an_administrator"

package bo.saludencasa.features.verification.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentImageTest {
    @Test
    fun `rejects an empty byte array`() {
        val result = DocumentImage.create(ByteArray(0))

        assertTrue(result.isFailure)
        assertEquals("document_image_empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `rejects a payload larger than two MiB`() {
        val result = DocumentImage.create(ByteArray(DocumentImage.MAX_BYTES + 1))

        assertTrue(result.isFailure)
        assertEquals("document_image_too_large", result.exceptionOrNull()?.message)
    }

    // The upper bound is the ceiling, not a value above it. The storage bucket
    // has the same limit, so a payload sitting exactly at it is still valid.
    @Test
    fun `accepts a payload at the two MiB boundary`() {
        val result = DocumentImage.create(ByteArray(DocumentImage.MAX_BYTES))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `accepts a small payload`() {
        val result = DocumentImage.create(ByteArray(16))

        assertTrue(result.isSuccess)
    }
}

package bo.saludencasa.features.verification.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentCaptionTest {
    @Test
    fun `trims surrounding whitespace`() {
        val caption = DocumentCaption.create("  Carnet del colegio  ").getOrThrow()

        assertEquals("Carnet del colegio", caption.value)
    }

    @Test
    fun `rejects a caption that is only whitespace`() {
        val result = DocumentCaption.create("   ")

        assertTrue(result.isFailure)
        assertEquals("document_caption_out_of_range", result.exceptionOrNull()?.message)
    }

    @Test
    fun `rejects a caption longer than 120 characters`() {
        val result = DocumentCaption.create("a".repeat(121))

        assertTrue(result.isFailure)
    }

    @Test
    fun `accepts a 120 character caption at the boundary`() {
        val result = DocumentCaption.create("a".repeat(120))

        assertTrue(result.isSuccess)
    }
}

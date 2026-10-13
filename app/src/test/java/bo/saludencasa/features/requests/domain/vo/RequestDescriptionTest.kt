package bo.saludencasa.features.requests.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestDescriptionTest {
    @Test
    fun rejectsBlankDescription() {
        assertTrue(RequestDescription.create("   ").isFailure)
    }

    @Test
    fun acceptsADescriptionAtTheUpperBound() {
        val atLimit = "a".repeat(1000)
        assertTrue(RequestDescription.create(atLimit).isSuccess)
    }

    @Test
    fun rejectsADescriptionOverTheUpperBound() {
        val overLimit = "a".repeat(1001)
        assertTrue(RequestDescription.create(overLimit).isFailure)
    }

    // The bound is on the trimmed value: surrounding spaces must not let a
    // too-long description slip past, nor push a borderline one over.
    @Test
    fun measuresTheBoundAfterTrimming() {
        val padded = "  " + "a".repeat(1000) + "  "
        val result = RequestDescription.create(padded)
        assertTrue(result.isSuccess)
        assertEquals(1000, result.getOrThrow().value.length)
    }

    @Test
    fun carriesAKeyNotAUserFacingSentence() {
        val message = RequestDescription.create("").exceptionOrNull()?.message
        assertFalse(message.orEmpty().contains(" "))
    }
}

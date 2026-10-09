package bo.saludencasa.features.verification.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RejectionReasonTest {
    @Test
    fun rejectsEmptyReason() {
        assertTrue(RejectionReason.create("").isFailure)
    }

    @Test
    fun rejectsWhitespaceOnlyReason() {
        assertTrue(RejectionReason.create("   ").isFailure)
    }

    @Test
    fun rejectsReasonLongerThanTheColumnAllows() {
        assertTrue(RejectionReason.create("x".repeat(301)).isFailure)
    }

    @Test
    fun acceptsReasonAtBothBounds() {
        assertTrue(RejectionReason.create("x").isSuccess)
        assertTrue(RejectionReason.create("x".repeat(300)).isSuccess)
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("Foto borrosa", RejectionReason.create("  Foto borrosa \n").getOrThrow().value)
    }

    // The bound is on what is stored, so padding must not count against it.
    @Test
    fun theBoundCountsTheTrimmedText() {
        assertTrue(RejectionReason.create(" ".repeat(10) + "x".repeat(300)).isSuccess)
    }
}

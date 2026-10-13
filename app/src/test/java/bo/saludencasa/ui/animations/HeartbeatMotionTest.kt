package bo.saludencasa.ui.animations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HeartbeatMotionTest {
    // The startup screen leaves the moment the intro is over. A phase that
    // outlasted it would be cut off on the last frame anybody sees: the logo
    // frozen mid-beat, the name half faded in, the trace short of the edge.
    @Test
    fun everyPhaseHasSettledWhenTheIntroEnds() {
        val end = INTRO_MILLIS.toFloat()

        assertEquals(1f, markSettle(end), 0f)
        assertEquals(1f, wordmarkReveal(end), 0f)
        assertEquals(1f, firstSweep(end).head, 0f)
        assertEquals(1f, beatScale(end - SWEEP_START_MILLIS), 0f)
    }

    // The peak is found on the trace itself, not read from the constant the
    // beat uses: moving one without the other makes the logo pulse while the
    // line under it is still flat.
    @Test
    fun theLogoBeatsAsTheTraceCrossesItsTallestPeak() {
        val peakX = heartbeatSamples.minBy { heartbeatY(it) }
        val crossingMillis = peakX * SWEEP_MILLIS

        assertEquals(1f, beatScale(crossingMillis - 1f), 0f)
        assertTrue(beatScale(crossingMillis + 60f) > 1.05f)
    }

    // The first version counted each knot of the QRS complex in the segment
    // that ends there and in the one that starts there, and drew the R peak at
    // twice its height as a one-sample spike. Every knot is a sample.
    @Test
    fun theTraceHasNoJumpAtAnyPointItIsDrawnThrough() {
        val epsilon = 0.0001f

        heartbeatSamples.forEach { x ->
            val before = abs(heartbeatY(x) - heartbeatY(x - epsilon))
            val after = abs(heartbeatY(x) - heartbeatY(x + epsilon))
            assertTrue("The trace jumps at x = $x", before < MAX_STEP && after < MAX_STEP)
        }
    }
}

// The steepest stroke of the trace, from the R peak down to S, falls about
// 0.0015 in 0.0001 of width.
private const val MAX_STEP = 0.005f

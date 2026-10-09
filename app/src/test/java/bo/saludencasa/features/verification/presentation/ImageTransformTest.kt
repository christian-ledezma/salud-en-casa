package bo.saludencasa.features.verification.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// A 1000 by 2000 pixel viewer, the shape of a phone screen.
private val SCREEN = Size(1000f, 2000f)

class ImageTransformTest {
    private fun fitted() = ImageTransform()

    @Test
    fun zoomNeverGoesBelowTheFittedSize() {
        val shrunk = fitted().transformedBy(zoom = 0.2f, pan = Offset.Zero, container = SCREEN)

        assertEquals(MIN_DOCUMENT_ZOOM, shrunk.scale, 0f)
        assertTrue(shrunk.isFitted)
    }

    @Test
    fun zoomNeverExceedsTheMaximum() {
        val stretched = fitted().transformedBy(zoom = 40f, pan = Offset.Zero, container = SCREEN)

        assertEquals(MAX_DOCUMENT_ZOOM, stretched.scale, 0f)
    }

    // The whole document is on screen, so there is nothing to uncover by
    // dragging. Letting it move would push the image into the void and the only
    // way back would be closing the viewer.
    @Test
    fun theImageDoesNotMoveWhileItFitsTheScreen() {
        val dragged = fitted().transformedBy(zoom = 1f, pan = Offset(400f, -900f), container = SCREEN)

        assertEquals(Offset.Zero, dragged.offset)
    }

    // At twice the size the content is 2000 by 4000, so half of the 1000 by 2000
    // surplus is the furthest the centre can travel: 500 and 1000.
    @Test
    fun theDragStopsAtTheEdgeOfTheEnlargedImage() {
        val zoomed = fitted().transformedBy(zoom = 2f, pan = Offset.Zero, container = SCREEN)

        val dragged = zoomed.transformedBy(zoom = 1f, pan = Offset(5_000f, -5_000f), container = SCREEN)

        assertEquals(500f, dragged.offset.x, 0f)
        assertEquals(-1_000f, dragged.offset.y, 0f)
    }

    @Test
    fun aDragInsideTheEdgeIsFollowedExactly() {
        val zoomed = fitted().transformedBy(zoom = 2f, pan = Offset.Zero, container = SCREEN)

        val dragged = zoomed.transformedBy(zoom = 1f, pan = Offset(120f, -40f), container = SCREEN)

        assertEquals(Offset(120f, -40f), dragged.offset)
    }

    // Pinching back out with the image pushed into a corner has to bring it
    // home, or the viewer reopens on a blank screen the next time.
    @Test
    fun zoomingBackOutRecentresTheImage() {
        val cornered =
            fitted()
                .transformedBy(zoom = 3f, pan = Offset.Zero, container = SCREEN)
                .transformedBy(zoom = 1f, pan = Offset(5_000f, 5_000f), container = SCREEN)
        assertTrue(cornered.offset.x > 0f)

        val back = cornered.transformedBy(zoom = 0.1f, pan = Offset.Zero, container = SCREEN)

        assertEquals(MIN_DOCUMENT_ZOOM, back.scale, 0f)
        assertEquals(Offset.Zero, back.offset)
    }

    // The point under the fingers has to stay put, which means the offset grows
    // with the scale. Ignoring that makes the image slide toward the centre on
    // every pinch and the zoom feel like it fights the hand.
    @Test
    fun theOffsetGrowsWithTheZoomSoTheHeldPointStaysPut() {
        val zoomed =
            fitted()
                .transformedBy(zoom = 2f, pan = Offset.Zero, container = SCREEN)
                .transformedBy(zoom = 1f, pan = Offset(100f, 0f), container = SCREEN)

        val further = zoomed.transformedBy(zoom = 2f, pan = Offset.Zero, container = SCREEN)

        assertEquals(4f, further.scale, 0f)
        assertEquals(200f, further.offset.x, 0f)
    }
}

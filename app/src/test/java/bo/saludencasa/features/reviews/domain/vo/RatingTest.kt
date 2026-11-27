package bo.saludencasa.features.reviews.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingTest {
    private fun keyOf(raw: Int) = Rating.create(raw).exceptionOrNull()?.message

    @Test
    fun rejectsRatingBelowOne() {
        assertEquals("rating_out_of_range", keyOf(0))
        assertEquals("rating_out_of_range", keyOf(-1))
    }

    @Test
    fun rejectsRatingAboveFive() {
        assertEquals("rating_out_of_range", keyOf(6))
    }

    @Test
    fun acceptsRatingAtBothBounds() {
        assertEquals(1, Rating.create(1).getOrThrow().value)
        assertEquals(5, Rating.create(5).getOrThrow().value)
    }

    @Test
    fun acceptsEveryRatingBetweenTheBounds() {
        assertTrue((Rating.MIN..Rating.MAX).all { Rating.create(it).isSuccess })
    }
}

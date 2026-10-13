package bo.saludencasa.features.reviews.data.mapper

import bo.saludencasa.features.reviews.data.model.ReviewDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ReviewMapperTest {
    @Test
    fun `reads the instant PostgREST sends, offset included`() {
        val review = dto(createdAt = "2026-09-01T12:00:00+00:00").toProfessionalReview()

        assertEquals(Instant.parse("2026-09-01T12:00:00Z"), review?.createdAt)
    }

    // A row without a usable date fails its whole page rather than vanishing: a
    // short page is the only signal the list ended, so a dropped row would stop
    // the pagination one page early (docs/decisions.md, 2026-10-06).
    @Test
    fun `a row with a date this version cannot read is not a review`() {
        assertNull(dto(createdAt = "ayer").toProfessionalReview())
    }

    // A rating with no words is drawn as the stars alone. Blank text would
    // leave an empty line that reads as a defect.
    @Test
    fun `a comment of only whitespace is no comment at all`() {
        // The review itself is asserted first: with only the safe call, a
        // mapping that returned null for any other reason would satisfy this
        // without the blank comment ever being read.
        val review = dto(comment = "   ").toProfessionalReview()

        assertNotNull(review)
        assertNull(review?.comment)
    }

    @Test
    fun `keeps the comment that was written`() {
        assertEquals("Muy puntual.", dto(comment = "Muy puntual.").toProfessionalReview()?.comment)
    }
}

private fun dto(
    id: String = "review-1",
    rating: Int = 5,
    comment: String? = "Muy puntual.",
    createdAt: String = "2026-09-01T12:00:00+00:00",
): ReviewDto = ReviewDto(id = id, rating = rating, comment = comment, createdAt = createdAt)

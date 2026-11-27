package bo.saludencasa.features.reviews.domain.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewCommentTest {
    private fun keyOf(raw: String) = ReviewComment.create(raw).exceptionOrNull()?.message

    @Test
    fun rejectsAnEmptyComment() {
        assertEquals("review_comment_empty", keyOf(""))
    }

    @Test
    fun rejectsACommentOfOnlySpaces() {
        assertEquals("review_comment_empty", keyOf(" \n\t "))
    }

    @Test
    fun acceptsExactlyTheMaximum() {
        assertTrue(ReviewComment.create("x".repeat(ReviewComment.MAX_LENGTH)).isSuccess)
    }

    @Test
    fun rejectsOneCharacterOverTheMaximum() {
        assertEquals("review_comment_too_long", keyOf("x".repeat(ReviewComment.MAX_LENGTH + 1)))
    }

    @Test
    fun spacesAroundTheCommentDoNotCountAndAreNotSent() {
        val raw = "  " + "x".repeat(ReviewComment.MAX_LENGTH) + "\n"

        assertEquals("x".repeat(ReviewComment.MAX_LENGTH), ReviewComment.create(raw).getOrThrow().value)
    }

    // The engine checks char_length, which counts an emoji once; String.length
    // counts it twice, so a comment of 600 emoji is within the limit and 1200 units.
    @Test
    fun countsCodePointsTheWayTheEngineDoes() {
        val emoji = "😀"

        assertTrue(ReviewComment.create(emoji.repeat(ReviewComment.MAX_LENGTH)).isSuccess)
        assertEquals("review_comment_too_long", keyOf(emoji.repeat(ReviewComment.MAX_LENGTH + 1)))
    }

    // The bound is the engine's own, written out here and not read from the constant
    // under test: a limit raised on the phone alone would pass every comment up to a
    // refusal of the insert.
    @Test
    fun theLimitIsTheOneThatTheCheckOnTheColumnEnforces() {
        assertTrue(ReviewComment.create("x".repeat(1000)).isSuccess)
        assertEquals("review_comment_too_long", keyOf("x".repeat(1001)))
    }
}

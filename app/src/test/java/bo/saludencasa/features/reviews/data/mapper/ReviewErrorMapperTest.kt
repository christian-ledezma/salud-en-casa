package bo.saludencasa.features.reviews.data.mapper

import bo.saludencasa.features.reviews.domain.model.ReviewError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ReviewErrorMapperTest {
    // The unique key on (service_id, author_id): two taps, or two phones.
    @Test
    fun aSecondRatingOfTheSameAttentionSaysItWasAlreadyRated() {
        val duplicate =
            Exception(
                "duplicate key value violates unique constraint \"reviews_service_id_author_id_key\"",
            )

        assertEquals(ReviewError.AlreadyRated, duplicate.toReviewError())
    }

    @Test
    fun theInsertPolicyRefusalSaysTheAttentionCannotBeRated() {
        val refused = Exception("new row violates row-level security policy for table \"reviews\"")

        assertEquals(ReviewError.NotAllowed, refused.toReviewError())
    }

    @Test
    fun aDroppedConnectionIsNetworkUnavailable() {
        assertEquals(ReviewError.NetworkUnavailable, IOException("timeout").toReviewError())
    }

    @Test
    fun anythingElseIsUnexpected() {
        assertEquals(ReviewError.Unexpected, Exception("something else").toReviewError())
    }
}

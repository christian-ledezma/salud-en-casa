package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.domain.model.OfferError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

// supabase-kt carries the response body as the exception message, so these are
// the bodies the engine actually answers with.
class OfferErrorMapperTest {
    private fun refusal(body: String): OfferError = IllegalStateException(body).toOfferError()

    // HT-24: the floor the engine enforces arrives as its own error, so the screen
    // can say what happened instead of a generic refusal.
    @Test
    fun theFloorLabelIsBelowMinimum() {
        assertEquals(
            OfferError.BelowMinimum(null),
            refusal("""{"code":"P0001","message":"counter_offer_below_your_last_offer"}"""),
        )
    }

    // Two answers to the same offer, from two taps or two phones: the second
    // loses to the unique index and reads as a refusal, not as "unexpected".
    @Test
    fun aSecondAnswerToTheSameOfferIsARefusal() {
        assertEquals(
            OfferError.NotAllowed,
            refusal(
                """{"code":"23505","message":"duplicate key value violates unique constraint """ +
                    """\"idx_request_offers_one_answer_per_parent\""}""",
            ),
        )
    }

    @Test
    fun aSecondFirstOfferIsARefusal() {
        assertEquals(
            OfferError.NotAllowed,
            refusal(
                """{"code":"23505","message":"duplicate key value violates unique constraint """ +
                    """\"idx_request_offers_one_first_offer\""}""",
            ),
        )
    }

    @Test
    fun theRowLevelSecurityRefusalIsNotAllowed() {
        assertEquals(
            OfferError.NotAllowed,
            refusal("""new row violates row-level security policy for table "request_offers""""),
        )
    }

    @Test
    fun aDroppedConnectionIsTheNetwork() {
        assertEquals(OfferError.NetworkUnavailable, IOException("reset").toOfferError())
    }
}

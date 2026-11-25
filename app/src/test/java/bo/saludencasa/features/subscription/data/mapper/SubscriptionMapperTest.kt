package bo.saludencasa.features.subscription.data.mapper

import bo.saludencasa.features.subscription.data.model.SubscriptionDto
import bo.saludencasa.features.subscription.domain.model.Subscription
import bo.saludencasa.features.subscription.domain.model.SubscriptionState
import bo.saludencasa.features.subscription.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class SubscriptionMapperTest {
    // PostgREST writes the offset as +00:00, which Instant.parse refuses.
    @Test
    fun aTrialReadsItsEndWithThePostgrestOffset() {
        val state =
            SubscriptionDto(required = true, status = "TRIAL", endsAt = "2026-12-24T16:00:00.123456+00:00")
                .toSubscriptionState()

        assertEquals(
            SubscriptionState(
                required = true,
                subscription = Subscription(SubscriptionStatus.TRIAL, Instant.parse("2026-12-24T16:00:00.123456Z")),
            ),
            state,
        )
    }

    @Test
    fun noStatusIsNoPeriod() {
        val state = SubscriptionDto(required = true, status = null, endsAt = null).toSubscriptionState()

        assertEquals(SubscriptionState(required = true, subscription = null), state)
    }

    // PLAY_BILLING arrives in Sprint 11. An older version that read an unknown
    // status as "no period" would hide the row, and with it an expiry.
    @Test
    fun aStatusThisVersionDoesNotKnowIsUnreadable() {
        assertNull(
            SubscriptionDto(required = true, status = "PLAY_BILLING", endsAt = "2026-12-24T16:00:00+00:00")
                .toSubscriptionState(),
        )
    }

    @Test
    fun aPeriodWithoutAUsableEndIsUnreadable() {
        assertNull(SubscriptionDto(required = true, status = "EXPIRED", endsAt = "yesterday").toSubscriptionState())
    }
}

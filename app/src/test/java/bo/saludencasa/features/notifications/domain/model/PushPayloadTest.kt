package bo.saludencasa.features.notifications.domain.model

import bo.saludencasa.features.notifications.APPOINTMENT_AT
import bo.saludencasa.features.notifications.PROFESSIONAL_ID
import bo.saludencasa.features.notifications.REQUEST_ID
import bo.saludencasa.features.notifications.SIGNED_IN_USER
import bo.saludencasa.features.notifications.everyPayload
import bo.saludencasa.features.profile.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PushPayloadTest {
    private fun wire(vararg overrides: Pair<String, String?>): Map<String, String> {
        val base =
            mutableMapOf(
                "event" to "REQUEST_NEARBY",
                "requestId" to REQUEST_ID,
                "recipientId" to SIGNED_IN_USER,
                "recipientRole" to "PROFESSIONAL",
                "serviceTypeName" to "Inyectables",
                "distanceM" to "1200",
            )
        overrides.forEach { (key, value) -> if (value == null) base.remove(key) else base[key] = value }
        return base
    }

    private fun failureKey(data: Map<String, String>): String? = PushPayload.parse(data).exceptionOrNull()?.message

    // What request_push_body writes is what the phone must read.
    @Test
    fun parsesANearbyRequestWithItsDistance() {
        assertEquals(1200, (PushPayload.parse(wire()).getOrThrow() as PushPayload.NearbyRequest).distanceM)
    }

    @Test
    fun parsesAnAppointmentWithItsDate() {
        val scheduled =
            PushPayload
                .parse(
                    wire(
                        "event" to "APPOINTMENT_SCHEDULED",
                        "distanceM" to null,
                        "scheduledAt" to "2026-10-20T14:00:00Z",
                    ),
                ).getOrThrow()

        assertEquals(APPOINTMENT_AT, (scheduled as PushPayload.AppointmentScheduled).scheduledAt)
    }

    @Test
    fun parsesAnExpiryForThePatientRole() {
        val expired =
            PushPayload
                .parse(wire("event" to "REQUEST_EXPIRED", "distanceM" to null, "recipientRole" to "PATIENT"))
                .getOrThrow()

        assertEquals(UserRole.PATIENT, expired.subject.recipientRole)
    }

    @Test
    fun parsesAnOfferWithItsAmountForThePatientRole() {
        val offer =
            PushPayload
                .parse(
                    wire(
                        "event" to "OFFER_RECEIVED",
                        "distanceM" to null,
                        "recipientRole" to "PATIENT",
                        "amountBob" to "160.00",
                        "professionalId" to PROFESSIONAL_ID,
                    ),
                ).getOrThrow()

        offer as PushPayload.OfferReceived
        assertEquals(UserRole.PATIENT, offer.subject.recipientRole)
        assertEquals(0, offer.amountBob.compareTo(BigDecimal("160.00")))
        assertEquals(PROFESSIONAL_ID, offer.professionalId)
    }

    // HT-24: the tap opens the thread of the professional who offered, so an
    // offer without them has nowhere to go.
    @Test
    fun rejectsAnOfferWithoutItsProfessional() {
        assertEquals(
            "push_payload_invalid_professional",
            failureKey(
                wire(
                    "event" to "OFFER_RECEIVED",
                    "distanceM" to null,
                    "recipientRole" to "PATIENT",
                    "amountBob" to "160.00",
                ),
            ),
        )
    }

    @Test
    fun rejectsAnOfferWithoutItsAmount() {
        assertEquals(
            "push_payload_invalid_amount",
            failureKey(wire("event" to "OFFER_RECEIVED", "distanceM" to null, "recipientRole" to "PATIENT")),
        )
    }

    @Test
    fun rejectsAnOfferAmountThatIsNotPositive() {
        assertEquals(
            "push_payload_invalid_amount",
            failureKey(
                wire(
                    "event" to "OFFER_RECEIVED",
                    "distanceM" to null,
                    "recipientRole" to "PATIENT",
                    "amountBob" to "0",
                ),
            ),
        )
    }

    @Test
    fun rejectsAnOfferAmountThatIsNotANumber() {
        assertEquals(
            "push_payload_invalid_amount",
            failureKey(
                wire(
                    "event" to "OFFER_RECEIVED",
                    "distanceM" to null,
                    "recipientRole" to "PATIENT",
                    "amountBob" to "mucho",
                ),
            ),
        )
    }

    private fun counterWire(vararg overrides: Pair<String, String?>): Map<String, String> =
        wire(
            "event" to "COUNTER_OFFER_RECEIVED",
            "distanceM" to null,
            "recipientRole" to "PATIENT",
            "amountBob" to "140.00",
            "professionalId" to PROFESSIONAL_ID,
            *overrides,
        )

    // The one event that reaches either role: the same wire shape parses for a
    // patient and for a professional, because the role is a field of the subject
    // and not a property of the event.
    @Test
    fun parsesACounterOfferForEitherRole() {
        listOf(UserRole.PATIENT, UserRole.PROFESSIONAL).forEach { role ->
            val counter =
                PushPayload.parse(counterWire("recipientRole" to role.name)).getOrThrow()

            counter as PushPayload.CounterOfferReceived
            assertEquals(role, counter.subject.recipientRole)
            assertEquals(0, counter.amountBob.compareTo(BigDecimal("140.00")))
            assertEquals(PROFESSIONAL_ID, counter.professionalId)
        }
    }

    // Without the thread's professional the tap has nothing to open.
    @Test
    fun rejectsACounterOfferWithoutItsProfessional() {
        assertEquals(
            "push_payload_invalid_professional",
            failureKey(counterWire("professionalId" to null)),
        )
    }

    @Test
    fun rejectsACounterOfferWhoseProfessionalIsNotAnIdentifier() {
        assertEquals(
            "push_payload_invalid_professional",
            failureKey(counterWire("professionalId" to "profesional-1")),
        )
    }

    @Test
    fun rejectsACounterOfferWithoutItsAmount() {
        assertEquals("push_payload_invalid_amount", failureKey(counterWire("amountBob" to null)))
    }

    @Test
    fun rejectsACounterOfferAmountThatIsNotPositive() {
        assertEquals("push_payload_invalid_amount", failureKey(counterWire("amountBob" to "-10")))
    }

    // An accepted offer reaches its issuer with the amount and no professional:
    // the attention opens by request.
    @Test
    fun parsesAnAcceptedOfferWithItsAmount() {
        val accepted =
            PushPayload
                .parse(
                    wire(
                        "event" to "OFFER_ACCEPTED",
                        "distanceM" to null,
                        "amountBob" to "160.00",
                    ),
                ).getOrThrow()

        accepted as PushPayload.OfferAccepted
        assertEquals(0, accepted.amountBob.compareTo(BigDecimal("160.00")))
    }

    @Test
    fun rejectsAnAcceptedOfferWithoutItsAmount() {
        assertEquals(
            "push_payload_invalid_amount",
            failureKey(wire("event" to "OFFER_ACCEPTED", "distanceM" to null)),
        )
    }

    @Test
    fun rejectsAnUnknownEvent() {
        assertEquals("push_payload_unknown_event", failureKey(wire("event" to "MESSAGE_RECEIVED")))
    }

    @Test
    fun rejectsANearbyRequestWithoutDistance() {
        assertEquals("push_payload_invalid_distance", failureKey(wire("distanceM" to null)))
    }

    @Test
    fun rejectsANegativeDistance() {
        assertEquals("push_payload_invalid_distance", failureKey(wire("distanceM" to "-1")))
    }

    @Test
    fun acceptsAZeroDistance() {
        assertEquals(
            0,
            (PushPayload.parse(wire("distanceM" to "0")).getOrThrow() as PushPayload.NearbyRequest).distanceM,
        )
    }

    @Test
    fun rejectsAnAppointmentWithoutItsDate() {
        assertEquals(
            "push_payload_invalid_scheduled_at",
            failureKey(wire("event" to "APPOINTMENT_CANCELLED", "distanceM" to null)),
        )
    }

    // PostgreSQL's own rendering is +00:00, which Instant.parse rejects; the
    // SQL writes a Z on purpose, and this pins what the phone accepts.
    @Test
    fun rejectsADateTheServerShouldNeverWrite() {
        assertEquals(
            "push_payload_invalid_scheduled_at",
            failureKey(wire("event" to "APPOINTMENT_SCHEDULED", "scheduledAt" to "20/10/2026 14:00")),
        )
    }

    // An administrator receives nothing through this channel, and a role
    // switch towards ADMIN must never be triggered by a notification.
    @Test
    fun rejectsAdminAsTheRecipientRole() {
        assertEquals("push_payload_invalid_role", failureKey(wire("recipientRole" to "ADMIN")))
    }

    @Test
    fun rejectsARequestIdThatIsNotAnIdentifier() {
        assertEquals("push_payload_invalid_request", failureKey(wire("requestId" to "../inbox")))
    }

    @Test
    fun rejectsAMissingRecipient() {
        assertEquals("push_payload_invalid_recipient", failureKey(wire("recipientId" to null)))
    }

    @Test
    fun rejectsABlankServiceType() {
        assertEquals("push_payload_missing_service_type", failureKey(wire("serviceTypeName" to " ")))
    }

    // A server one release ahead may send keys this build does not know.
    @Test
    fun ignoresKeysItDoesNotKnow() {
        assertTrue(PushPayload.parse(wire("ttlSeconds" to "1800", "amountBob" to "90")).isSuccess)
    }

    // The tap intent carries the payload through toMap and back through parse;
    // anything lost or added on the way would open the wrong thing.
    @Test
    fun aRoundTripThroughTheMapKeepsEveryEventIntact() {
        everyPayload().forEach { payload ->
            assertEquals(payload, PushPayload.parse(payload.toMap()).getOrThrow())
        }
    }
}

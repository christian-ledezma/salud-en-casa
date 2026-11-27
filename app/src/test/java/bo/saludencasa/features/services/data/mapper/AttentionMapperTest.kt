package bo.saludencasa.features.services.data.mapper

import bo.saludencasa.features.services.data.model.AttentionDto
import bo.saludencasa.features.services.data.model.AttentionSummaryDto
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.services.domain.model.PaymentStatus
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class AttentionMapperTest {
    private fun dto(
        status: String = "ASSIGNED",
        createdAt: String = "2026-10-29T12:00:00Z",
        iAmPatient: Boolean = true,
        conversationOpen: Boolean = true,
        conversationWritableUntil: String? = null,
        cancellationReason: String? = null,
        cancelledByMe: Boolean? = null,
        cancelledAt: String? = null,
        paymentStatus: String = "PENDING",
        paymentMethod: String? = null,
        patientConfirmedAt: String? = null,
        professionalConfirmedAt: String? = null,
        disputedAt: String? = null,
        resolvedAt: String? = null,
        myRating: Int? = null,
        myReviewComment: String? = null,
        myReviewedAt: String? = null,
    ) = AttentionDto(
        serviceId = "s1",
        requestId = "r1",
        status = status,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        createdAt = createdAt,
        counterpartId = "p1",
        counterpartName = "María Quispe",
        counterpartPhotoUrl = null,
        iAmPatient = iAmPatient,
        city = "La Paz",
        conversationOpen = conversationOpen,
        conversationWritableUntil = conversationWritableUntil,
        cancellationReason = cancellationReason,
        cancelledByMe = cancelledByMe,
        cancelledAt = cancelledAt,
        paymentStatus = paymentStatus,
        paymentMethod = paymentMethod,
        patientConfirmedAt = patientConfirmedAt,
        professionalConfirmedAt = professionalConfirmedAt,
        disputedAt = disputedAt,
        resolvedAt = resolvedAt,
        myRating = myRating,
        myReviewComment = myReviewComment,
        myReviewedAt = myReviewedAt,
    )

    @Test
    fun mapsTheRowToAttentionWithTheCounterpart() {
        val attention = dto().toAttention()!!

        assertEquals("s1", attention.serviceId)
        assertEquals(ServiceStatus.ASSIGNED, attention.status)
        assertEquals(BigDecimal("150.00"), attention.finalAmountBob)
        assertEquals("p1", attention.counterpart.id)
        assertEquals("María Quispe", attention.counterpart.name)
        assertEquals(true, attention.iAmThePatient)
    }

    // A status the client does not know is a contract mismatch, not a value to
    // guess at: the row is dropped so the repository reports Unexpected.
    @Test
    fun mapsThePatientsConfirmationWithItsMethodAndTime() {
        val payment =
            dto(
                status = "COMPLETED",
                paymentStatus = "PATIENT_CONFIRMED",
                paymentMethod = "DIRECT_QR",
                patientConfirmedAt = "2026-11-23T13:40:00+00:00",
            ).toAttention()!!.payment

        assertEquals(PaymentStatus.PATIENT_CONFIRMED, payment.status)
        assertEquals(PaymentMethod.DIRECT_QR, payment.method)
        assertEquals(Instant.parse("2026-11-23T13:40:00Z"), payment.patientConfirmedAt)
        assertNull(payment.professionalConfirmedAt)
    }

    // The engine retired SETTLED in HT-30: a row that still says it is a mismatch.
    @Test
    fun rejectsAPaymentStatusTheClientDoesNotKnow() {
        assertNull(dto(paymentStatus = "SETTLED").toAttention())
    }

    @Test
    fun rejectsAPaymentMethodTheClientDoesNotKnow() {
        assertNull(dto(paymentMethod = "CARD").toAttention())
    }

    @Test
    fun rejectsAnUnknownStatus() {
        assertNull(dto(status = "ARCHIVED").toAttention())
    }

    @Test
    fun rejectsAnUnparseableCreatedAt() {
        assertNull(dto(createdAt = "not-a-timestamp").toAttention())
    }

    // What PostgREST really writes: microseconds and +00:00, not a Z.
    @Test
    fun readsTheOffsetPostgrestWrites() {
        val attention = dto(createdAt = "2026-10-29T12:00:00.123456+00:00").toAttention()

        assertEquals(Instant.parse("2026-10-29T12:00:00.123456Z"), attention?.createdAt)
    }

    @Test
    fun aCancelledRowCarriesWhyWhoAndWhen() {
        val cancellation =
            dto(
                status = "CANCELLED",
                cancellationReason = "No puedo llegar",
                cancelledByMe = false,
                cancelledAt = "2026-11-04T20:20:00.5+00:00",
            ).toAttention()?.cancellation

        assertEquals("No puedo llegar", cancellation?.reason)
        assertEquals(false, cancellation?.byMe)
        assertEquals(Instant.parse("2026-11-04T20:20:00.5Z"), cancellation?.at)
    }

    @Test
    fun aRowNotCancelledHasNoCancellation() {
        assertNull(dto().toAttention()?.cancellation)
    }

    @Test
    fun anIncompleteOrUnreadableCancellationIsAContractMismatch() {
        assertNull(
            dto(status = "CANCELLED", cancelledByMe = true, cancelledAt = "2026-11-04T20:20:00+00:00").toAttention(),
        )
        assertNull(
            dto(
                status = "CANCELLED",
                cancellationReason = "x",
                cancelledAt = "2026-11-04T20:20:00+00:00",
            ).toAttention(),
        )
        assertNull(
            dto(
                status = "CANCELLED",
                cancellationReason = "x",
                cancelledByMe = true,
                cancelledAt = "ayer",
            ).toAttention(),
        )
    }

    // docs/decisions.md, 2026-11-04, HU-24: the window is the engine's, not the phone's.
    @Test
    fun theWindowOfTheConversationIsTheOneTheEngineSends() {
        val attention =
            dto(
                status = "COMPLETED",
                conversationOpen = false,
                conversationWritableUntil = "2026-11-16T19:40:00+00:00",
            ).toAttention()

        assertEquals(false, attention?.conversationOpen)
        assertEquals(Instant.parse("2026-11-16T19:40:00Z"), attention?.conversationWritableUntil)
    }

    @Test
    fun anUnreadableWindowIsAContractMismatch() {
        assertNull(dto(conversationWritableUntil = "pronto").toAttention())
    }

    // Both parties cancelled at once: the second one finds it done, and rereads.
    @Test
    fun aReasonAlreadyWrittenMeansTheAttentionChanged() {
        val refused = IllegalStateException("cancellation_reason_is_written_once")

        assertEquals(ServiceError.AttentionChanged, refused.toServiceError())
    }

    // The payment was confirmed meanwhile, from another phone or a second tap that
    // arrived first: the screen rereads and finds it, instead of an error.
    @Test
    fun aPaymentAlreadyConfirmedMeansTheAttentionChanged() {
        val refused = IllegalStateException("patient_confirms_a_pending_payment_once")

        assertEquals(ServiceError.AttentionChanged, refused.toServiceError())
    }

    // HU-28 shows when a dispute was opened, so the screen must read that time.
    @Test
    fun aDisputeCarriesWhenItWasOpened() {
        val payment =
            dto(
                status = "COMPLETED",
                paymentStatus = "DISPUTED",
                paymentMethod = "CASH",
                patientConfirmedAt = "2026-11-23T13:40:00+00:00",
                disputedAt = "2026-11-24T09:15:00+00:00",
            ).toAttention()!!.payment

        assertEquals(PaymentStatus.DISPUTED, payment.status)
        assertEquals(Instant.parse("2026-11-24T09:15:00Z"), payment.disputedAt)
    }

    @Test
    fun anUnreadableDisputeTimeIsAContractMismatch() {
        assertNull(dto(paymentStatus = "DISPUTED", disputedAt = "ayer").toAttention())
    }

    // The professional answered meanwhile, from another phone: the screen rereads
    // and finds the answer, instead of an error.
    @Test
    fun aPaymentAlreadyAnsweredMeansTheAttentionChanged() {
        val refused = IllegalStateException("professional_answers_a_patient_confirmed_payment_once")

        assertEquals(ServiceError.AttentionChanged, refused.toServiceError())
    }

    // The professional said they were paid before the patient said anything.
    @Test
    fun aReceiptDeclaredFirstIsReadWithoutThePatientsTime() {
        val payment =
            dto(
                status = "COMPLETED",
                paymentStatus = "PROFESSIONAL_CONFIRMED",
                paymentMethod = "DIRECT_QR",
                professionalConfirmedAt = "2026-11-24T13:45:00+00:00",
            ).toAttention()!!.payment

        assertEquals(PaymentStatus.PROFESSIONAL_CONFIRMED, payment.status)
        assertEquals(PaymentMethod.DIRECT_QR, payment.method)
        assertNull(payment.patientConfirmedAt)
        assertEquals(Instant.parse("2026-11-24T13:45:00Z"), payment.professionalConfirmedAt)
    }

    @Test
    fun aResolvedDisputeCarriesWhenItWasResolved() {
        val payment =
            dto(
                status = "COMPLETED",
                paymentStatus = "RESOLVED_NOT_PAID",
                paymentMethod = "CASH",
                patientConfirmedAt = "2026-11-23T13:40:00+00:00",
                disputedAt = "2026-11-24T09:15:00+00:00",
                resolvedAt = "2026-11-24T15:20:00+00:00",
            ).toAttention()!!.payment

        assertEquals(PaymentStatus.RESOLVED_NOT_PAID, payment.status)
        assertEquals(Instant.parse("2026-11-24T15:20:00Z"), payment.resolvedAt)
    }

    @Test
    fun anUnreadableResolutionTimeIsAContractMismatch() {
        assertNull(dto(paymentStatus = "RESOLVED_PAID", resolvedAt = "ayer").toAttention())
    }

    // Both declared at once and the other arrived first: the screen rereads and
    // shows what the other said, instead of an error.
    @Test
    fun aDeclarationThatArrivedSecondMeansTheAttentionChanged() {
        listOf(
            "professional_declares_a_pending_payment_once",
            "patient_only_accepts_the_professionals_receipt",
        ).forEach { code ->
            assertEquals(code, ServiceError.AttentionChanged, IllegalStateException(code).toServiceError())
        }
    }

    // A refusal the patient cannot fix by rereading stays an error.
    @Test
    fun aRefusedMethodIsNotTakenForAChange() {
        val refused = IllegalStateException("payment_method_not_allowed")

        assertEquals(ServiceError.Unexpected, refused.toServiceError())
    }

    private fun summaryDto(
        status: String = "COMPLETED",
        scheduledAt: String? = null,
        startedAt: String? = null,
    ) = AttentionSummaryDto(
        requestId = "r1",
        status = status,
        finalAmountBob = BigDecimal("45.00"),
        serviceTypeName = "Inyectables",
        counterpartName = "María Quispe",
        createdAt = "2026-10-29T12:00:00.123456+00:00",
        scheduledAt = scheduledAt,
        startedAt = startedAt,
    )

    // The start decides the date of the card: read wrong, the card says «Acordada»
    // for an attention under way.
    @Test
    fun theStartOfTheAttentionReachesTheSummary() {
        assertEquals(
            Instant.parse("2026-11-04T14:02:00Z"),
            summaryDto(startedAt = "2026-11-04T14:02:00+00:00").toAttentionSummary()?.startedAt,
        )
    }

    @Test
    fun anUnreadableStartIsAContractMismatch() {
        assertNull(summaryDto(startedAt = "ayer").toAttentionSummary())
    }

    @Test
    fun anImmediateAttentionHasNoAppointment() {
        assertNull(summaryDto().toAttentionSummary()?.scheduledAt)
    }

    @Test
    fun aScheduledAttentionKeepsItsAppointment() {
        val summary = summaryDto(scheduledAt = "2026-11-05T14:30:00+00:00").toAttentionSummary()

        assertEquals(Instant.parse("2026-11-05T14:30:00Z"), summary?.scheduledAt)
    }

    // One unreadable row fails the page instead of showing a card with a hole.
    @Test
    fun aSummaryWithAnUnknownStatusOrADateItCannotReadIsRejected() {
        assertNull(summaryDto(status = "ARCHIVED").toAttentionSummary())
        assertNull(summaryDto(scheduledAt = "not-a-timestamp").toAttentionSummary())
    }

    // docs/decisions.md, 2026-11-04, HU-23: the engine leaves these out for a
    // professional once the attention ended, and that is a row like any other.
    @Test
    fun readsARowWithoutTheAddress() {
        val place = dto().toAttention()!!.place

        assertEquals("La Paz", place.city)
        assertNull(place.addressText)
        assertNull(place.latitude)
    }

    @Test
    fun readsTheDatesOfTheProgress() {
        val attention =
            dto()
                .copy(startedAt = "2026-11-04T14:02:00+00:00", finishedAt = "2026-11-04T15:40:00+00:00")
                .toAttention()!!

        assertEquals(Instant.parse("2026-11-04T14:02:00Z"), attention.startedAt)
        assertEquals(Instant.parse("2026-11-04T15:40:00Z"), attention.finishedAt)
    }

    @Test
    fun rejectsAnUnparseableStartDate() {
        assertNull(dto().copy(startedAt = "ayer").toAttention())
    }

    // The engine refused the jump: the screen rereads instead of showing an error.
    @Test
    fun aRefusedTransitionMeansTheAttentionChanged() {
        val terminal = IllegalStateException("service_status_is_terminal")
        val invalid = IllegalStateException("invalid_service_transition")

        assertEquals(ServiceError.AttentionChanged, terminal.toServiceError())
        assertEquals(ServiceError.AttentionChanged, invalid.toServiceError())
        assertEquals(ServiceError.Unexpected, IllegalStateException("boom").toServiceError())
    }

    // HU-29: the engine returns only what the reader rated.
    @Test
    fun aRatingTheReaderWroteComesBackWithItsStarsWordsAndDate() {
        val review =
            dto(
                status = "COMPLETED",
                myRating = 4,
                myReviewComment = "Muy puntual",
                myReviewedAt = "2026-11-26T16:00:00+00:00",
            ).toAttention()!!.myReview!!

        assertEquals(4, review.rating)
        assertEquals("Muy puntual", review.comment)
        assertEquals(Instant.parse("2026-11-26T16:00:00Z"), review.createdAt)
    }

    @Test
    fun anAttentionNotRatedHasNoReview() {
        assertNull(dto(status = "COMPLETED").toAttention()!!.myReview)
    }

    @Test
    fun aRatingWithNoWordsHasNoComment() {
        val review =
            dto(myRating = 5, myReviewComment = "   ", myReviewedAt = "2026-11-26T16:00:00+00:00")
                .toAttention()!!
                .myReview!!

        assertNull(review.comment)
    }

    // A rating that comes without its date cannot be shown, so the row is dropped
    // and the repository reports Unexpected.
    @Test
    fun aRatingWithoutADateMakesTheRowUnreadable() {
        assertNull(dto(myRating = 4).toAttention())
    }
}

package bo.saludencasa.features.services.domain.model

import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.paidInCash
import bo.saludencasa.features.services.receivedFirst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttentionTest {
    @Test
    fun thePatientConfirmsThePaymentOfACompletedAttention() {
        assertTrue(attention(status = ServiceStatus.COMPLETED, iAmThePatient = true).canConfirmPayment)
    }

    // RF-11.3 is the professional's own confirmation, a different act (HU-26).
    @Test
    fun theProfessionalNeverConfirmsThePatientsPayment() {
        assertFalse(attention(status = ServiceStatus.COMPLETED, iAmThePatient = false).canConfirmPayment)
    }

    @Test
    fun noPaymentIsConfirmedBeforeTheAttentionIsCompleted() {
        listOf(ServiceStatus.ASSIGNED, ServiceStatus.IN_PROGRESS, ServiceStatus.CANCELLED).forEach { status ->
            assertFalse(status.name, attention(status = status, iAmThePatient = true).canConfirmPayment)
        }
    }

    // docs/decisions.md, 2026-11-23, HU-25: a confirmation is final.
    @Test
    fun aConfirmedPaymentIsNotOfferedAgain() {
        val paid = attention(status = ServiceStatus.COMPLETED, iAmThePatient = true, payment = paidInCash())

        assertFalse(paid.canConfirmPayment)
    }

    // RF-11.3, RF-11.5: the professional answers what the patient declared.
    @Test
    fun theProfessionalAnswersOnceThePatientConfirmed() {
        val declared = attention(status = ServiceStatus.COMPLETED, iAmThePatient = false, payment = paidInCash())

        assertTrue(declared.canAnswerPayment)
    }

    // docs/decisions.md, 2026-11-24, HU-26: a dispute is two confirmations that
    // disagree, so there is nothing to answer until the patient has spoken.
    @Test
    fun theProfessionalCannotAnswerBeforeThePatientConfirms() {
        assertFalse(attention(status = ServiceStatus.COMPLETED, iAmThePatient = false).canAnswerPayment)
    }

    @Test
    fun thePatientNeverAnswersTheirOwnPayment() {
        val declared = attention(status = ServiceStatus.COMPLETED, iAmThePatient = true, payment = paidInCash())

        assertFalse(declared.canAnswerPayment)
    }

    @Test
    fun anAnsweredPaymentIsNotOfferedAgain() {
        listOf(PaymentStatus.BOTH_CONFIRMED, PaymentStatus.DISPUTED).forEach { status ->
            val answered =
                attention(
                    status = ServiceStatus.COMPLETED,
                    iAmThePatient = false,
                    payment = paidInCash().copy(status = status),
                )
            assertFalse(status.name, answered.canAnswerPayment)
        }
    }

    // docs/decisions.md, 2026-11-24, HU-28: the professional need not wait for
    // the patient to say they paid.
    @Test
    fun theProfessionalMayDeclareTheReceiptFirst() {
        assertTrue(attention(status = ServiceStatus.COMPLETED, iAmThePatient = false).canConfirmReceipt)
        assertFalse(attention(status = ServiceStatus.IN_PROGRESS, iAmThePatient = false).canConfirmReceipt)
        assertFalse(attention(status = ServiceStatus.COMPLETED, iAmThePatient = true).canConfirmReceipt)
    }

    // Whoever spoke first leaves the other only an answer, never a second
    // declaration of their own.
    @Test
    fun nobodyDeclaresOnceTheOtherPartyHas() {
        val patientDeclared = attention(status = ServiceStatus.COMPLETED, iAmThePatient = false, payment = paidInCash())
        val professionalDeclared =
            attention(status = ServiceStatus.COMPLETED, iAmThePatient = true, payment = receivedFirst())

        assertFalse(patientDeclared.canDeclarePayment)
        assertFalse(professionalDeclared.canDeclarePayment)
    }

    // The professional's receipt is final: the patient accepts it and nothing else.
    @Test
    fun onlyThePatientAcceptsAReceiptTheProfessionalDeclaredFirst() {
        assertTrue(
            attention(
                status = ServiceStatus.COMPLETED,
                iAmThePatient = true,
                payment = receivedFirst(),
            ).canAcceptReceipt,
        )
        assertFalse(
            attention(
                status = ServiceStatus.COMPLETED,
                iAmThePatient = false,
                payment = receivedFirst(),
            ).canAcceptReceipt,
        )
        assertFalse(
            attention(status = ServiceStatus.COMPLETED, iAmThePatient = true, payment = paidInCash()).canAcceptReceipt,
        )
        assertFalse(
            attention(status = ServiceStatus.COMPLETED, iAmThePatient = true, payment = receivedFirst())
                .copy(payment = receivedFirst().copy(status = PaymentStatus.BOTH_CONFIRMED))
                .canAcceptReceipt,
        )
        assertFalse(
            attention(status = ServiceStatus.COMPLETED, iAmThePatient = true, payment = receivedFirst())
                .canAnswerPayment,
        )
    }

    // serviceStateTransitionsRejectInvalidJumps, on the phone's side: the engine's
    // half is the experiment in plan.md, HU-23. No state offers a jump.
    @Test
    fun eachStateOffersOnlyTheStepThatFollowsIt() {
        assertEquals(AttentionStep.MarkArrival, ServiceStatus.ASSIGNED.nextStep())
        assertEquals(AttentionStep.MarkCompleted, ServiceStatus.IN_PROGRESS.nextStep())
        assertEquals(ServiceStatus.IN_PROGRESS, AttentionStep.MarkArrival.targetStatus)
        assertEquals(ServiceStatus.COMPLETED, AttentionStep.MarkCompleted.targetStatus)
    }

    // RF-10.3, and cannotCancelCompletedService on the phone's side.
    @Test
    fun onlyAnAttentionNotYetFinishedCanBeCancelled() {
        listOf(true, false).forEach { iAmThePatient ->
            assertTrue(attention(status = ServiceStatus.ASSIGNED, iAmThePatient = iAmThePatient).canCancel)
            assertTrue(attention(status = ServiceStatus.IN_PROGRESS, iAmThePatient = iAmThePatient).canCancel)
            assertFalse(attention(status = ServiceStatus.COMPLETED, iAmThePatient = iAmThePatient).canCancel)
            assertFalse(attention(status = ServiceStatus.CANCELLED, iAmThePatient = iAmThePatient).canCancel)
        }
    }

    // cannotMarkArrivalOnCancelledService.
    @Test
    fun aFinishedAttentionOffersNoStep() {
        assertNull(ServiceStatus.COMPLETED.nextStep())
        assertNull(ServiceStatus.CANCELLED.nextStep())
    }

    // RF-10.1 and RF-10.2 give both marks to the professional.
    @Test
    fun thePatientNeverHasAStep() {
        ServiceStatus.entries.forEach { status ->
            assertNull(status.name, attention(status = status, iAmThePatient = true).nextStepForMe)
        }
    }

    // RN-10, HU-29: one rating per attention, over a completed one, by the patient.
    @Test
    fun thePatientRatesACompletedAttentionNotYetRated() {
        assertTrue(attention(status = ServiceStatus.COMPLETED, iAmThePatient = true).canRate)
    }

    @Test
    fun noOneRatesAnAttentionThatIsNotCompleted() {
        listOf(ServiceStatus.ASSIGNED, ServiceStatus.IN_PROGRESS, ServiceStatus.CANCELLED).forEach { status ->
            assertFalse(status.name, attention(status = status, iAmThePatient = true).canRate)
        }
    }

    @Test
    fun theProfessionalHasNoRatingToLeaveWithThisStory() {
        assertFalse(attention(status = ServiceStatus.COMPLETED, iAmThePatient = false).canRate)
    }

    @Test
    fun anAttentionAlreadyRatedIsNotOfferedForRatingAgain() {
        val rated =
            attention(
                status = ServiceStatus.COMPLETED,
                iAmThePatient = true,
                myReview = MyReview(4, null, java.time.Instant.parse("2026-11-26T16:00:00Z")),
            )

        assertFalse(rated.canRate)
    }
}

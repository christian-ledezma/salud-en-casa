package bo.saludencasa.features.disputes.data.mapper

import bo.saludencasa.features.disputes.data.model.PaymentDisputeDto
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.services.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class PaymentDisputeMapperTest {
    private fun dto(
        method: String? = "DIRECT_QR",
        finishedAt: String? = "2026-11-23T13:38:00+00:00",
        disputedAt: String = "2026-11-23T14:15:00+00:00",
    ) = PaymentDisputeDto(
        paymentId = "p1",
        requestId = "r1",
        serviceTypeName = "Aplicación de inyectables",
        totalAmountBob = BigDecimal("45.00"),
        method = method,
        finishedAt = finishedAt,
        patientConfirmedAt = "2026-11-23T13:40:00+00:00",
        disputedAt = disputedAt,
        patientName = "Ana Quispe",
        patientEmail = "ana.quispe@ejemplo.com",
        professionalName = "María Flores",
        professionalEmail = "maria.flores@ejemplo.com",
    )

    @Test
    fun mapsEveryPartyWithTheirEmailAndEveryTime() {
        val dispute = dto().toPaymentDispute()!!

        assertEquals(PaymentMethod.DIRECT_QR, dispute.method)
        assertEquals(Instant.parse("2026-11-23T14:15:00Z"), dispute.disputedAt)
        assertEquals("ana.quispe@ejemplo.com", dispute.patient.email)
        assertEquals("maria.flores@ejemplo.com", dispute.professional.email)
    }

    @Test
    fun anUnknownMethodOrAnUnreadableTimeIsAContractMismatch() {
        assertNull(dto(method = "GIFT").toPaymentDispute())
        assertNull(dto(disputedAt = "ayer").toPaymentDispute())
        assertNull(dto(finishedAt = "ayer").toPaymentDispute())
    }

    // Resolved meanwhile: the screen says so instead of an error to retry.
    @Test
    fun aResolutionRefusedByTheGuardMeansItWasAlreadyResolved() {
        val refused = IllegalStateException("an_administrator_only_resolves_a_dispute")

        assertEquals(DisputeError.AlreadyResolved, refused.toDisputeError())
    }

    @Test
    fun anyOtherRefusalStaysUnexpected() {
        assertEquals(DisputeError.Unexpected, IllegalStateException("payment_method_not_allowed").toDisputeError())
    }
}

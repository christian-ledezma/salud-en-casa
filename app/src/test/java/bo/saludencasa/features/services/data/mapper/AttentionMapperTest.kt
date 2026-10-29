package bo.saludencasa.features.services.data.mapper

import bo.saludencasa.features.services.data.model.AttentionDto
import bo.saludencasa.features.services.domain.model.ServiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class AttentionMapperTest {
    private fun dto(
        status: String = "ASSIGNED",
        createdAt: String = "2026-10-29T12:00:00Z",
        iAmPatient: Boolean = true,
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
    fun rejectsAnUnknownStatus() {
        assertNull(dto(status = "ARCHIVED").toAttention())
    }

    @Test
    fun rejectsAnUnparseableCreatedAt() {
        assertNull(dto(createdAt = "not-a-timestamp").toAttention())
    }
}

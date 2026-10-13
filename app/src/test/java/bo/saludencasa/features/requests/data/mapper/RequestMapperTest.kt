package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.data.model.ServiceRequestDto
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class RequestMapperTest {
    @Test
    fun mapsAPublishedImmediateRequest() {
        val request = dto().toServiceRequest()!!

        assertEquals(RequestStatus.PUBLISHED, request.status)
        assertEquals(RequestModality.IMMEDIATE, request.modality)
        assertEquals(0, BigDecimal("150.00").compareTo(request.suggestedBudgetBob))
    }

    // PostgREST writes the offset as +00:00, which Instant.parse cannot read; the
    // mapper must still produce a valid instant.
    @Test
    fun parsesTheTimestampWithAnOffset() {
        val request = dto(createdAt = "2026-10-12T12:00:00+00:00").toServiceRequest()!!

        assertEquals("2026-10-12T12:00:00Z", request.createdAt.toString())
    }

    // A status the client does not know is a row it cannot honestly draw, so the
    // whole row is dropped rather than guessed.
    @Test
    fun returnsNullForAnUnknownStatus() {
        assertNull(dto(status = "ARCHIVED").toServiceRequest())
    }

    @Test
    fun returnsNullForAnUnparseableCreatedAt() {
        assertNull(dto(createdAt = "not-a-date").toServiceRequest())
    }

    @Test
    fun treatsAnAbsentBudgetAndDescriptionAsNull() {
        val request = dto(suggestedBudgetBob = null, description = null).toServiceRequest()!!

        assertNull(request.suggestedBudgetBob)
        assertNull(request.description)
    }

    private fun dto(
        status: String = "PUBLISHED",
        modality: String = "IMMEDIATE",
        createdAt: String = "2026-10-12T12:00:00+00:00",
        suggestedBudgetBob: String? = "150.00",
        description: String? = "Fiebre alta",
    ): ServiceRequestDto =
        ServiceRequestDto(
            id = "r1",
            serviceTypeName = "Vacunación",
            addressText = "Avenida América 1234, Cochabamba",
            city = "Cochabamba",
            modality = modality,
            status = status,
            description = description,
            suggestedBudgetBob = suggestedBudgetBob,
            expiresAt = "2026-10-12T13:00:00+00:00",
            createdAt = createdAt,
        )
}

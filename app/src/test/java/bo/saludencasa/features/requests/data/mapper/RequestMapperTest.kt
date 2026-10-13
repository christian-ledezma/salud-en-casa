package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.data.model.InboxRequestDto
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

    // The engine rounds the distance to the hundred metres, so the mapper only
    // has to turn the double it sends into the int the screen reads.
    @Test
    fun mapsAnInboxRequestAndRoundsItsDistance() {
        val request = inboxDto(distanceM = 1249.6).toInboxRequest()!!

        assertEquals(1250, request.distanceM)
        assertEquals(RequestStatus.PUBLISHED, request.status)
        assertEquals(0, BigDecimal("150.00").compareTo(request.suggestedBudgetBob))
    }

    @Test
    fun parsesTheInboxTimestampWithAnOffset() {
        val request = inboxDto(createdAt = "2026-10-13T12:00:00+00:00").toInboxRequest()!!

        assertEquals("2026-10-13T12:00:00Z", request.createdAt.toString())
    }

    @Test
    fun returnsNullForAnUnknownInboxStatus() {
        assertNull(inboxDto(status = "ARCHIVED").toInboxRequest())
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

    private fun inboxDto(
        status: String = "PUBLISHED",
        createdAt: String = "2026-10-13T12:00:00+00:00",
        distanceM: Double = 1200.0,
    ): InboxRequestDto =
        InboxRequestDto(
            id = "r1",
            serviceTypeName = "Vacunación",
            city = "Cochabamba",
            description = "Fiebre alta",
            suggestedBudgetBob = BigDecimal("150.00"),
            distanceM = distanceM,
            status = status,
            createdAt = createdAt,
        )
}

package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.data.model.BookableStartDto
import bo.saludencasa.features.requests.data.model.InboxRequestDto
import bo.saludencasa.features.requests.data.model.ServiceRequestDto
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

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

    // HU-16. The constraint scheduled_request_is_complete keeps this from the
    // database, so a scheduled row without its time is a broken reply, and
    // drawing it would show an appointment with no date.
    @Test
    fun aScheduledRequestWithoutItsTimeIsRejected() {
        assertNull(dto(modality = "SCHEDULED", scheduledAt = null).toServiceRequest())
        assertNull(inboxDto(modality = "SCHEDULED", scheduledAt = null).toInboxRequest())
    }

    @Test
    fun aScheduledRequestCarriesItsTimeAndItsProfessional() {
        val request =
            dto(
                modality = "SCHEDULED",
                scheduledAt = "2026-10-19T14:00:00+00:00",
                professionalName = "Ana Quispe",
            ).toServiceRequest()!!

        assertEquals(Instant.parse("2026-10-19T14:00:00Z"), request.scheduledAt)
        assertEquals("Ana Quispe", request.professionalName)
    }

    @Test
    fun theInboxTellsAScheduledRequestFromAnImmediateOne() {
        val scheduled =
            inboxDto(modality = "SCHEDULED", scheduledAt = "2026-10-19T14:00:00+00:00").toInboxRequest()!!
        val immediate = inboxDto().toInboxRequest()!!

        assertEquals(RequestModality.SCHEDULED, scheduled.modality)
        assertEquals(Instant.parse("2026-10-19T14:00:00Z"), scheduled.scheduledAt)
        assertEquals(RequestModality.IMMEDIATE, immediate.modality)
        assertNull(immediate.scheduledAt)
    }

    @Test
    fun anUnknownModalityInTheInboxIsRejected() {
        assertNull(inboxDto(modality = "LATER").toInboxRequest())
    }

    // The engine computes the wall-clock date and time in La Paz; the mapper
    // takes them as they come and never derives them from the instant.
    @Test
    fun aBookableStartKeepsTheEnginesInstantDateAndTime() {
        val start =
            BookableStartDto(
                startAt = "2026-10-20T01:30:00+00:00",
                localDate = "2026-10-19",
                localTime = "21:30:00",
            ).toBookableStart()!!

        assertEquals(Instant.parse("2026-10-20T01:30:00Z"), start.startAt)
        assertEquals(LocalDate.of(2026, 10, 19), start.date)
        assertEquals(LocalTime.of(21, 30), start.time)
    }

    @Test
    fun aBookableStartWithAnUnreadableTimeIsRejected() {
        assertNull(BookableStartDto("2026-10-20T01:30:00+00:00", "2026-10-19", "half past nine").toBookableStart())
    }

    // PostgREST returns a numeric column as an unquoted JSON number. A request
    // that carries a budget must decode; a String-typed field failed on the '8'
    // of 80.00 and the whole read was reported as an unexpected error.
    @Test
    fun decodesARequestWhoseBudgetIsAJsonNumber() {
        val json =
            """[{"id":"r1","service_type_name":"Vacunación","address_text":"Av. América 1234",
               "city":"Cochabamba","modality":"IMMEDIATE","status":"PUBLISHED","description":null,
               "suggested_budget_bob":80.00,"expires_at":"2026-10-24T13:00:00+00:00",
               "created_at":"2026-10-24T12:00:00+00:00","scheduled_at":null,"professional_name":null}]"""

        val request =
            Json
                .decodeFromString<List<ServiceRequestDto>>(json)
                .single()
                .toServiceRequest()!!

        assertEquals(0, BigDecimal("80.00").compareTo(request.suggestedBudgetBob))
    }

    private fun dto(
        status: String = "PUBLISHED",
        modality: String = "IMMEDIATE",
        createdAt: String = "2026-10-12T12:00:00+00:00",
        suggestedBudgetBob: BigDecimal? = BigDecimal("150.00"),
        description: String? = "Fiebre alta",
        scheduledAt: String? = null,
        professionalName: String? = null,
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
            scheduledAt = scheduledAt,
            professionalName = professionalName,
        )

    private fun inboxDto(
        status: String = "PUBLISHED",
        createdAt: String = "2026-10-13T12:00:00+00:00",
        distanceM: Double = 1200.0,
        modality: String = "IMMEDIATE",
        scheduledAt: String? = null,
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
            modality = modality,
            scheduledAt = scheduledAt,
        )
}

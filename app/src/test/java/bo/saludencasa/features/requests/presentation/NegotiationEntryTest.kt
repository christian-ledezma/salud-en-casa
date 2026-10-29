package bo.saludencasa.features.requests.presentation

import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.serviceRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class NegotiationEntryTest {
    private fun immediate(status: RequestStatus) =
        serviceRequest(id = "r1", status = status).copy(modality = RequestModality.IMMEDIATE, scheduledAt = null)

    private fun scheduled(
        status: RequestStatus,
        professionalId: String? = "p1",
    ) = serviceRequest(id = "r1", status = status).copy(
        modality = RequestModality.SCHEDULED,
        scheduledAt = Instant.parse("2026-10-30T15:00:00Z"),
        professionalId = professionalId,
    )

    // An open immediate request is negotiated on the waiting screen, offers and
    // all, whether or not anybody has offered yet.
    @Test
    fun anOpenImmediateRequestOpensTheWaitingScreen() {
        assertEquals(NegotiationEntry.ImmediateCare, immediate(RequestStatus.PUBLISHED).negotiationEntry())
        assertEquals(NegotiationEntry.ImmediateCare, immediate(RequestStatus.NEGOTIATING).negotiationEntry())
    }

    // A scheduled request has one professional: once they offered, their thread.
    @Test
    fun aScheduledRequestInNegotiationOpensItsThread() {
        assertEquals(
            NegotiationEntry.Thread(requestId = "r1", professionalId = "p1"),
            scheduled(RequestStatus.NEGOTIATING).negotiationEntry(),
        )
    }

    // Before the first offer there is no thread to open, and the card says so by
    // not being tappable rather than opening an empty screen.
    @Test
    fun aScheduledRequestWithoutAnOfferOpensNothing() {
        assertNull(scheduled(RequestStatus.PUBLISHED).negotiationEntry())
    }

    @Test
    fun aClosedRequestOpensNothing() {
        listOf(RequestStatus.CANCELLED, RequestStatus.EXPIRED, RequestStatus.ACCEPTED).forEach { status ->
            assertNull(immediate(status).negotiationEntry())
            assertNull(scheduled(status).negotiationEntry())
        }
    }
}

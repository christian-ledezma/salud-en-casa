package bo.saludencasa.features.requests.domain.model

import bo.saludencasa.features.requests.serviceRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceRequestTest {
    @Test
    fun anOpenImmediateRequestWithOffersShowsItsCount() {
        assertTrue(serviceRequest(status = RequestStatus.NEGOTIATING).copy(openOfferCount = 1).showsOfferCount)
    }

    // A published request has no offers yet, and the card says nothing rather than zero.
    @Test
    fun aRequestWithoutOffersDoesNotShowACount() {
        assertFalse(serviceRequest(status = RequestStatus.PUBLISHED).copy(openOfferCount = 0).showsOfferCount)
    }

    // accept_offer leaves the other offers as PROPOSED, and the list keeps cancelled
    // and expired requests, so the engine's count alone would still say «2 ofertas».
    @Test
    fun aClosedRequestDoesNotShowTheOffersLeftOnIt() {
        listOf(RequestStatus.CANCELLED, RequestStatus.EXPIRED, RequestStatus.ACCEPTED).forEach { status ->
            assertFalse(status.name, serviceRequest(status = status).copy(openOfferCount = 2).showsOfferCount)
        }
    }

    // A scheduled request has one addressee, whose name is already on the card.
    @Test
    fun aScheduledRequestDoesNotShowACount() {
        val request =
            serviceRequest(status = RequestStatus.NEGOTIATING, modality = RequestModality.SCHEDULED)
                .copy(openOfferCount = 1)

        assertFalse(request.showsOfferCount)
    }
}

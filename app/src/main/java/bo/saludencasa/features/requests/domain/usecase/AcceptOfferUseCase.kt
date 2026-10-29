package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.OfferActionResult
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

// Accepting the offer on the table seals the agreement: the engine marks the
// offer, creates the service and its pending payment, and moves the request to
// accepted, all in one transaction (RF-08.5). The whole operation lives in a
// server function; this only forwards the offer being accepted.
class AcceptOfferUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(offerId: String): OfferActionResult = repository.acceptOffer(offerId)
}

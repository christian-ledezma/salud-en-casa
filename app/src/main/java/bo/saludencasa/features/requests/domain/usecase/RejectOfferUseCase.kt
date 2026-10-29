package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.OfferActionResult
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

// Rejecting the offer on the table closes that thread. The request itself is not
// touched: it stays in negotiation and other professionals keep seeing it in
// their inbox (docs/decisions.md, 2026-10-27).
class RejectOfferUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(offerId: String): OfferActionResult = repository.rejectOffer(offerId)
}

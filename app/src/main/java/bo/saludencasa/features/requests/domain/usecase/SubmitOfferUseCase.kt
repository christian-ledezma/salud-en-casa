package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

// RF-08.1: the professional proposes an amount and, if they want, a message
// (docs/decisions.md, 2026-10-27, HT-24). The amount is a value object, so an
// invalid one never reaches the network.
class SubmitOfferUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(
        requestId: String,
        amountRaw: String,
        message: String,
    ): OfferSubmitResult {
        val amount =
            AmountBob.parse(amountRaw).getOrElse {
                return OfferSubmitResult.Failure(OfferError.InvalidAmount)
            }

        return repository.submitOffer(requestId, amount, message.trim().ifEmpty { null })
    }
}

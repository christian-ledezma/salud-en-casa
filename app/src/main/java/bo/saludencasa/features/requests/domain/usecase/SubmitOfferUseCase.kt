package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

// RF-08.1: the professional proposes an amount and a message. The amount is a
// value object, so an invalid one never reaches the network; the message is
// required by the criterion.
class SubmitOfferUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(
        requestId: String,
        amountRaw: String,
        message: String,
    ): OfferSubmitResult {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) return OfferSubmitResult.Failure(OfferError.MessageRequired)

        val amount =
            AmountBob.parse(amountRaw).getOrElse {
                return OfferSubmitResult.Failure(OfferError.InvalidAmount)
            }

        return repository.submitOffer(requestId, amount, trimmedMessage)
    }
}

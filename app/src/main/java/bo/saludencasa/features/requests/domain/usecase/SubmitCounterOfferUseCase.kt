package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

// RF-08.2: answering with another amount is a new offer that references the one
// on the table, never an edit of it (RN-05, INV-06). It takes the thread and not
// its pieces, so the parent, the professional and the patient's floor all come
// from the same reading. The floor is checked here so the person hears about it
// before anything leaves; the engine checks it again (docs/decisions.md,
// 2026-10-27, HT-24). The message is optional.
class SubmitCounterOfferUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(
        requestId: String,
        thread: OfferThread,
        amountRaw: String,
        message: String,
    ): OfferSubmitResult {
        val parent = thread.lastOffer ?: return OfferSubmitResult.Failure(OfferError.NotAllowed)

        val amount =
            AmountBob.parse(amountRaw).getOrElse {
                return OfferSubmitResult.Failure(OfferError.InvalidAmount)
            }

        val floor = thread.counterFloorBob
        if (floor != null && amount.value < floor) {
            return OfferSubmitResult.Failure(OfferError.BelowMinimum(floor))
        }

        return repository.submitCounterOffer(
            requestId = requestId,
            professionalId = thread.professionalId,
            parentOfferId = parent.id,
            amount = amount,
            message = message.trim().ifEmpty { null },
        )
    }
}

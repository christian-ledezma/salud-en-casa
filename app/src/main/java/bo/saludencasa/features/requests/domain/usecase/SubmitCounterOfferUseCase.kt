package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.CounterOfferResult
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.repository.IOfferRepository

// RF-08.2: answering with another amount is a new offer that references the one
// on the table, never an edit of it (RN-05, INV-06). It takes the thread and not
// its pieces, so the parent, the professional and the patient's floor all come
// from the same reading. The floor is checked here so the person hears about it
// before anything leaves; the engine checks it again (docs/decisions.md,
// 2026-10-27, HT-24). The message is optional. An amount equal to the offer on the
// table is not a counter-offer: nothing is written, and the person confirms
// accepting it. The floor goes first (docs/decisions.md, 2026-11-04, HT-29).
class SubmitCounterOfferUseCase(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(
        requestId: String,
        thread: OfferThread,
        amountRaw: String,
        message: String,
    ): CounterOfferResult {
        val parent = thread.lastOffer ?: return CounterOfferResult.Failure(OfferError.NotAllowed)

        val amount =
            AmountBob.parse(amountRaw).getOrElse {
                return CounterOfferResult.Failure(OfferError.InvalidAmount)
            }

        val floor = thread.counterFloorBob
        if (floor != null && amount.value < floor) {
            return CounterOfferResult.Failure(OfferError.BelowMinimum(floor))
        }

        // compareTo and not equals: 150 and 150.00 differ in scale, not in amount.
        if (amount.value.compareTo(parent.amountBob) == 0) return CounterOfferResult.AcceptsTheOffer

        val result =
            repository.submitCounterOffer(
                requestId = requestId,
                professionalId = thread.professionalId,
                parentOfferId = parent.id,
                amount = amount,
                message = message.trim().ifEmpty { null },
            )
        return when (result) {
            OfferSubmitResult.Success -> CounterOfferResult.Sent
            is OfferSubmitResult.Failure -> CounterOfferResult.Failure(result.error)
        }
    }
}

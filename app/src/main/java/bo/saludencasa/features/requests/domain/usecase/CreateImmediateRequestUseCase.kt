package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.CreateImmediateRequestDraft
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository
import bo.saludencasa.features.requests.domain.vo.RequestDescription

class CreateImmediateRequestUseCase(
    private val repository: IRequestRepository,
) {
    // Description and budget are optional (RF-07.1): blank means absent, not
    // invalid. A value that is present is held to its value object before it
    // reaches the server.
    suspend operator fun invoke(
        serviceTypeId: String,
        addressId: String,
        description: String,
        budget: String,
    ): RequestResult {
        val validatedDescription =
            description.trim().takeIf { it.isNotEmpty() }?.let { raw ->
                RequestDescription.create(raw).getOrElse {
                    return RequestResult.Failure(RequestError.InvalidDescription)
                }
            }

        val validatedBudget =
            budget.trim().takeIf { it.isNotEmpty() }?.let { raw ->
                AmountBob.parse(raw).getOrElse {
                    return RequestResult.Failure(RequestError.InvalidBudget)
                }
            }

        return repository.createImmediate(
            CreateImmediateRequestDraft(
                serviceTypeId = serviceTypeId,
                addressId = addressId,
                description = validatedDescription?.value,
                suggestedBudgetBob = validatedBudget?.value,
            ),
        )
    }
}

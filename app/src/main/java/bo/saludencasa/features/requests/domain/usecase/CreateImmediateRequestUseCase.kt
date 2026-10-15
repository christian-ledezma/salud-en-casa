package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.CreateImmediateRequestDraft
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository

class CreateImmediateRequestUseCase(
    private val repository: IRequestRepository,
) {
    suspend operator fun invoke(
        serviceTypeId: String,
        addressId: String,
        description: String,
        budget: String,
    ): RequestResult =
        when (val fields = validateOptionalFields(description, budget)) {
            is OptionalFields.Invalid -> {
                RequestResult.Failure(fields.error)
            }

            is OptionalFields.Valid -> {
                repository.createImmediate(
                    CreateImmediateRequestDraft(
                        serviceTypeId = serviceTypeId,
                        addressId = addressId,
                        description = fields.description,
                        suggestedBudgetBob = fields.suggestedBudgetBob,
                    ),
                )
            }
        }
}

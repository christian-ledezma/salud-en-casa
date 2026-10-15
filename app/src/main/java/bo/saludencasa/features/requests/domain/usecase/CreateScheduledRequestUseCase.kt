package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.domain.model.BookableStart
import bo.saludencasa.features.requests.domain.model.CreateScheduledRequestDraft
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.repository.IRequestRepository

class CreateScheduledRequestUseCase(
    private val repository: IRequestRepository,
) {
    // The time is checked by the server alone (docs/decisions.md, 2026-10-15, HU-16).
    suspend operator fun invoke(
        professionalId: String,
        serviceTypeId: String,
        addressId: String,
        start: BookableStart,
        description: String,
        budget: String,
    ): RequestResult =
        when (val fields = validateOptionalFields(description, budget)) {
            is OptionalFields.Invalid -> {
                RequestResult.Failure(fields.error)
            }

            is OptionalFields.Valid -> {
                repository.createScheduled(
                    CreateScheduledRequestDraft(
                        professionalId = professionalId,
                        serviceTypeId = serviceTypeId,
                        addressId = addressId,
                        scheduledAt = start.startAt,
                        description = fields.description,
                        suggestedBudgetBob = fields.suggestedBudgetBob,
                    ),
                )
            }
        }
}

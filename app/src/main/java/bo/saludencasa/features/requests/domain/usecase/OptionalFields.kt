package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.vo.RequestDescription
import java.math.BigDecimal

internal sealed interface OptionalFields {
    data class Valid(
        val description: String?,
        val suggestedBudgetBob: BigDecimal?,
    ) : OptionalFields

    data class Invalid(
        val error: RequestError,
    ) : OptionalFields
}

// Both are optional (RF-07.1): blank means absent, not invalid. A value that is
// present is held to its value object before it reaches the server.
internal fun validateOptionalFields(
    description: String,
    budget: String,
): OptionalFields {
    val validatedDescription =
        description.trim().takeIf { it.isNotEmpty() }?.let { raw ->
            RequestDescription.create(raw).getOrElse {
                return OptionalFields.Invalid(RequestError.InvalidDescription)
            }
        }

    val validatedBudget =
        budget.trim().takeIf { it.isNotEmpty() }?.let { raw ->
            AmountBob.parse(raw).getOrElse {
                return OptionalFields.Invalid(RequestError.InvalidBudget)
            }
        }

    return OptionalFields.Valid(
        description = validatedDescription?.value,
        suggestedBudgetBob = validatedBudget?.value,
    )
}

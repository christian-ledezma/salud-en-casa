package bo.saludencasa.features.requests.domain.model

import java.math.BigDecimal

// What travels into the creation function: the identifiers the patient chose and
// the two optional fields, already validated. The location snapshot and the
// expiry are the server's to set, so they are not here.
data class CreateImmediateRequestDraft(
    val serviceTypeId: String,
    val addressId: String,
    val description: String?,
    val suggestedBudgetBob: BigDecimal?,
)

sealed interface RequestResult {
    data class Created(
        val requestId: String,
    ) : RequestResult

    data class Failure(
        val error: RequestError,
    ) : RequestResult
}

sealed interface MyRequestsResult {
    data class Loaded(
        val requests: List<ServiceRequest>,
    ) : MyRequestsResult

    data class Failure(
        val error: RequestError,
    ) : MyRequestsResult
}

sealed interface MyRequestsPageResult {
    data class Loaded(
        val requests: List<ServiceRequest>,
        val endReached: Boolean,
    ) : MyRequestsPageResult

    data class Failure(
        val error: RequestError,
    ) : MyRequestsPageResult
}

sealed interface CancelRequestResult {
    data object Success : CancelRequestResult

    data class Failure(
        val error: RequestError,
    ) : CancelRequestResult
}

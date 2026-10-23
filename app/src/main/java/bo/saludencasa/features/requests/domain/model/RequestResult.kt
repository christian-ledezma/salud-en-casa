package bo.saludencasa.features.requests.domain.model

import java.math.BigDecimal
import java.time.Instant

// What travels into the creation function: the identifiers the patient chose and
// the two optional fields, already validated. The location snapshot and the
// expiry are the server's to set, so they are not here.
data class CreateImmediateRequestDraft(
    val serviceTypeId: String,
    val addressId: String,
    val description: String?,
    val suggestedBudgetBob: BigDecimal?,
)

data class CreateScheduledRequestDraft(
    val professionalId: String,
    val serviceTypeId: String,
    val addressId: String,
    val scheduledAt: Instant,
    val description: String?,
    val suggestedBudgetBob: BigDecimal?,
)

sealed interface BookableTimesResult {
    data class Loaded(
        val times: BookableTimes,
    ) : BookableTimesResult

    data class Failure(
        val error: RequestError,
    ) : BookableTimesResult
}

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

// HT-23: what «Atención ahora» shows on arrival. None is an answer, not a
// failure: it is the patient with nothing waiting, who gets the form.
sealed interface ActiveRequestResult {
    data class Found(
        val request: ServiceRequest,
    ) : ActiveRequestResult

    data object None : ActiveRequestResult

    data class Failure(
        val error: RequestError,
    ) : ActiveRequestResult
}

sealed interface CancelRequestResult {
    data object Success : CancelRequestResult

    data class Failure(
        val error: RequestError,
    ) : CancelRequestResult
}

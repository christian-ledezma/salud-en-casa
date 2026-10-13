package bo.saludencasa.features.requests.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestStatus

@StringRes
internal fun RequestError.messageRes(): Int =
    when (this) {
        RequestError.NotSignedIn -> R.string.error_request_not_signed_in
        RequestError.AddressNotOwned -> R.string.error_request_address_not_owned
        RequestError.ServiceTypeUnavailable -> R.string.error_request_service_type_unavailable
        RequestError.InvalidDescription -> R.string.error_request_invalid_description
        RequestError.InvalidBudget -> R.string.error_request_invalid_budget
        RequestError.NetworkUnavailable -> R.string.error_network_unavailable
        RequestError.Unexpected -> R.string.error_unexpected
    }

@StringRes
internal fun RequestStatus.labelRes(): Int =
    when (this) {
        RequestStatus.PUBLISHED -> R.string.request_status_published
        RequestStatus.NEGOTIATING -> R.string.request_status_negotiating
        RequestStatus.ACCEPTED -> R.string.request_status_accepted
        RequestStatus.IN_PROGRESS -> R.string.request_status_in_progress
        RequestStatus.COMPLETED -> R.string.request_status_completed
        RequestStatus.CANCELLED -> R.string.request_status_cancelled
        RequestStatus.EXPIRED -> R.string.request_status_expired
    }

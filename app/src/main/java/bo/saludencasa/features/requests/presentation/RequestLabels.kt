package bo.saludencasa.features.requests.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import bo.saludencasa.R
import bo.saludencasa.core.util.distanceText
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus

@StringRes
internal fun RequestError.messageRes(): Int =
    when (this) {
        RequestError.NotSignedIn -> R.string.error_request_not_signed_in
        RequestError.AddressNotOwned -> R.string.error_request_address_not_owned
        RequestError.ServiceTypeUnavailable -> R.string.error_request_service_type_unavailable
        RequestError.InvalidDescription -> R.string.error_request_invalid_description
        RequestError.InvalidBudget -> R.string.error_request_invalid_budget
        RequestError.CannotScheduleWithYourself -> R.string.error_request_cannot_schedule_with_yourself
        RequestError.ProfessionalUnavailable -> R.string.error_request_professional_unavailable
        RequestError.ServiceNotOffered -> R.string.error_request_service_not_offered
        RequestError.OutsideCoverage -> R.string.error_request_outside_coverage
        RequestError.TimeNotBookable -> R.string.error_request_time_not_bookable
        RequestError.NetworkUnavailable -> R.string.error_network_unavailable
        RequestError.Unexpected -> R.string.error_unexpected
    }

@StringRes
internal fun OfferError.messageRes(): Int =
    when (this) {
        OfferError.NotSignedIn -> R.string.error_request_not_signed_in
        OfferError.InvalidAmount -> R.string.error_offer_invalid_amount
        is OfferError.BelowMinimum -> R.string.error_offer_below_your_last
        OfferError.NotAllowed -> R.string.error_offer_not_allowed
        OfferError.NetworkUnavailable -> R.string.error_network_unavailable
        OfferError.Unexpected -> R.string.error_unexpected
    }

// What the thread is waiting for, said from the reader's side: the two roles
// share the screen, so neither label names a role.
@StringRes
internal fun OfferThread.stateRes(): Int =
    when {
        isClosed -> R.string.thread_state_closed
        isMyTurn -> R.string.thread_state_your_turn
        else -> R.string.thread_state_waiting
    }

@StringRes
internal fun OfferIssuer.threadLabelRes(mine: Boolean): Int =
    when {
        mine -> R.string.thread_issuer_me
        this == OfferIssuer.PROFESSIONAL -> R.string.thread_issuer_professional
        else -> R.string.thread_issuer_patient
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

@StringRes
internal fun RequestModality.labelRes(): Int =
    when (this) {
        RequestModality.IMMEDIATE -> R.string.request_modality_immediate
        RequestModality.SCHEDULED -> R.string.request_modality_scheduled
    }

@Composable
internal fun inboxDistanceText(distanceM: Int): String =
    distanceText(LocalResources.current, distanceM, LocalConfiguration.current.locales[0])

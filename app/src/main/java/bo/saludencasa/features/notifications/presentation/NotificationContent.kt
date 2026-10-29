package bo.saludencasa.features.notifications.presentation

import android.content.res.Resources
import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.core.util.distanceText
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.notifications.domain.model.PushPayload
import java.util.Locale

internal data class NotificationText(
    val title: String,
    val body: String,
)

@StringRes
private fun PushPayload.titleRes(): Int =
    when (this) {
        is PushPayload.NearbyRequest -> R.string.notification_request_nearby_title
        is PushPayload.AppointmentScheduled -> R.string.notification_appointment_scheduled_title
        is PushPayload.AppointmentCancelled -> R.string.notification_appointment_cancelled_title
        is PushPayload.RequestExpired -> R.string.notification_request_expired_title
        is PushPayload.OfferReceived -> R.string.notification_offer_received_title
        is PushPayload.CounterOfferReceived -> R.string.notification_counter_offer_received_title
    }

// RF-09.10.
internal fun PushPayload.text(
    resources: Resources,
    locale: Locale,
): NotificationText {
    val type = subject.serviceTypeName
    val body =
        when (this) {
            is PushPayload.NearbyRequest -> {
                resources.getString(
                    R.string.notification_request_nearby_body,
                    type,
                    distanceText(resources, distanceM, locale),
                )
            }

            is PushPayload.AppointmentScheduled -> {
                resources.getString(
                    R.string.notification_appointment_scheduled_body,
                    type,
                    formatAppointment(scheduledAt, locale),
                )
            }

            is PushPayload.AppointmentCancelled -> {
                resources.getString(
                    R.string.notification_appointment_cancelled_body,
                    type,
                    formatAppointment(scheduledAt, locale),
                )
            }

            is PushPayload.RequestExpired -> {
                resources.getString(R.string.notification_request_expired_body, type)
            }

            is PushPayload.OfferReceived -> {
                resources.getString(
                    R.string.notification_offer_received_body,
                    type,
                    formatBob(amountBob, locale),
                )
            }

            is PushPayload.CounterOfferReceived -> {
                resources.getString(
                    R.string.notification_counter_offer_received_body,
                    type,
                    formatBob(amountBob, locale),
                )
            }
        }
    return NotificationText(title = resources.getString(titleRes()), body = body)
}

// docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 5.
internal fun PushPayload.publicText(resources: Resources): NotificationText =
    NotificationText(
        title = resources.getString(titleRes()),
        body = resources.getString(R.string.notification_public_body),
    )

internal fun PushPayload.channel(): NotificationChannelSpec =
    when (this) {
        is PushPayload.NearbyRequest,
        is PushPayload.AppointmentScheduled,
        is PushPayload.AppointmentCancelled,
        is PushPayload.RequestExpired,
        is PushPayload.OfferReceived,
        is PushPayload.CounterOfferReceived,
        -> NotificationChannelSpec.REQUESTS
    }

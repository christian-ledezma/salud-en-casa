package bo.saludencasa.features.notifications.presentation

import android.content.res.Resources
import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.core.util.distanceText
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.verification.domain.model.ReviewStatus
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
        is PushPayload.OfferAccepted -> R.string.notification_offer_accepted_title
        is PushPayload.MessageReceived -> R.string.notification_message_received_title
        is PushPayload.ProfessionalArrived -> R.string.notification_professional_arrived_title
        is PushPayload.ServiceCompleted -> R.string.notification_service_completed_title
        is PushPayload.AttentionCancelled -> R.string.notification_attention_cancelled_title
        is PushPayload.PatientConfirmedPayment -> R.string.notification_patient_confirmed_payment_title
        is PushPayload.ProfessionalConfirmedPayment -> R.string.notification_professional_confirmed_payment_title
        is PushPayload.PaymentDisputed -> R.string.notification_payment_disputed_title
        is PushPayload.PaymentResolved -> R.string.notification_payment_resolved_title
        is PushPayload.DocumentVerdict -> R.string.notification_document_verdict_title
        is PushPayload.VerificationApproved -> R.string.notification_verification_approved_title
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

            is PushPayload.OfferAccepted -> {
                resources.getString(
                    R.string.notification_offer_accepted_body,
                    type,
                    formatBob(amountBob, locale),
                )
            }

            is PushPayload.MessageReceived -> {
                resources.getString(R.string.notification_message_received_body, senderName, type)
            }

            is PushPayload.ProfessionalArrived -> {
                resources.getString(R.string.notification_professional_arrived_body, senderName, type)
            }

            is PushPayload.ServiceCompleted -> {
                resources.getString(R.string.notification_service_completed_body, senderName, type)
            }

            is PushPayload.AttentionCancelled -> {
                resources.getString(R.string.notification_attention_cancelled_body, senderName, type)
            }

            is PushPayload.PatientConfirmedPayment -> {
                val body =
                    when (method) {
                        PaymentMethod.CASH -> R.string.notification_patient_confirmed_payment_body_cash
                        PaymentMethod.DIRECT_QR -> R.string.notification_patient_confirmed_payment_body_direct_qr
                        PaymentMethod.GATEWAY -> R.string.notification_patient_confirmed_payment_body
                    }
                resources.getString(body, senderName, formatBob(amountBob, locale), type)
            }

            is PushPayload.ProfessionalConfirmedPayment -> {
                val body =
                    when (method) {
                        PaymentMethod.CASH -> R.string.notification_professional_confirmed_payment_body_cash
                        PaymentMethod.DIRECT_QR -> R.string.notification_professional_confirmed_payment_body_direct_qr
                        PaymentMethod.GATEWAY -> R.string.notification_professional_confirmed_payment_body
                    }
                resources.getString(body, senderName, formatBob(amountBob, locale), type)
            }

            is PushPayload.PaymentDisputed -> {
                resources.getString(
                    R.string.notification_payment_disputed_body,
                    senderName,
                    formatBob(amountBob, locale),
                    type,
                )
            }

            is PushPayload.PaymentResolved -> {
                val forThePatient = subject.recipientRole == UserRole.PATIENT
                val body =
                    when {
                        forThePatient && paid -> R.string.notification_payment_resolved_paid_patient
                        forThePatient -> R.string.notification_payment_resolved_not_paid_patient
                        paid -> R.string.notification_payment_resolved_paid_professional
                        else -> R.string.notification_payment_resolved_not_paid_professional
                    }
                resources.getString(body, formatBob(amountBob, locale), senderName, type)
            }

            is PushPayload.DocumentVerdict -> {
                val plural =
                    when (status) {
                        ReviewStatus.APPROVED -> R.plurals.notification_document_verdict_approved_body
                        ReviewStatus.REJECTED -> R.plurals.notification_document_verdict_rejected_body
                        ReviewStatus.PENDING -> error("unreachable: a verdict is never PENDING")
                    }
                resources.getQuantityString(plural, documentCount, documentCount)
            }

            is PushPayload.VerificationApproved -> {
                resources.getString(R.string.notification_verification_approved_body)
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
        is PushPayload.OfferAccepted,
        -> NotificationChannelSpec.REQUESTS

        is PushPayload.MessageReceived -> NotificationChannelSpec.MESSAGES

        is PushPayload.ProfessionalArrived,
        is PushPayload.ServiceCompleted,
        is PushPayload.AttentionCancelled,
        is PushPayload.PatientConfirmedPayment,
        is PushPayload.ProfessionalConfirmedPayment,
        is PushPayload.PaymentDisputed,
        is PushPayload.PaymentResolved,
        -> NotificationChannelSpec.ATTENTIONS

        is PushPayload.DocumentVerdict,
        is PushPayload.VerificationApproved,
        -> NotificationChannelSpec.VERIFICATION
    }

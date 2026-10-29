package bo.saludencasa.features.notifications.domain.model

import bo.saludencasa.features.profile.domain.model.UserRole
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeParseException

enum class NotificationEvent {
    REQUEST_NEARBY,
    APPOINTMENT_SCHEDULED,
    APPOINTMENT_CANCELLED,
    REQUEST_EXPIRED,
    OFFER_RECEIVED,
    COUNTER_OFFER_RECEIVED,
}

data class PushSubject(
    val requestId: String,
    val recipientId: String,
    val recipientRole: UserRole,
    val serviceTypeName: String,
)

// RF-09.10.
sealed interface PushPayload {
    val subject: PushSubject
    val event: NotificationEvent

    data class NearbyRequest(
        override val subject: PushSubject,
        val distanceM: Int,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.REQUEST_NEARBY
    }

    data class AppointmentScheduled(
        override val subject: PushSubject,
        val scheduledAt: Instant,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.APPOINTMENT_SCHEDULED
    }

    data class AppointmentCancelled(
        override val subject: PushSubject,
        val scheduledAt: Instant,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.APPOINTMENT_CANCELLED
    }

    data class RequestExpired(
        override val subject: PushSubject,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.REQUEST_EXPIRED
    }

    // RF-09.10: the amount is structured data, like the distance, never the text
    // of the offer's message, which the engine leaves out of the body.
    data class OfferReceived(
        override val subject: PushSubject,
        val amountBob: BigDecimal,
        // The professional who offered, so the tap opens their thread
        // (docs/decisions.md, 2026-10-27, HT-24).
        val professionalId: String,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.OFFER_RECEIVED
    }

    // The one event that reaches either role: whoever did not write the
    // counter-offer is told of it (RF-09.5). professionalId names the thread, so
    // the tap opens that thread and not the whole negotiation.
    data class CounterOfferReceived(
        override val subject: PushSubject,
        val amountBob: BigDecimal,
        val professionalId: String,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.COUNTER_OFFER_RECEIVED
    }

    fun toMap(): Map<String, String> =
        buildMap {
            put(KEY_EVENT, event.name)
            put(KEY_REQUEST_ID, subject.requestId)
            put(KEY_RECIPIENT_ID, subject.recipientId)
            put(KEY_RECIPIENT_ROLE, subject.recipientRole.name)
            put(KEY_SERVICE_TYPE_NAME, subject.serviceTypeName)
            when (this@PushPayload) {
                is NearbyRequest -> {
                    put(KEY_DISTANCE_M, distanceM.toString())
                }

                is AppointmentScheduled -> {
                    put(KEY_SCHEDULED_AT, scheduledAt.toString())
                }

                is AppointmentCancelled -> {
                    put(KEY_SCHEDULED_AT, scheduledAt.toString())
                }

                is RequestExpired -> {
                    // Nothing of its own: the subject carries all of it.
                }

                is OfferReceived -> {
                    put(KEY_AMOUNT_BOB, amountBob.toPlainString())
                    put(KEY_PROFESSIONAL_ID, professionalId)
                }

                is CounterOfferReceived -> {
                    put(KEY_AMOUNT_BOB, amountBob.toPlainString())
                    put(KEY_PROFESSIONAL_ID, professionalId)
                }
            }
        }

    companion object {
        const val KEY_EVENT = "event"
        const val KEY_REQUEST_ID = "requestId"
        const val KEY_RECIPIENT_ID = "recipientId"
        const val KEY_RECIPIENT_ROLE = "recipientRole"
        const val KEY_SERVICE_TYPE_NAME = "serviceTypeName"
        const val KEY_DISTANCE_M = "distanceM"
        const val KEY_SCHEDULED_AT = "scheduledAt"
        const val KEY_AMOUNT_BOB = "amountBob"
        const val KEY_PROFESSIONAL_ID = "professionalId"

        private val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
        private val RECIPIENT_ROLES = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL)

        fun parse(data: Map<String, String>): Result<PushPayload> {
            val event =
                NotificationEvent.entries.firstOrNull { it.name == data[KEY_EVENT] }
                    ?: return failure("push_payload_unknown_event")
            val requestId =
                data[KEY_REQUEST_ID]?.takeIf(UUID_PATTERN::matches) ?: return failure("push_payload_invalid_request")
            val recipientId =
                data[KEY_RECIPIENT_ID]?.takeIf(UUID_PATTERN::matches)
                    ?: return failure("push_payload_invalid_recipient")
            val role =
                RECIPIENT_ROLES.firstOrNull { it.name == data[KEY_RECIPIENT_ROLE] }
                    ?: return failure("push_payload_invalid_role")
            val serviceTypeName =
                data[KEY_SERVICE_TYPE_NAME]?.takeIf(String::isNotBlank)
                    ?: return failure("push_payload_missing_service_type")
            val subject = PushSubject(requestId, recipientId, role, serviceTypeName)

            return when (event) {
                NotificationEvent.REQUEST_NEARBY -> {
                    val distanceM =
                        data[KEY_DISTANCE_M]?.toIntOrNull()?.takeIf { it >= 0 }
                            ?: return failure("push_payload_invalid_distance")
                    Result.success(NearbyRequest(subject, distanceM))
                }

                NotificationEvent.APPOINTMENT_SCHEDULED -> {
                    val at =
                        data[KEY_SCHEDULED_AT].toInstantOrNull() ?: return failure("push_payload_invalid_scheduled_at")
                    Result.success(AppointmentScheduled(subject, at))
                }

                NotificationEvent.APPOINTMENT_CANCELLED -> {
                    val at =
                        data[KEY_SCHEDULED_AT].toInstantOrNull() ?: return failure("push_payload_invalid_scheduled_at")
                    Result.success(AppointmentCancelled(subject, at))
                }

                NotificationEvent.REQUEST_EXPIRED -> {
                    Result.success(RequestExpired(subject))
                }

                NotificationEvent.OFFER_RECEIVED -> {
                    val amount = data.amountOrNull() ?: return failure("push_payload_invalid_amount")
                    val professionalId =
                        data.professionalIdOrNull() ?: return failure("push_payload_invalid_professional")
                    Result.success(OfferReceived(subject, amount, professionalId))
                }

                NotificationEvent.COUNTER_OFFER_RECEIVED -> {
                    val amount = data.amountOrNull() ?: return failure("push_payload_invalid_amount")
                    val professionalId =
                        data.professionalIdOrNull() ?: return failure("push_payload_invalid_professional")
                    Result.success(CounterOfferReceived(subject, amount, professionalId))
                }
            }
        }

        private fun Map<String, String>.professionalIdOrNull(): String? =
            this[KEY_PROFESSIONAL_ID]?.takeIf(UUID_PATTERN::matches)

        private fun Map<String, String>.amountOrNull(): BigDecimal? =
            this[KEY_AMOUNT_BOB]?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }

        private fun String?.toInstantOrNull(): Instant? =
            this?.let {
                try {
                    Instant.parse(it)
                } catch (_: DateTimeParseException) {
                    null
                }
            }

        private fun failure(key: String): Result<PushPayload> = Result.failure(IllegalArgumentException(key))
    }
}

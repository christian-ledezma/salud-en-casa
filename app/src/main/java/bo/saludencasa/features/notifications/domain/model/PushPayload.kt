package bo.saludencasa.features.notifications.domain.model

import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.domain.model.ReviewStatus
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
    OFFER_ACCEPTED,
    MESSAGE_RECEIVED,
    PROFESSIONAL_ARRIVED,
    SERVICE_COMPLETED,
    ATTENTION_CANCELLED,
    DOCUMENT_VERDICT,
    VERIFICATION_APPROVED,
}

// requestId, recipientRole and serviceTypeName are null for HU-37's two events:
// neither is about a service_requests row, and the checklist they open answers
// to every role the person holds, not to one of them.
data class PushSubject(
    val requestId: String?,
    val recipientId: String,
    val recipientRole: UserRole?,
    val serviceTypeName: String?,
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

    // Reaches the issuer of the accepted offer: whoever did not accept is told
    // their offer was taken (RF-09.5). The tap opens the attention, which is keyed
    // by the request, so no professionalId travels. The amount is structured data.
    data class OfferAccepted(
        override val subject: PushSubject,
        val amountBob: BigDecimal,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.OFFER_ACCEPTED
    }

    // Reaches the other party of the service. The sender's name is a profile
    // attribute, not text of the message, which never travels (RF-09.10).
    data class MessageReceived(
        override val subject: PushSubject,
        val senderName: String,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.MESSAGE_RECEIVED
    }

    // Both reach the patient and open the attention. The name is the professional's,
    // a profile attribute (RF-09.10).
    data class ProfessionalArrived(
        override val subject: PushSubject,
        val senderName: String,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.PROFESSIONAL_ARRIVED
    }

    data class ServiceCompleted(
        override val subject: PushSubject,
        val senderName: String,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.SERVICE_COMPLETED
    }

    // To the counterpart of whoever cancelled, either role. The name is theirs, a
    // profile attribute; the reason is written by a user and never travels (RF-09.10).
    data class AttentionCancelled(
        override val subject: PushSubject,
        val senderName: String,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.ATTENTION_CANCELLED
    }

    // Reaches whoever uploaded the document, for one verdict or several
    // registered together (RF-09.9). The reason, written by the administrator,
    // never travels (RF-09.10): it is read by opening the checklist, as before
    // this event existed.
    data class DocumentVerdict(
        override val subject: PushSubject,
        val status: ReviewStatus,
        val documentCount: Int,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.DOCUMENT_VERDICT
    }

    // Reaches the professional once they are searchable (RF-09.9, RF-04.5).
    data class VerificationApproved(
        override val subject: PushSubject,
    ) : PushPayload {
        override val event: NotificationEvent get() = NotificationEvent.VERIFICATION_APPROVED
    }

    fun toMap(): Map<String, String> =
        buildMap {
            put(KEY_EVENT, event.name)
            subject.requestId?.let { put(KEY_REQUEST_ID, it) }
            put(KEY_RECIPIENT_ID, subject.recipientId)
            subject.recipientRole?.let { put(KEY_RECIPIENT_ROLE, it.name) }
            subject.serviceTypeName?.let { put(KEY_SERVICE_TYPE_NAME, it) }
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

                is OfferAccepted -> {
                    put(KEY_AMOUNT_BOB, amountBob.toPlainString())
                }

                is MessageReceived -> {
                    put(KEY_SENDER_NAME, senderName)
                }

                is ProfessionalArrived -> {
                    put(KEY_SENDER_NAME, senderName)
                }

                is ServiceCompleted -> {
                    put(KEY_SENDER_NAME, senderName)
                }

                is AttentionCancelled -> {
                    put(KEY_SENDER_NAME, senderName)
                }

                is DocumentVerdict -> {
                    put(KEY_STATUS, status.name)
                    put(KEY_DOCUMENT_COUNT, documentCount.toString())
                }

                is VerificationApproved -> {
                    // Nothing of its own: the subject carries all of it.
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
        const val KEY_SENDER_NAME = "senderName"
        const val KEY_STATUS = "status"
        const val KEY_DOCUMENT_COUNT = "documentCount"

        private val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
        private val RECIPIENT_ROLES = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL)

        // PENDING is never a verdict: it is where a document starts, not where
        // an administrator leaves it.
        private val VERDICT_STATUSES = setOf(ReviewStatus.APPROVED, ReviewStatus.REJECTED)

        fun parse(data: Map<String, String>): Result<PushPayload> {
            val event =
                NotificationEvent.entries.firstOrNull { it.name == data[KEY_EVENT] }
                    ?: return failure("push_payload_unknown_event")
            val recipientId =
                data[KEY_RECIPIENT_ID]?.takeIf(UUID_PATTERN::matches)
                    ?: return failure("push_payload_invalid_recipient")

            // Neither event below is about a request, so neither requires one,
            // a role or a service type: the subject carries only the recipient.
            if (event == NotificationEvent.DOCUMENT_VERDICT) {
                val subject = PushSubject(requestId = null, recipientId, recipientRole = null, serviceTypeName = null)
                val status = data.verdictStatusOrNull() ?: return failure("push_payload_invalid_status")
                val documentCount =
                    data[KEY_DOCUMENT_COUNT]?.toIntOrNull()?.takeIf { it > 0 }
                        ?: return failure("push_payload_invalid_document_count")
                return Result.success(DocumentVerdict(subject, status, documentCount))
            }
            if (event == NotificationEvent.VERIFICATION_APPROVED) {
                val subject = PushSubject(requestId = null, recipientId, recipientRole = null, serviceTypeName = null)
                return Result.success(VerificationApproved(subject))
            }

            val requestId =
                data[KEY_REQUEST_ID]?.takeIf(UUID_PATTERN::matches) ?: return failure("push_payload_invalid_request")
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

                NotificationEvent.OFFER_ACCEPTED -> {
                    val amount = data.amountOrNull() ?: return failure("push_payload_invalid_amount")
                    Result.success(OfferAccepted(subject, amount))
                }

                NotificationEvent.MESSAGE_RECEIVED -> {
                    val senderName = data.senderNameOrNull() ?: return failure("push_payload_missing_sender_name")
                    Result.success(MessageReceived(subject, senderName))
                }

                NotificationEvent.PROFESSIONAL_ARRIVED -> {
                    val senderName = data.senderNameOrNull() ?: return failure("push_payload_missing_sender_name")
                    Result.success(ProfessionalArrived(subject, senderName))
                }

                NotificationEvent.SERVICE_COMPLETED -> {
                    val senderName = data.senderNameOrNull() ?: return failure("push_payload_missing_sender_name")
                    Result.success(ServiceCompleted(subject, senderName))
                }

                NotificationEvent.ATTENTION_CANCELLED -> {
                    val senderName = data.senderNameOrNull() ?: return failure("push_payload_missing_sender_name")
                    Result.success(AttentionCancelled(subject, senderName))
                }

                NotificationEvent.DOCUMENT_VERDICT, NotificationEvent.VERIFICATION_APPROVED -> {
                    error("unreachable: $event already returned above")
                }
            }
        }

        private fun Map<String, String>.senderNameOrNull(): String? = this[KEY_SENDER_NAME]?.takeIf(String::isNotBlank)

        private fun Map<String, String>.verdictStatusOrNull(): ReviewStatus? =
            this[KEY_STATUS]?.let { raw -> VERDICT_STATUSES.firstOrNull { it.name == raw } }

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

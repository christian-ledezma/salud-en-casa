package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.model.NotificationDestination
import bo.saludencasa.features.notifications.domain.model.NotificationOpenResult
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.notifications.domain.model.PushSubject
import bo.saludencasa.features.notifications.domain.repository.INotificationRepository
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetRolesUseCase
import bo.saludencasa.features.profile.domain.usecase.SwitchActiveRoleUseCase

// RF-09.11.
class OpenNotificationUseCase(
    private val repository: INotificationRepository,
    private val getRoles: GetRolesUseCase,
    private val switchActiveRole: SwitchActiveRoleUseCase,
) {
    suspend operator fun invoke(payload: PushPayload): NotificationOpenResult {
        val userId = repository.awaitSignedInUserId()
        if (userId == null || userId != payload.subject.recipientId) return NotificationOpenResult.Dropped

        val roles =
            when (val result = getRoles()) {
                is RoleResult.Loaded -> result.roles
                is RoleResult.Failure -> return NotificationOpenResult.Failure(result.error)
            }

        // Null for HU-37's two events: the checklist answers to every role the
        // person holds, so nothing is switched before opening it.
        val role = payload.subject.recipientRole
        if (role != null && roles.active != role) {
            val switched = switchActiveRole(role)
            if (switched is SwitchRoleResult.Failure) return NotificationOpenResult.Failure(switched.error)
        }

        return NotificationOpenResult.Open(payload.destination())
    }

    // docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 8.
    private fun PushPayload.destination(): NotificationDestination =
        when (this) {
            // docs/decisions.md, 2026-10-27, HT-24: a request for the
            // professional opens that request, where they offer, and not the
            // inbox with the row to look for.
            is PushPayload.NearbyRequest -> {
                NotificationDestination.OfferThread(subject.requestIdOfARequest, professionalId = null)
            }

            is PushPayload.AppointmentScheduled -> {
                NotificationDestination.OfferThread(subject.requestIdOfARequest, professionalId = null)
            }

            is PushPayload.AppointmentCancelled -> {
                NotificationDestination.Inbox
            }

            is PushPayload.RequestExpired -> {
                NotificationDestination.MyRequests
            }

            // docs/decisions.md, 2026-10-27, HT-24: an offer and a counter-offer
            // both take whoever receives them straight to that thread.
            is PushPayload.OfferReceived -> {
                NotificationDestination.OfferThread(subject.requestIdOfARequest, professionalId)
            }

            // The professional opens their own thread without their identifier,
            // the way every one of their entries does, so the screen listens on
            // their side before it has read anything.
            is PushPayload.CounterOfferReceived -> {
                NotificationDestination.OfferThread(
                    subject.requestIdOfARequest,
                    professionalId.takeIf { subject.recipientRole == UserRole.PATIENT },
                )
            }

            // HU-19: an accepted offer takes its issuer to the attention, which
            // each party resolves from the request.
            is PushPayload.OfferAccepted -> {
                NotificationDestination.Attention(subject.requestIdOfARequest)
            }

            is PushPayload.MessageReceived -> {
                NotificationDestination.Conversation(subject.requestIdOfARequest)
            }

            is PushPayload.ProfessionalArrived -> {
                NotificationDestination.Attention(subject.requestIdOfARequest)
            }

            // docs/decisions.md, 2026-11-04, HU-23.
            is PushPayload.ServiceCompleted -> {
                NotificationDestination.Attention(subject.requestIdOfARequest)
            }

            is PushPayload.AttentionCancelled -> {
                NotificationDestination.Attention(subject.requestIdOfARequest)
            }

            is PushPayload.DocumentVerdict,
            is PushPayload.VerificationApproved,
            -> {
                NotificationDestination.Verification
            }
        }

    // Every payload reaching this point but the two above is about a request;
    // parse() never builds one of those without it.
    private val PushSubject.requestIdOfARequest: String
        get() = checkNotNull(requestId)
}

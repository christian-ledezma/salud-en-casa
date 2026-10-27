package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.model.NotificationDestination
import bo.saludencasa.features.notifications.domain.model.NotificationOpenResult
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.notifications.domain.repository.INotificationRepository
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
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

        val role = payload.subject.recipientRole
        if (roles.active != role) {
            val switched = switchActiveRole(role)
            if (switched is SwitchRoleResult.Failure) return NotificationOpenResult.Failure(switched.error)
        }

        return NotificationOpenResult.Open(payload.destination())
    }

    // docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 8.
    private fun PushPayload.destination(): NotificationDestination =
        when (this) {
            is PushPayload.NearbyRequest -> NotificationDestination.Inbox(subject.requestId)

            is PushPayload.AppointmentScheduled -> NotificationDestination.Inbox(subject.requestId)

            is PushPayload.AppointmentCancelled -> NotificationDestination.Inbox(null)

            is PushPayload.RequestExpired -> NotificationDestination.MyRequests

            // docs/decisions.md, 2026-10-23: the offer takes the patient to the
            // request's offers.
            is PushPayload.OfferReceived -> NotificationDestination.Negotiation(subject.requestId)
        }
}

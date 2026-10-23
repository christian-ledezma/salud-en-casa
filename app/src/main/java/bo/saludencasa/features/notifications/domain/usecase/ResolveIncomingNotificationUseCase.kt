package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.model.DiscardReason
import bo.saludencasa.features.notifications.domain.model.IncomingNotification
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.notifications.domain.repository.INotificationRepository

// docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 4.
class ResolveIncomingNotificationUseCase(
    private val repository: INotificationRepository,
) {
    suspend operator fun invoke(data: Map<String, String>): IncomingNotification {
        val payload =
            PushPayload.parse(data).getOrElse {
                return IncomingNotification.Discarded(DiscardReason.MALFORMED)
            }
        val userId =
            repository.awaitSignedInUserId()
                ?: return IncomingNotification.Discarded(DiscardReason.NO_SESSION)

        return if (userId == payload.subject.recipientId) {
            IncomingNotification.Show(payload)
        } else {
            IncomingNotification.Discarded(DiscardReason.OTHER_ACCOUNT)
        }
    }
}

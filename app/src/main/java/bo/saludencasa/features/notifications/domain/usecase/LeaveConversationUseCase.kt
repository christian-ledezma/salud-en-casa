package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.repository.INotificationRepository

class LeaveConversationUseCase(
    private val repository: INotificationRepository,
) {
    operator fun invoke(requestId: String) {
        repository.leaveConversation(requestId)
    }
}

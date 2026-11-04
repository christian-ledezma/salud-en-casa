package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.repository.INotificationRepository

class EnterConversationUseCase(
    private val repository: INotificationRepository,
) {
    operator fun invoke(requestId: String) {
        repository.enterConversation(requestId)
    }
}

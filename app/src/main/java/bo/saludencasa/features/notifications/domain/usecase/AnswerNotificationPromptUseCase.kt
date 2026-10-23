package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.repository.INotificationRepository

class AnswerNotificationPromptUseCase(
    private val repository: INotificationRepository,
) {
    suspend operator fun invoke() {
        repository.markPromptAnswered()
    }
}

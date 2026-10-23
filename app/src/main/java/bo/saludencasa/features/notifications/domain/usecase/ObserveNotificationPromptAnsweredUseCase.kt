package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow

class ObserveNotificationPromptAnsweredUseCase(
    private val repository: INotificationRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observePromptAnswered()
}

package bo.saludencasa.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.notifications.domain.usecase.AnswerNotificationPromptUseCase
import bo.saludencasa.features.notifications.domain.usecase.ObserveNotificationPromptAnsweredUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

class NotificationPermissionViewModel(
    observeAnswered: ObserveNotificationPromptAnsweredUseCase,
    private val answerPrompt: AnswerNotificationPromptUseCase,
) : ViewModel() {
    val answered: StateFlow<Boolean> =
        observeAnswered().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = true,
        )

    fun onAnswered() {
        viewModelScope.launch { answerPrompt() }
    }
}

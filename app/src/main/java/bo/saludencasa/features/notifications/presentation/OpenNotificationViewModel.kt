package bo.saludencasa.features.notifications.presentation

import androidx.lifecycle.ViewModel
import bo.saludencasa.features.notifications.domain.model.NotificationDestination
import bo.saludencasa.features.notifications.domain.model.NotificationOpenResult
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.notifications.domain.usecase.OpenNotificationUseCase

class OpenNotificationViewModel(
    private val openNotification: OpenNotificationUseCase,
) : ViewModel() {
    suspend fun resolve(payload: PushPayload): NotificationDestination? =
        when (val result = openNotification(payload)) {
            is NotificationOpenResult.Open -> result.destination
            NotificationOpenResult.Dropped, is NotificationOpenResult.Failure -> null
        }
}

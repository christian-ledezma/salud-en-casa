package bo.saludencasa.features.notifications.presentation

import android.util.Log
import bo.saludencasa.di.ApplicationScope
import bo.saludencasa.features.notifications.domain.model.DeviceResult
import bo.saludencasa.features.notifications.domain.model.IncomingNotification
import bo.saludencasa.features.notifications.domain.usecase.RegisterDeviceUseCase
import bo.saludencasa.features.notifications.domain.usecase.ResolveIncomingNotificationUseCase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// The platform creates the service, so it cannot take its dependencies through
// a constructor; Koin hands them over instead.
class SaludEnCasaMessagingService :
    FirebaseMessagingService(),
    KoinComponent {
    private val applicationScope: CoroutineScope by inject(ApplicationScope)
    private val resolveIncoming: ResolveIncomingNotificationUseCase by inject()
    private val registerDevice: RegisterDeviceUseCase by inject()
    private val publisher: NotificationPublisher by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        applicationScope.launch {
            when (val incoming = resolveIncoming(message.data)) {
                is IncomingNotification.Show -> publisher.show(incoming.payload)
                is IncomingNotification.Discarded -> Log.d(TAG, "Notification discarded: ${incoming.reason}")
            }
        }
    }

    override fun onRegistered(installationId: String) {
        applicationScope.launch {
            val result = registerDevice(installationId)
            if (result is DeviceResult.Failure) Log.w(TAG, "Device not registered: ${result.error}")
        }
    }

    private companion object {
        const val TAG = "MessagingService"
    }
}

package bo.saludencasa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.notifications.presentation.toBundle
import bo.saludencasa.features.notifications.presentation.toPushPayload
import bo.saludencasa.navigation.SaludEnCasaNavHost
import bo.saludencasa.ui.theme.SaludEnCasaTheme

class MainActivity : ComponentActivity() {
    private var pendingNotification by mutableStateOf<PushPayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // docs/decisions.md, 2026-10-13, the startup screen.
        setTheme(R.style.Theme_SaludEnCasa)
        super.onCreate(savedInstanceState)
        // A recreated activity gets back its launch intent, whose tap may
        // already have been handled; the one still pending is in the state.
        pendingNotification =
            if (savedInstanceState == null) {
                intent.toPushPayload()
            } else {
                savedInstanceState.getBundle(PENDING_NOTIFICATION)?.toPushPayload()
            }
        enableEdgeToEdge()
        setContent {
            SaludEnCasaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SaludEnCasaNavHost(
                        modifier = Modifier.padding(innerPadding),
                        pendingNotification = pendingNotification,
                        onNotificationHandled = { pendingNotification = null },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingNotification?.let { outState.putBundle(PENDING_NOTIFICATION, it.toBundle()) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toPushPayload()?.let { pendingNotification = it }
    }

    private companion object {
        const val PENDING_NOTIFICATION = "pending_notification"
    }
}

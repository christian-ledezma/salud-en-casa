package bo.saludencasa.features.notifications.data.datasource

import android.content.Context
import androidx.core.app.NotificationManagerCompat

class ShownNotificationsDataSource(
    private val context: Context,
) {
    fun clearAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}

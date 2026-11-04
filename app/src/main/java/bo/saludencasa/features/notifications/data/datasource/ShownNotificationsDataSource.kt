package bo.saludencasa.features.notifications.data.datasource

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import bo.saludencasa.features.notifications.domain.model.NotificationSlot

class ShownNotificationsDataSource(
    private val context: Context,
) {
    fun clearAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun clear(
        requestId: String,
        slot: NotificationSlot,
    ) {
        NotificationManagerCompat.from(context).cancel(requestId, slot.ordinal)
    }
}

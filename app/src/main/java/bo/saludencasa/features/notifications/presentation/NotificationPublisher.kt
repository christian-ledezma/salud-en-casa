package bo.saludencasa.features.notifications.presentation

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import bo.saludencasa.R
import bo.saludencasa.features.notifications.domain.model.PushPayload

class NotificationPublisher(
    private val context: Context,
) {
    fun show(payload: PushPayload) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locale = context.resources.configuration.locales[0]
        val text = payload.text(context.resources, locale)
        val publicText = payload.publicText(context.resources)
        val channelId = payload.channel().id
        val tap =
            PendingIntent.getActivity(
                context,
                0,
                notificationIntent(context, payload),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val publicVersion =
            NotificationCompat
                .Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(publicText.title)
                .setContentText(publicText.body)
                .build()

        val notification =
            NotificationCompat
                .Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(text.title)
                .setContentText(text.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .build()

        manager.notify(payload.subject.requestId, NOTIFICATION_ID, notification)
    }

    private companion object {
        const val NOTIFICATION_ID = 0
    }
}

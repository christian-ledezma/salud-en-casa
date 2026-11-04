package bo.saludencasa.features.notifications.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import bo.saludencasa.R

// docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 7.
enum class NotificationChannelSpec(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
    val importance: Int,
) {
    REQUESTS(
        id = "requests",
        nameRes = R.string.notification_channel_requests_name,
        descriptionRes = R.string.notification_channel_requests_description,
        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
    ),

    // docs/decisions.md, 2026-11-03, HU-21: silenced apart from the requests.
    MESSAGES(
        id = "messages",
        nameRes = R.string.notification_channel_messages_name,
        descriptionRes = R.string.notification_channel_messages_description,
        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
    ),

    // docs/decisions.md, 2026-11-04, HU-23.
    ATTENTIONS(
        id = "attentions",
        nameRes = R.string.notification_channel_attentions_name,
        descriptionRes = R.string.notification_channel_attentions_description,
        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
    ),
    ;

    companion object {
        // Run at every start: creating an existing channel only refreshes its
        // name and description, which follows a change of the phone's language.
        fun registerAll(context: Context) {
            NotificationManagerCompat.from(context).createNotificationChannelsCompat(
                entries.map { spec ->
                    NotificationChannelCompat
                        .Builder(spec.id, spec.importance)
                        .setName(context.getString(spec.nameRes))
                        .setDescription(context.getString(spec.descriptionRes))
                        .build()
                },
            )
        }
    }
}

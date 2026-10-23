package bo.saludencasa.features.notifications.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import bo.saludencasa.MainActivity
import bo.saludencasa.features.notifications.domain.model.PushPayload

private const val ACTION_OPEN_NOTIFICATION = "bo.saludencasa.action.OPEN_NOTIFICATION"
private const val EXTRA_PREFIX = "push."

// The data URI is what keeps the pending intents of two requests apart:
// extras do not take part in Intent.filterEquals.
internal fun notificationIntent(
    context: Context,
    payload: PushPayload,
): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_NOTIFICATION
        data =
            Uri
                .Builder()
                .scheme("saludencasa")
                .authority("notification")
                .appendPath(payload.subject.requestId)
                .build()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        payload.toMap().forEach { (key, value) -> putExtra(EXTRA_PREFIX + key, value) }
    }

// Reopening the app from the recents screen hands back the intent that last
// launched it, which would replay a tap already handled.
fun Intent.toPushPayload(): PushPayload? {
    if (action != ACTION_OPEN_NOTIFICATION) return null
    if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
    val extras = extras ?: return null

    val data =
        extras
            .keySet()
            .filter { it.startsWith(EXTRA_PREFIX) }
            .associate { it.removePrefix(EXTRA_PREFIX) to extras.getString(it).orEmpty() }
    return PushPayload.parse(data).getOrNull()
}

fun PushPayload.toBundle(): Bundle = Bundle().apply { toMap().forEach { (key, value) -> putString(key, value) } }

fun Bundle.toPushPayload(): PushPayload? =
    PushPayload.parse(keySet().associateWith { getString(it).orEmpty() }).getOrNull()

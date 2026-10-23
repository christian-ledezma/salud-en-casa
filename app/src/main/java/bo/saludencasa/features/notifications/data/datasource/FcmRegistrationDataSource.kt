package bo.saludencasa.features.notifications.data.datasource

import com.google.firebase.messaging.FirebaseMessaging

// With firebase_messaging_installation_id_enabled the legacy token methods
// throw; register() answers through onRegistered, even when the installation
// id was already cached (docs/decisions.md, 2026-10-16).
class FcmRegistrationDataSource {
    fun register() {
        FirebaseMessaging.getInstance().register()
    }

    fun unregister() {
        FirebaseMessaging.getInstance().unregister()
    }
}

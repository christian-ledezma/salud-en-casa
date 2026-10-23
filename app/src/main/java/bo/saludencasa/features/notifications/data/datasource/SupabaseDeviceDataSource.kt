package bo.saludencasa.features.notifications.data.datasource

import bo.saludencasa.features.notifications.data.model.DeviceTokenParams
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class SupabaseDeviceDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // A notification can start the process, and then it arrives before the
    // stored session has been loaded back.
    suspend fun awaitUserId(): String? {
        withTimeoutOrNull(SESSION_RESTORE_TIMEOUT_MS) {
            supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        }
        return currentUserId()
    }

    suspend fun register(token: String) {
        supabase.postgrest.rpc(REGISTER_MY_DEVICE, DeviceTokenParams(token))
    }

    suspend fun unregister(token: String) {
        supabase.postgrest.rpc(UNREGISTER_MY_DEVICE, DeviceTokenParams(token))
    }

    private companion object {
        const val REGISTER_MY_DEVICE = "register_my_device"
        const val UNREGISTER_MY_DEVICE = "unregister_my_device"
        const val SESSION_RESTORE_TIMEOUT_MS = 3_000L
    }
}

package bo.saludencasa.features.subscription.data.datasource

import bo.saludencasa.features.subscription.data.model.SubscriptionDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class SupabaseSubscriptionDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // my_subscription is a definer pinned to auth.uid(): one row, always, with
    // the switch and the caller's latest period, if any.
    suspend fun fetchMine(): SubscriptionDto? =
        supabase.postgrest
            .rpc(MY_SUBSCRIPTION)
            .decodeList<SubscriptionDto>()
            .firstOrNull()

    private companion object {
        const val MY_SUBSCRIPTION = "my_subscription"
    }
}

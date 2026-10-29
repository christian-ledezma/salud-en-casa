package bo.saludencasa.features.services.data.datasource

import bo.saludencasa.features.services.data.model.AttentionDto
import bo.saludencasa.features.services.data.model.AttentionRequestIdParam
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

class SupabaseServiceDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // my_attention is a definer pinned to a party of the service; it reveals the
    // counterpart's contact, which the select_counterpart policies also allow once
    // the service exists, but builds the row in one audited place.
    suspend fun fetchAttention(requestId: String): AttentionDto? =
        supabase.postgrest
            .rpc(MY_ATTENTION, AttentionRequestIdParam(requestId))
            .decodeList<AttentionDto>()
            .firstOrNull()

    private companion object {
        const val MY_ATTENTION = "my_attention"
    }
}

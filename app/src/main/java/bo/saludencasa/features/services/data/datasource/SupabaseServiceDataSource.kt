package bo.saludencasa.features.services.data.datasource

import bo.saludencasa.core.network.broadcastSignals
import bo.saludencasa.features.services.data.model.ActiveAttentionParams
import bo.saludencasa.features.services.data.model.AttentionDto
import bo.saludencasa.features.services.data.model.AttentionRequestIdParam
import bo.saludencasa.features.services.data.model.AttentionSummaryDto
import bo.saludencasa.features.services.data.model.MyAttentionsParams
import bo.saludencasa.features.services.data.model.ServiceStatusRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow

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

    suspend fun fetchMyAttentions(
        role: String,
        offset: Int,
        limit: Int,
    ): List<AttentionSummaryDto> =
        supabase.postgrest
            .rpc(MY_ATTENTIONS, MyAttentionsParams(role = role, limit = limit, offset = offset))
            .decodeList<AttentionSummaryDto>()

    suspend fun fetchActiveAttention(role: String): AttentionSummaryDto? =
        supabase.postgrest
            .rpc(MY_ACTIVE_ATTENTION, ActiveAttentionParams(role))
            .decodeList<AttentionSummaryDto>()
            .firstOrNull()

    // No dates: stamp_service_progress owns them. Returns how many rows the policy
    // let through, which is none for someone who is not a party of the service.
    suspend fun updateStatus(
        requestId: String,
        status: String,
    ): Int =
        supabase
            .from(SERVICES)
            .update({ set("status", status) }) {
                filter { eq("request_id", requestId) }
                select(Columns.list("status"))
            }.decodeList<ServiceStatusRow>()
            .size

    // Neither the author nor the date: stamp_service_progress owns them.
    suspend fun cancel(
        requestId: String,
        reason: String,
    ): Int =
        supabase
            .from(SERVICES)
            .update({
                set("status", CANCELLED)
                set("cancellation_reason", reason)
            }) {
                filter { eq("request_id", requestId) }
                select(Columns.list("status"))
            }.decodeList<ServiceStatusRow>()
            .size

    // notify_attention_changed sends to this topic, and attention_listen lets only
    // the two parties of the service hear it.
    fun signals(requestId: String): Flow<Unit> = supabase.broadcastSignals(TOPIC_PREFIX + requestId, EVENT)

    private companion object {
        const val SERVICES = "services"
        const val CANCELLED = "CANCELLED"
        const val TOPIC_PREFIX = "attention:"
        const val EVENT = "attention_changed"
        const val MY_ATTENTION = "my_attention"
        const val MY_ATTENTIONS = "my_attentions"
        const val MY_ACTIVE_ATTENTION = "my_active_attention"
    }
}

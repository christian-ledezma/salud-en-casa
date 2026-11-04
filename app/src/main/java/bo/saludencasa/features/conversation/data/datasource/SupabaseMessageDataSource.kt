package bo.saludencasa.features.conversation.data.datasource

import bo.saludencasa.core.network.broadcastSignals
import bo.saludencasa.features.conversation.data.model.ConversationPageParams
import bo.saludencasa.features.conversation.data.model.ConversationRequestIdParam
import bo.saludencasa.features.conversation.data.model.MessageDto
import bo.saludencasa.features.conversation.data.model.NewMessageRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow

class SupabaseMessageDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // conversation_page walks the index with a row comparison, which a PostgREST
    // filter cannot write (docs/decisions.md, 2026-11-03, HU-21).
    suspend fun fetchPage(params: ConversationPageParams): List<MessageDto> =
        supabase.postgrest
            .rpc(CONVERSATION_PAGE, params)
            .decodeList<MessageDto>()

    suspend fun insert(row: NewMessageRow): MessageDto =
        supabase
            .from(MESSAGES)
            .insert(row) { select(COLUMNS) }
            .decodeSingle<MessageDto>()

    suspend fun fetchOne(messageId: String): MessageDto? =
        supabase
            .from(MESSAGES)
            .select(COLUMNS) { filter { eq("id", messageId) } }
            .decodeList<MessageDto>()
            .firstOrNull()

    suspend fun markRead(requestId: String) {
        supabase.postgrest.rpc(MARK_CONVERSATION_READ, ConversationRequestIdParam(requestId))
    }

    // notify_conversation_message and notify_conversation_read send to this topic,
    // and conversation_listen lets only the two parties of the service hear it.
    fun signals(requestId: String): Flow<Unit> = supabase.broadcastSignals(TOPIC_PREFIX + requestId, EVENT)

    private companion object {
        const val MESSAGES = "messages"
        const val CONVERSATION_PAGE = "conversation_page"
        const val MARK_CONVERSATION_READ = "mark_conversation_read"
        const val TOPIC_PREFIX = "conversation:"
        const val EVENT = "conversation_changed"
        val COLUMNS = Columns.list("id", "sender_id", "content", "read_at", "created_at", "is_work_record")
    }
}

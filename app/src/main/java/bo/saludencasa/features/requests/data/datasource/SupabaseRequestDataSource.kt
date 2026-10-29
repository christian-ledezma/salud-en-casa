package bo.saludencasa.features.requests.data.datasource

import bo.saludencasa.features.requests.data.model.BookableStartDto
import bo.saludencasa.features.requests.data.model.BookableTimesParams
import bo.saludencasa.features.requests.data.model.CreateImmediateRequestParams
import bo.saludencasa.features.requests.data.model.CreateScheduledRequestParams
import bo.saludencasa.features.requests.data.model.InboxRequestDto
import bo.saludencasa.features.requests.data.model.RequestInboxParams
import bo.saludencasa.features.requests.data.model.ServiceRequestDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class SupabaseRequestDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // The function returns the new identifier as a bare uuid, decoded the same
    // way add_my_role is.
    suspend fun createImmediate(params: CreateImmediateRequestParams): String =
        supabase.postgrest
            .rpc(CREATE_IMMEDIATE, params)
            .decodeAs<String>()

    suspend fun createScheduled(params: CreateScheduledRequestParams): String =
        supabase.postgrest
            .rpc(CREATE_SCHEDULED, params)
            .decodeAs<String>()

    // Not paged: fourteen days at most by construction (RNF-08).
    suspend fun fetchBookableTimes(params: BookableTimesParams): List<BookableStartDto> =
        supabase.postgrest
            .rpc(BOOKABLE_START_TIMES, params)
            .decodeList<BookableStartDto>()

    // my_service_requests is a definer view whose WHERE pins every row to the
    // caller, so no patient filter is added on this side.
    suspend fun findMyRequests(
        offset: Int,
        limit: Int,
    ): List<ServiceRequestDto> =
        supabase
            .from(MY_SERVICE_REQUESTS)
            .select(COLUMNS) {
                order("created_at", Order.DESCENDING)
                // id breaks ties so two requests created in the same instant
                // cannot swap places between pages.
                order("id", Order.ASCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }.decodeList<ServiceRequestDto>()

    // docs/decisions.md, 2026-10-23, HT-23: filtered here and not by reading the
    // first page of findMyRequests.
    suspend fun findActiveImmediate(): ServiceRequestDto? =
        supabase
            .from(MY_SERVICE_REQUESTS)
            .select(COLUMNS) {
                filter {
                    eq("modality", "IMMEDIATE")
                    isIn("status", listOf("PUBLISHED", "NEGOTIATING"))
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeList<ServiceRequestDto>()
            .firstOrNull()

    // A direct status write, allowed for the owning patient only from PUBLISHED
    // or NEGOTIATING to CANCELLED by guard_request_lifecycle (RF-07.6). The
    // column builder writes straight into the request body, so it does not drop
    // a field that happens to match a serializable default.
    suspend fun cancel(requestId: String) {
        supabase
            .from(SERVICE_REQUESTS)
            .update({ set("status", "CANCELLED") }) {
                filter { eq("id", requestId) }
            }
    }

    // HU-14 and HU-16: the inbox projection. request_inbox is security definer
    // and folds in the coverage, the addressee, the type, the status and the
    // time guards, so the client adds no filter; it sends the page bounds and
    // reads back the rounded distance without any of the patient's location.
    suspend fun fetchInbox(
        offset: Int,
        limit: Int,
    ): List<InboxRequestDto> =
        supabase.postgrest
            .rpc(REQUEST_INBOX, RequestInboxParams(limit = limit, offset = offset))
            .decodeList<InboxRequestDto>()

    // A private broadcast channel: the notify_immediate_inbox trigger sends an
    // empty "inbox_changed" to the shared immediate_inbox topic, and the
    // realtime.messages policy lets only an approved professional receive it. The
    // payload carries nothing, so the flow is mapped to Unit; the caller re-reads
    // fetchInbox on each emission. subscribe() does not block, so joining in
    // onStart runs after the broadcast callback is registered on collection.
    fun inboxSignals(): Flow<Unit> {
        val channel = supabase.realtime.channel(INBOX_TOPIC) { isPrivate = true }

        return channel
            .broadcastFlow<JsonObject>(INBOX_EVENT)
            .map { }
            .onStart { channel.subscribe() }
            .onCompletion {
                // The collection ends by cancellation, and a suspending call in a
                // cancelled coroutine throws before it does anything: without
                // NonCancellable the channel would never be left.
                withContext(NonCancellable) {
                    runCatching { channel.unsubscribe() }
                    runCatching { supabase.realtime.removeChannel(channel) }
                }
            }
    }

    private companion object {
        const val CREATE_IMMEDIATE = "create_immediate_request"
        const val CREATE_SCHEDULED = "create_scheduled_request"
        const val BOOKABLE_START_TIMES = "bookable_start_times"
        const val REQUEST_INBOX = "request_inbox"
        const val MY_SERVICE_REQUESTS = "my_service_requests"
        const val SERVICE_REQUESTS = "service_requests"
        const val INBOX_TOPIC = "immediate_inbox"
        const val INBOX_EVENT = "inbox_changed"

        val COLUMNS: Columns =
            Columns.list(
                "id",
                "service_type_name",
                "address_text",
                "city",
                "modality",
                "status",
                "description",
                "suggested_budget_bob",
                "expires_at",
                "created_at",
                "scheduled_at",
                "professional_name",
                "professional_id",
            )
    }
}

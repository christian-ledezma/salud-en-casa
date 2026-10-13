package bo.saludencasa.features.requests.data.datasource

import bo.saludencasa.features.requests.data.model.CreateImmediateRequestParams
import bo.saludencasa.features.requests.data.model.ServiceRequestDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc

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

    // my_service_requests is security_invoker, so service_requests_select_own
    // decides which rows come back; no patient filter is added on this side.
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

    private companion object {
        const val CREATE_IMMEDIATE = "create_immediate_request"
        const val MY_SERVICE_REQUESTS = "my_service_requests"
        const val SERVICE_REQUESTS = "service_requests"

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
            )
    }
}

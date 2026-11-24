package bo.saludencasa.features.disputes.data.datasource

import bo.saludencasa.features.disputes.data.model.PaymentDisputeDto
import bo.saludencasa.features.disputes.data.model.PaymentStatusRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

class SupabaseDisputeDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun fetchDisputes(
        offset: Long,
        limit: Long,
    ): List<PaymentDisputeDto> =
        supabase
            .from(QUEUE)
            .select(COLUMNS) {
                order("disputed_at", Order.ASCENDING)
                // payment_id breaks ties so two disputes opened at the same
                // instant cannot swap places between pages.
                order("payment_id", Order.ASCENDING)
                range(offset, offset + limit - 1)
            }.decodeList<PaymentDisputeDto>()

    suspend fun fetchDispute(paymentId: String): PaymentDisputeDto? =
        supabase
            .from(QUEUE)
            .select(COLUMNS) {
                filter { eq("payment_id", paymentId) }
            }.decodeSingleOrNull<PaymentDisputeDto>()

    // Only the status travels: stamp_payment_confirmation seals who and when.
    // The DISPUTED filter makes a dispute resolved meanwhile update no row
    // instead of reaching the guard as a second resolution.
    suspend fun resolve(
        paymentId: String,
        status: String,
    ): Int =
        supabase
            .from(PAYMENTS)
            .update({ set("status", status) }) {
                filter {
                    eq("id", paymentId)
                    eq("status", DISPUTED)
                }
                select(Columns.list("status"))
            }.decodeList<PaymentStatusRow>()
            .size

    private companion object {
        const val QUEUE = "payment_dispute_queue"
        const val PAYMENTS = "payments"
        const val DISPUTED = "DISPUTED"
        val COLUMNS =
            Columns.list(
                "payment_id",
                "request_id",
                "service_type_name",
                "total_amount_bob",
                "method",
                "finished_at",
                "patient_confirmed_at",
                "disputed_at",
                "patient_name",
                "patient_email",
                "professional_name",
                "professional_email",
            )
    }
}

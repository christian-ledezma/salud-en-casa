package bo.saludencasa.features.availability.data.datasource

import bo.saludencasa.features.availability.data.model.AvailabilitySlotDto
import bo.saludencasa.features.availability.data.model.AvailabilitySlotRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

class SupabaseAvailabilityDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // availability_slots_select_own already filters to the caller: the client
    // adds no professional_id predicate on top. Ordering here only shapes the
    // response for the view model, which groups and re-orders anyway.
    suspend fun findMySlots(): List<AvailabilitySlotDto> =
        supabase
            .from(AVAILABILITY_SLOTS)
            .select(COLUMNS) {
                order("day_of_week", Order.ASCENDING)
                order("start_time", Order.ASCENDING)
                limit(MAX_SLOTS)
            }.decodeList<AvailabilitySlotDto>()

    suspend fun insertSlot(row: AvailabilitySlotRow) {
        supabase
            .from(AVAILABILITY_SLOTS)
            .insert(row)
    }

    suspend fun deleteSlot(slotId: String) {
        supabase
            .from(AVAILABILITY_SLOTS)
            .delete {
                filter { eq("id", slotId) }
            }
    }

    private companion object {
        const val AVAILABILITY_SLOTS = "availability_slots"

        val COLUMNS: Columns = Columns.list("id", "day_of_week", "start_time", "end_time")

        // A professional declaring 7 days * dozens of slots is already an
        // outlier; a hundred fits one page with slack to spare.
        const val MAX_SLOTS = 200L
    }
}

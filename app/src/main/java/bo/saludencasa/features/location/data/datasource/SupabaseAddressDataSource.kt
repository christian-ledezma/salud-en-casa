package bo.saludencasa.features.location.data.datasource

import bo.saludencasa.features.location.data.model.AddressDto
import bo.saludencasa.features.location.data.model.AddressRow
import bo.saludencasa.features.location.data.model.InsertedAddressDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

class SupabaseAddressDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // Narrowing by profile in the client would be the condition INV-13 says
    // never to rely on: addresses_select_own decides whose rows the view
    // returns, and my_addresses is security_invoker precisely so that policy
    // still applies to it. The primary address sorts first, and the limit
    // keeps this the same kind of bounded query as every other one (RNF-08),
    // even though a person's own addresses were never going to be many.
    suspend fun findAllAddresses(): List<AddressDto> =
        supabase
            .from("my_addresses")
            .select {
                order("is_primary", Order.DESCENDING)
                order("alias", Order.ASCENDING)
                limit(MAX_ADDRESSES)
            }.decodeList<AddressDto>()

    suspend fun findAddress(id: String): AddressDto? =
        supabase
            .from("my_addresses")
            .select {
                filter { eq("id", id) }
            }.decodeSingleOrNull<AddressDto>()

    suspend fun insertAddress(row: AddressRow): String =
        supabase
            .from("addresses")
            .insert(row) {
                select(Columns.list("id"))
            }.decodeSingle<InsertedAddressDto>()
            .id

    suspend fun updateAddress(
        id: String,
        row: AddressRow,
    ) {
        supabase
            .from("addresses")
            .update(row) {
                filter { eq("id", id) }
            }
    }

    // The column-builder update writes straight into a JsonElement map, so it
    // never goes through kotlinx.serialization's encodeDefaults. A data-class
    // body would have: see docs/decisions.md, 2026-09-16, "Marcar como
    // principal no escribía nada".
    suspend fun setPrimary(id: String) {
        supabase
            .from("addresses")
            .update({ set("is_primary", true) }) {
                filter { eq("id", id) }
            }
    }

    suspend fun deleteAddress(id: String) {
        supabase
            .from("addresses")
            .delete {
                filter { eq("id", id) }
            }
    }

    private companion object {
        const val MAX_ADDRESSES = 50L
    }
}

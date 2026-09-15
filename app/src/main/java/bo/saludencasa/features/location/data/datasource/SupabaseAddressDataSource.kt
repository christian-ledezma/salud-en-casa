package bo.saludencasa.features.location.data.datasource

import bo.saludencasa.features.location.data.model.AddressDto
import bo.saludencasa.features.location.data.model.AddressRow
import bo.saludencasa.features.location.data.model.InsertedAddressDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class SupabaseAddressDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // Only the primary flag is filtered here. Narrowing by profile in the client
    // would be the condition INV-13 says never to rely on: addresses_select_own
    // decides whose rows the view returns, and my_addresses is security_invoker
    // precisely so that policy still applies to it.
    suspend fun findPrimaryAddress(): AddressDto? =
        supabase
            .from("my_addresses")
            .select {
                filter { eq("is_primary", true) }
            }.decodeSingleOrNull<AddressDto>()

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
}

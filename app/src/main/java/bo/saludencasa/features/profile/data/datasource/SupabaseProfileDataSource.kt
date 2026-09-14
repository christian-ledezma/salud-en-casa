package bo.saludencasa.features.profile.data.datasource

import bo.saludencasa.features.profile.data.model.AssignRoleParams
import bo.saludencasa.features.profile.data.model.ProfileRoleDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc

class SupabaseProfileDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun findRole(userId: String): ProfileRoleDto? =
        supabase
            .from("profiles")
            .select(Columns.list("role")) {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileRoleDto>()

    suspend fun assignRole(role: String): String =
        supabase.postgrest
            .rpc("assign_my_role", AssignRoleParams(role = role))
            .decodeAs<String>()
}

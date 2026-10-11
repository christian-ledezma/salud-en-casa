package bo.saludencasa.features.search.data.datasource

import bo.saludencasa.features.search.data.model.NearbyProfessionalDto
import bo.saludencasa.features.search.data.model.NearbySearchParams
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

class SupabaseProfessionalSearchDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun searchNearby(params: NearbySearchParams): List<NearbyProfessionalDto> =
        supabase.postgrest
            .rpc(SEARCH_NEARBY, params)
            .decodeList<NearbyProfessionalDto>()

    private companion object {
        const val SEARCH_NEARBY = "search_nearby_professionals"
    }
}

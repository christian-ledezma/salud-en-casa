package bo.saludencasa.features.profile.data.datasource

import bo.saludencasa.features.profile.data.model.AssignRoleParams
import bo.saludencasa.features.profile.data.model.PatientDto
import bo.saludencasa.features.profile.data.model.ProfessionalDto
import bo.saludencasa.features.profile.data.model.ProfileDto
import bo.saludencasa.features.profile.data.model.ProfileRoleDto
import bo.saludencasa.features.profile.data.model.PublicProfileDto
import bo.saludencasa.features.profile.data.model.SaveProfileParams
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

    suspend fun findProfile(userId: String): ProfileDto? =
        supabase
            .from("profiles")
            .select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileDto>()

    suspend fun findPatient(userId: String): PatientDto? =
        supabase
            .from("patients")
            .select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<PatientDto>()

    suspend fun findProfessional(userId: String): ProfessionalDto? =
        supabase
            .from("professionals")
            .select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfessionalDto>()

    suspend fun findPublicProfile(professionalId: String): PublicProfileDto? =
        supabase
            .from("professional_directory")
            .select {
                filter { eq("id", professionalId) }
            }.decodeSingleOrNull<PublicProfileDto>()

    suspend fun saveProfile(params: SaveProfileParams) {
        supabase.postgrest.rpc("save_my_profile", params)
    }

    suspend fun setAvailableNow(
        userId: String,
        availableNow: Boolean,
    ) {
        supabase
            .from("professionals")
            .update({ set("available_now", availableNow) }) {
                filter { eq("id", userId) }
            }
    }
}

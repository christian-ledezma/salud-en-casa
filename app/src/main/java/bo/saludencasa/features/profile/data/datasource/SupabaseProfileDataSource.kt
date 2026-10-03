package bo.saludencasa.features.profile.data.datasource

import bo.saludencasa.features.profile.data.model.AddRoleParams
import bo.saludencasa.features.profile.data.model.PatientDto
import bo.saludencasa.features.profile.data.model.ProfessionalDto
import bo.saludencasa.features.profile.data.model.ProfileDto
import bo.saludencasa.features.profile.data.model.ProfileRolesDto
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

    suspend fun findRoles(userId: String): ProfileRolesDto? =
        supabase
            .from("my_roles")
            .select(Columns.list("active_role", "held_roles")) {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileRolesDto>()

    suspend fun addRole(role: String): String =
        supabase.postgrest
            .rpc("add_my_role", AddRoleParams(role = role))
            .decodeAs<String>()

    // A direct update rather than a stored function: one column of one table has
    // nothing that can be left half done, and the composite foreign key already
    // refuses a role the person does not hold (docs/decisions.md, 2026-10-01).
    //
    // Written with set() and not with a serializable row. install(Postgrest)
    // does not receive encodeDefaults = true, so a field whose value matches its
    // default disappears from the body and the PATCH that travels is {} -- the
    // defect of 2026-09-16, which cost a working feature and a day to find.
    suspend fun setActiveRole(
        userId: String,
        role: String,
    ) {
        supabase
            .from("profiles")
            .update({ set("active_role", role) }) {
                filter { eq("id", userId) }
            }
    }

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

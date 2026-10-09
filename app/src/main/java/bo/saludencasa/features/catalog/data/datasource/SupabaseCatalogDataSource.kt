package bo.saludencasa.features.catalog.data.datasource

import bo.saludencasa.features.catalog.data.model.ProfessionalServiceDto
import bo.saludencasa.features.catalog.data.model.ProfessionalServiceRow
import bo.saludencasa.features.catalog.data.model.ServiceTypeDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.math.BigDecimal

class SupabaseCatalogDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // No active filter: service_types_select_all already returns only the
    // active entries, and repeating the condition here would be the second
    // copy of a rule the policy owns.
    suspend fun findServiceTypes(): List<ServiceTypeDto> =
        supabase
            .from(SERVICE_TYPES)
            .select(TYPE_COLUMNS) {
                order("name", Order.ASCENDING)
                limit(MAX_SERVICE_TYPES)
            }.decodeList<ServiceTypeDto>()

    // Narrowed by professional on this side, unlike the addresses read.
    // professional_services_select_public makes every approved professional's
    // rows readable on purpose (RF-02.6), so without the filter this would
    // return the whole directory's services instead of the caller's.
    suspend fun findMyServices(professionalId: String): List<ProfessionalServiceDto> =
        supabase
            .from(PROFESSIONAL_SERVICES)
            .select(SERVICE_COLUMNS) {
                filter { eq("professional_id", professionalId) }
                limit(MAX_SERVICE_TYPES)
            }.decodeList<ProfessionalServiceDto>()

    suspend fun insertService(row: ProfessionalServiceRow) {
        supabase
            .from(PROFESSIONAL_SERVICES)
            .insert(row)
    }

    // Written with the column builder for the reason given in
    // SupabaseProfileDataSource: a serializable body drops a field whose value
    // matches its default. The amount leaves as the literal text of the number,
    // which the engine casts to numeric (docs/decisions.md, 2026-09-14).
    suspend fun updateServicePrice(
        serviceId: String,
        price: BigDecimal,
    ) {
        supabase
            .from(PROFESSIONAL_SERVICES)
            .update({ set("reference_price_bob", price.toPlainString()) }) {
                filter { eq("id", serviceId) }
            }
    }

    suspend fun deleteService(serviceId: String) {
        supabase
            .from(PROFESSIONAL_SERVICES)
            .delete {
                filter { eq("id", serviceId) }
            }
    }

    private companion object {
        const val SERVICE_TYPES = "service_types"
        const val PROFESSIONAL_SERVICES = "professional_services"

        val TYPE_COLUMNS: Columns =
            Columns.list("id", "name", "description", "reference_price_bob", "estimated_duration_min")

        val SERVICE_COLUMNS: Columns = Columns.list("id", "service_type_id", "reference_price_bob")

        // The catalog is platform reference data and a professional can declare
        // each entry once, so one page holds both reads (RNF-08).
        const val MAX_SERVICE_TYPES = 100L
    }
}

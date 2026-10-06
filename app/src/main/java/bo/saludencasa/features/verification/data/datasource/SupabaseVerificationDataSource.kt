package bo.saludencasa.features.verification.data.datasource

import bo.saludencasa.features.verification.data.model.VerificationDocumentDto
import bo.saludencasa.features.verification.data.model.VerificationDocumentRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType

class SupabaseVerificationDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun findMyDocuments(userId: String): List<VerificationDocumentDto> =
        supabase
            .from(TABLE)
            .select(COLUMNS) {
                filter { eq("profile_id", userId) }
                order("created_at", Order.ASCENDING)
                limit(MAX_DOCUMENTS)
            }.decodeList<VerificationDocumentDto>()

    suspend fun upsertDocumentRow(row: VerificationDocumentRow) {
        supabase
            .from(TABLE)
            .upsert(row) {
                onConflict = "profile_id,document_type"
            }
    }

    suspend fun uploadToBucket(
        storagePath: String,
        bytes: ByteArray,
    ) {
        supabase.storage
            .from(BUCKET)
            .upload(storagePath, bytes) {
                contentType = ContentType.Image.JPEG
                upsert = true
            }
    }

    private companion object {
        const val BUCKET = "verification-documents"
        const val TABLE = "verification_documents"

        val COLUMNS: Columns =
            Columns.list("document_type", "status", "storage_path", "caption", "rejection_reason")

        // Six fixed types plus up to one OTHER per person. A pageful fits
        // comfortably under RNF-08's bounded-query rule.
        const val MAX_DOCUMENTS = 20L
    }
}

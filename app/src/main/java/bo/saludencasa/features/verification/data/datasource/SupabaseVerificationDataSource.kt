package bo.saludencasa.features.verification.data.datasource

import bo.saludencasa.features.verification.data.model.ApproveProfessionalParams
import bo.saludencasa.features.verification.data.model.DocumentReviewSubjectDto
import bo.saludencasa.features.verification.data.model.PendingReviewSubjectDto
import bo.saludencasa.features.verification.data.model.VerificationDocumentDto
import bo.saludencasa.features.verification.data.model.VerificationDocumentRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlin.time.Duration.Companion.minutes

class SupabaseVerificationDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun findDocumentsOf(profileId: String): List<VerificationDocumentDto> =
        supabase
            .from(TABLE)
            .select(COLUMNS) {
                filter { eq("profile_id", profileId) }
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

    suspend fun findPendingSubjects(
        term: String?,
        role: String?,
        newestFirst: Boolean,
        offset: Long,
        limit: Long,
    ): List<PendingReviewSubjectDto> =
        supabase
            .from(SUBJECT_QUEUE_VIEW)
            .select(QUEUE_COLUMNS) {
                filter {
                    // The star is the wildcard PostgREST understands; a percent
                    // sign would reach the engine as a literal.
                    if (term != null) ilike("search_text", "*$term*")
                    if (role != null) contains("held_roles", listOf(role))
                }
                order(
                    "waiting_since",
                    if (newestFirst) Order.DESCENDING else Order.ASCENDING,
                )
                // profile_id breaks ties so two people who started waiting at
                // the same instant cannot swap places between pages.
                order("profile_id", Order.ASCENDING)
                range(offset, offset + limit - 1)
            }.decodeList<PendingReviewSubjectDto>()

    suspend fun findReviewSubject(profileId: String): DocumentReviewSubjectDto? =
        supabase
            .from(SUBJECT_VIEW)
            .select(
                Columns.list(
                    "id",
                    "full_name",
                    "email",
                    "held_roles",
                    "professional_type",
                    "professional_verification_status",
                ),
            ) {
                filter { eq("id", profileId) }
            }.decodeSingleOrNull<DocumentReviewSubjectDto>()

    // The engine stamps reviewed_by and reviewed_at, so only the verdict travels.
    // Written with set() for the reason given in SupabaseProfileDataSource. The
    // row comes back so that a policy refusal, which updates zero rows without
    // raising, is not mistaken for a success.
    suspend fun writeVerdict(
        profileId: String,
        documentTypes: List<String>,
        status: String,
        rejectionReason: String?,
    ): Int =
        supabase
            .from(TABLE)
            .update({
                set("status", status)
                if (rejectionReason != null) set("rejection_reason", rejectionReason)
            }) {
                select(COLUMNS)
                filter {
                    eq("profile_id", profileId)
                    isIn("document_type", documentTypes)
                }
            }.decodeList<VerificationDocumentDto>()
            .size

    suspend fun approveProfessional(profileId: String) {
        supabase.postgrest.rpc("approve_professional_verification", ApproveProfessionalParams(profileId))
    }

    suspend fun createSignedUrl(storagePath: String): String =
        supabase.storage
            .from(BUCKET)
            .createSignedUrl(storagePath, SIGNED_URL_LIFETIME)

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
        const val SUBJECT_QUEUE_VIEW = "document_review_subject_queue"
        const val SUBJECT_VIEW = "document_review_profiles"

        // docs/decisions.md, 2026-10-06, signed URL lifetime.
        val SIGNED_URL_LIFETIME = 5.minutes

        val QUEUE_COLUMNS: Columns =
            Columns.list(
                "profile_id",
                "full_name",
                "email",
                "pending_count",
                "awaiting_verification",
                "waiting_since",
            )

        val COLUMNS: Columns =
            Columns.list("document_type", "status", "storage_path", "caption", "rejection_reason")

        // Six fixed types plus up to one OTHER per person. A pageful fits
        // comfortably under RNF-08's bounded-query rule.
        const val MAX_DOCUMENTS = 20L
    }
}

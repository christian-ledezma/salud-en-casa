package bo.saludencasa.features.reviews.data.datasource

import bo.saludencasa.features.reviews.data.model.ReviewDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

class SupabaseReviewDataSource(
    private val supabase: SupabaseClient,
) {
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // docs/decisions.md, 2026-10-11, the comments are read from a projection.
    suspend fun findReviewsOf(
        professionalId: String,
        offset: Int,
        limit: Int,
    ): List<ReviewDto> =
        supabase
            .from(VIEW)
            .select(COLUMNS) {
                filter { eq("recipient_id", professionalId) }
                order("created_at", Order.DESCENDING)
                // id breaks ties so two comments written in the same instant
                // cannot swap places between pages.
                order("id", Order.ASCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }.decodeList<ReviewDto>()

    private companion object {
        const val VIEW = "professional_reviews"
        val COLUMNS = Columns.list("id", "rating", "comment", "created_at")
    }
}

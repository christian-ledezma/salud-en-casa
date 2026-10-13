package bo.saludencasa.features.reviews.domain.model

import java.time.Instant

// Without its author on purpose: the public profile shows what was said and
// when, never who said it (docs/decisions.md, 2026-10-11).
data class ProfessionalReview(
    val id: String,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
)

package bo.saludencasa.features.reviews.data.mapper

import bo.saludencasa.features.reviews.data.model.ReviewDto
import bo.saludencasa.features.reviews.domain.model.ProfessionalReview
import java.time.OffsetDateTime

internal fun ReviewDto.toProfessionalReview(): ProfessionalReview? {
    // PostgREST sends the offset as +00:00, which Instant.parse does not take.
    val created = runCatching { OffsetDateTime.parse(createdAt).toInstant() }.getOrNull() ?: return null

    return ProfessionalReview(
        id = id,
        rating = rating,
        // An empty comment is a rating with no words, which the card draws as
        // the stars alone rather than as a blank line.
        comment = comment?.takeIf { it.isNotBlank() },
        createdAt = created,
    )
}

package bo.saludencasa.features.verification.data.mapper

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.data.model.DocumentReviewSubjectDto
import bo.saludencasa.features.verification.data.model.PendingDocumentReviewDto
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubject
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.PendingDocumentReview
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import java.time.OffsetDateTime

// docs/decisions.md, 2026-10-06, queue pagination.
internal fun PendingDocumentReviewDto.toPendingReview(): PendingDocumentReview? {
    val type = DocumentType.entries.firstOrNull { it.name == documentType } ?: return null
    // PostgREST sends the offset as +00:00, which Instant.parse does not take.
    val created = runCatching { OffsetDateTime.parse(createdAt).toInstant() }.getOrNull() ?: return null
    return PendingDocumentReview(
        profileId = profileId,
        type = type,
        caption = caption,
        createdAt = created,
        fullName = fullName,
        email = email,
    )
}

internal fun DocumentReviewSubjectDto.toSubject(): DocumentReviewSubject? {
    val roles = heldRoles.map { name -> UserRole.entries.firstOrNull { it.name == name } ?: return null }.toSet()
    val type = professionalType?.let { name -> ProfessionalType.entries.firstOrNull { it.name == name } ?: return null }
    val status = professionalStatus?.let { name -> ReviewStatus.entries.firstOrNull { it.name == name } ?: return null }
    return DocumentReviewSubject(
        profileId = id,
        fullName = fullName,
        email = email,
        heldRoles = roles,
        professionalType = type,
        professionalStatus = status,
    )
}

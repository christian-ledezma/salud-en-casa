package bo.saludencasa.features.verification.data.mapper

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.data.model.DocumentReviewSubjectDto
import bo.saludencasa.features.verification.data.model.PendingReviewSubjectDto
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubject
import bo.saludencasa.features.verification.domain.model.PendingReviewSubject
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import java.time.OffsetDateTime

// docs/decisions.md, 2026-10-08, the queue lists people.
internal fun PendingReviewSubjectDto.toPendingSubject(): PendingReviewSubject? {
    // PostgREST sends the offset as +00:00, which Instant.parse does not take.
    val since = runCatching { OffsetDateTime.parse(waitingSince).toInstant() }.getOrNull() ?: return null
    return PendingReviewSubject(
        profileId = profileId,
        fullName = fullName,
        email = email,
        pendingCount = pendingCount,
        awaitingVerification = awaitingVerification,
        waitingSince = since,
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

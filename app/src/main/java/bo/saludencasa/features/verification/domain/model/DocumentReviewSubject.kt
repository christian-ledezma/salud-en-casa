package bo.saludencasa.features.verification.domain.model

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole

data class DocumentReviewSubject(
    val profileId: String,
    val fullName: String,
    val email: String,
    val heldRoles: Set<UserRole>,
    val professionalType: ProfessionalType?,
    val professionalStatus: ReviewStatus?,
)

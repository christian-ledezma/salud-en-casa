package bo.saludencasa.features.verification.domain.model

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileRoles
import bo.saludencasa.features.profile.domain.model.UserRole

// Union of role-based document sets. See docs/decisions.md, 2026-10-01 (held
// roles, not active) and 2026-10-05 (OTHER as the always-present optional slot).
data class VerificationChecklist(
    val required: List<DocumentType>,
    val documents: Map<DocumentType, VerificationDocument>,
) {
    fun documentFor(type: DocumentType): VerificationDocument? = documents[type]

    companion object {
        fun create(
            roles: ProfileRoles,
            professionalType: ProfessionalType?,
            documents: List<VerificationDocument>,
        ): VerificationChecklist =
            VerificationChecklist(
                required = requiredFor(roles, professionalType),
                documents = documents.associateBy { it.type },
            )

        private fun requiredFor(
            roles: ProfileRoles,
            professionalType: ProfessionalType?,
        ): List<DocumentType> =
            buildList {
                val patient = roles.has(UserRole.PATIENT)
                val professional = roles.has(UserRole.PROFESSIONAL)
                if (patient || professional) {
                    add(DocumentType.ID_FRONT)
                    add(DocumentType.ID_BACK)
                    add(DocumentType.SELFIE)
                }
                if (professional) {
                    add(DocumentType.DEGREE)
                    // A professional who has not declared their type yet is
                    // asked for both: the slot the admin will review must not
                    // be missing from the first open.
                    when (professionalType) {
                        ProfessionalType.STUDENT -> {
                            add(DocumentType.STUDENT_CARD)
                        }

                        ProfessionalType.DOCTOR,
                        ProfessionalType.NURSE,
                        ProfessionalType.PHYSIOTHERAPIST,
                        -> {
                            add(DocumentType.LICENSE)
                        }

                        null -> {
                            add(DocumentType.LICENSE)
                            add(DocumentType.STUDENT_CARD)
                        }
                    }
                }
            }
    }
}

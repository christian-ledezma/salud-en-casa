package bo.saludencasa.features.verification.domain.model

import bo.saludencasa.features.profile.domain.model.UserRole

data class DocumentReviewDossier(
    val subject: DocumentReviewSubject,
    val checklist: VerificationChecklist,
) {
    val reviewableTypes: List<DocumentType>
        get() = checklist.required + listOfNotNull(DocumentType.OTHER.takeIf { checklist.documentFor(it) != null })

    // A prediction used to offer the button, never a decision: the engine checks
    // the same thing in approve_professional_verification and has the last word.
    val canApproveProfessional: Boolean
        get() =
            UserRole.PROFESSIONAL in subject.heldRoles &&
                subject.professionalType != null &&
                subject.professionalStatus != ReviewStatus.APPROVED &&
                checklist.required.all { checklist.documentFor(it)?.status == ReviewStatus.APPROVED }
}

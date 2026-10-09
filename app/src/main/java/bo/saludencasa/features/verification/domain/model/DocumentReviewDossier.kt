package bo.saludencasa.features.verification.domain.model

import bo.saludencasa.features.profile.domain.model.UserRole

data class DocumentReviewDossier(
    val subject: DocumentReviewSubject,
    val checklist: VerificationChecklist,
) {
    val reviewableTypes: List<DocumentType>
        get() = checklist.required + listOfNotNull(DocumentType.OTHER.takeIf { checklist.documentFor(it) != null })

    // A verdict needs something to pass judgement on, so a slot the person has
    // not filled yet cannot be selected.
    val selectableTypes: List<DocumentType>
        get() = reviewableTypes.filter { checklist.documentFor(it) != null }

    // Where the viewer opens: the oldest unanswered document if there is one,
    // because that is what the administrator came to decide.
    val firstTypeToShow: DocumentType
        get() =
            selectableTypes.firstOrNull { checklist.documentFor(it)?.status == ReviewStatus.PENDING }
                ?: selectableTypes.firstOrNull()
                ?: reviewableTypes.first()

    // A prediction used to offer the button, never a decision: the engine checks
    // the same thing in approve_professional_verification and has the last word.
    val canApproveProfessional: Boolean
        get() =
            UserRole.PROFESSIONAL in subject.heldRoles &&
                subject.professionalType != null &&
                subject.professionalStatus != ReviewStatus.APPROVED &&
                checklist.required.all { checklist.documentFor(it)?.status == ReviewStatus.APPROVED }
}

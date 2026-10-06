package bo.saludencasa.features.verification.data.mapper

import bo.saludencasa.features.verification.data.model.VerificationDocumentDto
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationDocument

internal fun VerificationDocumentDto.toDocument(): VerificationDocument? {
    val type = DocumentType.entries.firstOrNull { it.name == documentType } ?: return null
    val reviewStatus = ReviewStatus.entries.firstOrNull { it.name == status } ?: return null
    return VerificationDocument(
        type = type,
        status = reviewStatus,
        storagePath = storagePath,
        caption = caption,
        rejectionReason = rejectionReason,
    )
}

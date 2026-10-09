package bo.saludencasa.features.verification.domain.model

import java.time.Instant

data class PendingDocumentReview(
    val profileId: String,
    val type: DocumentType,
    val caption: String?,
    val createdAt: Instant,
    val fullName: String,
    val email: String,
)

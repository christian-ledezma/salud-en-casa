package bo.saludencasa.features.verification.domain.model

data class VerificationDocument(
    val type: DocumentType,
    val status: ReviewStatus,
    val storagePath: String,
    val caption: String?,
    val rejectionReason: String?,
)

package bo.saludencasa.features.verification.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerificationDocumentDto(
    @SerialName("document_type") val documentType: String,
    val status: String,
    @SerialName("storage_path") val storagePath: String,
    val caption: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
)

// docs/decisions.md, 2026-09-16 · encodeDefaults. No default on caption so null
// travels as JSON null and clears the column on a fixed-type row. The same
// holds for the review fields: a resubmission must clear them (docs/decisions.md,
// 2026-10-05, HU-08).
@Serializable
data class VerificationDocumentRow(
    @SerialName("profile_id") val profileId: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("storage_path") val storagePath: String,
    val caption: String?,
    val status: String,
    @SerialName("reviewed_by") val reviewedBy: String?,
    @SerialName("reviewed_at") val reviewedAt: String?,
    @SerialName("rejection_reason") val rejectionReason: String?,
)

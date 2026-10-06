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
// travels as JSON null and clears the column on a fixed-type row.
@Serializable
data class VerificationDocumentRow(
    @SerialName("profile_id") val profileId: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("storage_path") val storagePath: String,
    val caption: String?,
)

package bo.saludencasa.features.verification.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewQueueOrder
import bo.saludencasa.features.verification.domain.model.ReviewRoleFilter
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationError

@StringRes
internal fun DocumentType.titleRes(): Int =
    when (this) {
        DocumentType.ID_FRONT -> R.string.verification_document_id_front_title
        DocumentType.ID_BACK -> R.string.verification_document_id_back_title
        DocumentType.SELFIE -> R.string.verification_document_selfie_title
        DocumentType.DEGREE -> R.string.verification_document_degree_title
        DocumentType.LICENSE -> R.string.verification_document_license_title
        DocumentType.STUDENT_CARD -> R.string.verification_document_student_card_title
        DocumentType.OTHER -> R.string.verification_document_other_title
    }

@StringRes
internal fun DocumentType.descriptionRes(): Int =
    when (this) {
        DocumentType.ID_FRONT -> R.string.verification_document_id_front_description
        DocumentType.ID_BACK -> R.string.verification_document_id_back_description
        DocumentType.SELFIE -> R.string.verification_document_selfie_description
        DocumentType.DEGREE -> R.string.verification_document_degree_description
        DocumentType.LICENSE -> R.string.verification_document_license_description
        DocumentType.STUDENT_CARD -> R.string.verification_document_student_card_description
        DocumentType.OTHER -> R.string.verification_document_other_description
    }

@StringRes
internal fun ReviewStatus.badgeRes(): Int =
    when (this) {
        ReviewStatus.PENDING -> R.string.verification_status_pending
        ReviewStatus.APPROVED -> R.string.verification_status_approved
        ReviewStatus.REJECTED -> R.string.verification_status_rejected
    }

@StringRes
internal fun ReviewRoleFilter.labelRes(): Int =
    when (this) {
        ReviewRoleFilter.ALL -> R.string.review_queue_filter_role_all
        ReviewRoleFilter.PATIENT -> R.string.review_queue_filter_role_patient
        ReviewRoleFilter.PROFESSIONAL -> R.string.review_queue_filter_role_professional
    }

@StringRes
internal fun ReviewQueueOrder.labelRes(): Int =
    when (this) {
        ReviewQueueOrder.OLDEST_FIRST -> R.string.review_queue_order_oldest
        ReviewQueueOrder.NEWEST_FIRST -> R.string.review_queue_order_newest
    }

@StringRes
internal fun VerificationError.messageRes(): Int =
    when (this) {
        VerificationError.NotSignedIn -> R.string.error_verification_not_signed_in
        VerificationError.EmptyImage -> R.string.error_verification_empty_image
        VerificationError.ImageTooLarge -> R.string.error_verification_image_too_large
        VerificationError.UnreadableImage -> R.string.error_verification_unreadable_image
        VerificationError.CaptionRequired -> R.string.error_verification_caption_required
        VerificationError.CaptionNotAllowed -> R.string.error_verification_caption_not_allowed
        VerificationError.InvalidCaption -> R.string.error_verification_invalid_caption
        VerificationError.DocumentFrozenByReview -> R.string.error_verification_document_frozen
        VerificationError.NotAuthorized -> R.string.error_verification_not_authorized
        VerificationError.RequiredDocumentsNotApproved -> R.string.error_verification_required_documents_not_approved
        VerificationError.ProfessionalProfileIncomplete -> R.string.error_verification_professional_profile_incomplete
        VerificationError.InvalidRejectionReason -> R.string.error_verification_invalid_rejection_reason
        VerificationError.NetworkUnavailable -> R.string.error_network_unavailable
        VerificationError.Unexpected -> R.string.error_unexpected
    }

package bo.saludencasa.features.verification.domain.model

sealed interface MyDocumentsResult {
    data class Loaded(
        val documents: List<VerificationDocument>,
    ) : MyDocumentsResult

    data class Failure(
        val error: VerificationError,
    ) : MyDocumentsResult
}

sealed interface VerificationChecklistResult {
    data class Loaded(
        val checklist: VerificationChecklist,
    ) : VerificationChecklistResult

    data class Failure(
        val error: VerificationError,
    ) : VerificationChecklistResult
}

sealed interface UploadDocumentResult {
    data object Success : UploadDocumentResult

    data class Failure(
        val error: VerificationError,
    ) : UploadDocumentResult
}

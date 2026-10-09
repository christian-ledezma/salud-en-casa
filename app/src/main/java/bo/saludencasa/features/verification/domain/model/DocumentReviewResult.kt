package bo.saludencasa.features.verification.domain.model

sealed interface PendingReviewsResult {
    data class Loaded(
        val reviews: List<PendingDocumentReview>,
    ) : PendingReviewsResult

    data class Failure(
        val error: VerificationError,
    ) : PendingReviewsResult
}

sealed interface PendingReviewPageResult {
    data class Loaded(
        val reviews: List<PendingDocumentReview>,
        val endReached: Boolean,
    ) : PendingReviewPageResult

    data class Failure(
        val error: VerificationError,
    ) : PendingReviewPageResult
}

sealed interface DocumentReviewSubjectResult {
    data class Loaded(
        val subject: DocumentReviewSubject,
    ) : DocumentReviewSubjectResult

    data class Failure(
        val error: VerificationError,
    ) : DocumentReviewSubjectResult
}

sealed interface DocumentReviewDossierResult {
    data class Loaded(
        val dossier: DocumentReviewDossier,
    ) : DocumentReviewDossierResult

    data class Failure(
        val error: VerificationError,
    ) : DocumentReviewDossierResult
}

sealed interface ReviewActionResult {
    data object Success : ReviewActionResult

    data class Failure(
        val error: VerificationError,
    ) : ReviewActionResult
}

sealed interface DocumentUrlResult {
    data class Loaded(
        val url: String,
    ) : DocumentUrlResult

    data class Failure(
        val error: VerificationError,
    ) : DocumentUrlResult
}

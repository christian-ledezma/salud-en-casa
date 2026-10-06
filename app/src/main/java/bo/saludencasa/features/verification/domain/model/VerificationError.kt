package bo.saludencasa.features.verification.domain.model

sealed interface VerificationError {
    data object NotSignedIn : VerificationError

    data object EmptyImage : VerificationError

    data object ImageTooLarge : VerificationError

    data object UnreadableImage : VerificationError

    data object CaptionRequired : VerificationError

    data object CaptionNotAllowed : VerificationError

    data object InvalidCaption : VerificationError

    data object DocumentFrozenByReview : VerificationError

    data object NetworkUnavailable : VerificationError

    data object Unexpected : VerificationError
}

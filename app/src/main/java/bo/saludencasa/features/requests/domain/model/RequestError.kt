package bo.saludencasa.features.requests.domain.model

// The error carries a type, never a phrase (.claude/rules/i18n.md). The
// presentation layer turns each case into a resource key.
sealed interface RequestError {
    data object NotSignedIn : RequestError

    // The chosen address does not belong to the caller. The creation function is
    // the only path, and it reads the snapshot from an address the patient owns.
    data object AddressNotOwned : RequestError

    data object ServiceTypeUnavailable : RequestError

    data object InvalidDescription : RequestError

    data object InvalidBudget : RequestError

    data object NetworkUnavailable : RequestError

    data object Unexpected : RequestError
}

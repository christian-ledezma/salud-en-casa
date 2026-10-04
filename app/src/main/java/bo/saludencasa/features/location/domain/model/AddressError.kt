package bo.saludencasa.features.location.domain.model

sealed interface AddressError {
    data object NotSignedIn : AddressError

    data object InvalidAlias : AddressError

    data object InvalidAddressText : AddressError

    data object InvalidCity : AddressError

    data object InvalidReference : AddressError

    data object PointNotChosen : AddressError

    data object GeocoderUnavailable : AddressError

    data object LocationPermissionDenied : AddressError

    data object LocationUnavailable : AddressError

    // The address being deleted is the primary one or the professional base,
    // and more than one address would survive it, so which one inherits the
    // mark is the person's choice and not the application's.
    data object SuccessorRequired : AddressError

    data object NetworkUnavailable : AddressError

    data object Unexpected : AddressError
}

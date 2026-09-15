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

    data object NetworkUnavailable : AddressError

    data object Unexpected : AddressError
}

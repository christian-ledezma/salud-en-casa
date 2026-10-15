package bo.saludencasa.features.requests.presentation

import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.requests.domain.model.RequestError

internal fun CatalogError.toRequestError(): RequestError =
    when (this) {
        CatalogError.NotSignedIn -> RequestError.NotSignedIn
        CatalogError.NetworkUnavailable -> RequestError.NetworkUnavailable
        else -> RequestError.Unexpected
    }

internal fun AddressError.toRequestError(): RequestError =
    when (this) {
        AddressError.NotSignedIn -> RequestError.NotSignedIn
        AddressError.NetworkUnavailable -> RequestError.NetworkUnavailable
        else -> RequestError.Unexpected
    }

internal fun ProfileError.toRequestError(): RequestError =
    when (this) {
        ProfileError.NotSignedIn -> RequestError.NotSignedIn
        ProfileError.NetworkUnavailable -> RequestError.NetworkUnavailable
        else -> RequestError.Unexpected
    }

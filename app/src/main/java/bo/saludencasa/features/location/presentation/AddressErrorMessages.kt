package bo.saludencasa.features.location.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.features.location.domain.model.AddressError

@StringRes
internal fun AddressError.messageRes(): Int =
    when (this) {
        AddressError.NotSignedIn -> R.string.error_address_not_signed_in
        AddressError.InvalidAlias -> R.string.error_address_invalid_alias
        AddressError.InvalidAddressText -> R.string.error_address_invalid_text
        AddressError.InvalidCity -> R.string.error_address_invalid_city
        AddressError.InvalidReference -> R.string.error_address_invalid_reference
        AddressError.PointNotChosen -> R.string.error_address_point_not_chosen
        AddressError.GeocoderUnavailable -> R.string.error_address_geocoder_unavailable
        AddressError.LocationPermissionDenied -> R.string.error_address_permission_denied
        AddressError.LocationUnavailable -> R.string.error_address_location_unavailable
        AddressError.SuccessorRequired -> R.string.error_address_successor_required
        AddressError.NetworkUnavailable -> R.string.error_network_unavailable
        AddressError.Unexpected -> R.string.error_unexpected
    }

@StringRes
internal fun AddressNotice.messageRes(): Int =
    when (this) {
        AddressNotice.PlaceNotFound -> R.string.error_address_place_not_found
        AddressNotice.PointNotNamed -> R.string.address_point_not_named
        is AddressNotice.Problem -> error.messageRes()
    }

// An error that names a field is drawn under that field; every other one is
// about the screen as a whole and goes next to the save button.
internal fun AddressError.isAboutAField(): Boolean =
    this == AddressError.InvalidAlias ||
        this == AddressError.InvalidAddressText ||
        this == AddressError.InvalidCity ||
        this == AddressError.InvalidReference

@Composable
internal fun AddressError?.messageFor(field: AddressError): String? =
    if (this == field) stringResource(messageRes()) else null

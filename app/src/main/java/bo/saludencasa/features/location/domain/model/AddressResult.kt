package bo.saludencasa.features.location.domain.model

import bo.saludencasa.core.vo.Coordinate

// Having no address is the first thing every person sees, so it travels as its
// own answer rather than as an empty success or as a failure to read.
sealed interface MyAddressResult {
    data class Registered(
        val address: Address,
    ) : MyAddressResult

    data object NotRegistered : MyAddressResult

    data class Failure(
        val error: AddressError,
    ) : MyAddressResult
}

sealed interface SaveAddressResult {
    data class Success(
        val address: Address,
    ) : SaveAddressResult

    data class Failure(
        val error: AddressError,
    ) : SaveAddressResult
}

// A geocoder that answers nothing is not a geocoder that failed. RF-03.6 asks
// the screen to keep working either way, and it keeps working differently: an
// unanswered point leaves the written address for the person to fill in, while
// a failure is something to report.
sealed interface GeocodingResult {
    data class Found(
        val place: Place,
    ) : GeocodingResult

    data object NotFound : GeocodingResult

    data class Failure(
        val error: AddressError,
    ) : GeocodingResult
}

sealed interface PositionResult {
    data class Located(
        val coordinate: Coordinate,
    ) : PositionResult

    data class Failure(
        val error: AddressError,
    ) : PositionResult
}

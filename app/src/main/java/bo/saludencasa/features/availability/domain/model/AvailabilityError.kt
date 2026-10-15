package bo.saludencasa.features.availability.domain.model

sealed interface AvailabilityError {
    data object EndNotAfterStart : AvailabilityError

    data object OverlapsExistingSlot : AvailabilityError

    data object NetworkUnavailable : AvailabilityError

    data object Unexpected : AvailabilityError
}

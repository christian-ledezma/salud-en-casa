package bo.saludencasa.features.availability.domain.model

sealed interface AvailabilityResult {
    data class Loaded(
        val availability: WeeklyAvailability,
    ) : AvailabilityResult

    data class Failure(
        val error: AvailabilityError,
    ) : AvailabilityResult
}

sealed interface SlotWriteResult {
    data object Success : SlotWriteResult

    data class Failure(
        val error: AvailabilityError,
    ) : SlotWriteResult
}

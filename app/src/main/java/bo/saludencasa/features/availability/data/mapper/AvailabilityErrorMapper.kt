package bo.saludencasa.features.availability.data.mapper

import bo.saludencasa.features.availability.domain.model.AvailabilityError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

internal fun Throwable.toAvailabilityError(): AvailabilityError {
    // Both paths raise the SQL unique-violation (errcode 23505): the UNIQUE
    // constraint for an exact duplicate, and the before-trigger that raises
    // 'availability_slot_overlaps' for a partial overlap. The trigger's
    // message is what the catalog-style match keys on; the duplicate also
    // overlaps itself, so it maps to the same user-facing error.
    val reason = message.orEmpty()
    if (OVERLAP_SIGNATURE in reason || DUPLICATE_KEY in reason) {
        return AvailabilityError.OverlapsExistingSlot
    }

    return when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> AvailabilityError.NetworkUnavailable
        else -> AvailabilityError.Unexpected
    }
}

private const val OVERLAP_SIGNATURE = "availability_slot_overlaps"
private const val DUPLICATE_KEY =
    "availability_slots_professional_id_day_of_week_start_time_end_time_key"

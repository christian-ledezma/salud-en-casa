package bo.saludencasa.features.availability.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// LocalTime leaves and reaches us as "HH:MM:SS": the engine's `time` type uses
// that textual form in PostgREST, matching what the schema's start_time /
// end_time columns already store.
@Serializable
data class AvailabilitySlotDto(
    val id: String,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
)

@Serializable
data class AvailabilitySlotRow(
    @SerialName("professional_id") val professionalId: String,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
)

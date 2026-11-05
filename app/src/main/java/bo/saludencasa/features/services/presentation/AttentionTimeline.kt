package bo.saludencasa.features.services.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.ServiceStatus
import java.time.Instant

internal enum class TimelineStepState {
    DONE,
    CURRENT,
    PENDING,
    CANCELLED,
}

internal data class TimelineStep(
    @param:StringRes val titleRes: Int,
    val state: TimelineStepState,
    val at: Instant? = null,
)

internal fun Attention.timeline(): List<TimelineStep> {
    val agreed = TimelineStep(R.string.attention_step_agreed, TimelineStepState.DONE, createdAt)
    if (status == ServiceStatus.CANCELLED) {
        return listOfNotNull(
            agreed,
            startedAt?.let { TimelineStep(R.string.attention_step_arrival, TimelineStepState.DONE, it) },
            TimelineStep(R.string.attention_step_cancelled, TimelineStepState.CANCELLED, cancellation?.at),
        )
    }
    return listOf(
        agreed,
        TimelineStep(
            titleRes = R.string.attention_step_arrival,
            state = if (startedAt != null) TimelineStepState.DONE else TimelineStepState.CURRENT,
            at = startedAt,
        ),
        TimelineStep(
            titleRes = R.string.attention_step_completed,
            state =
                when {
                    finishedAt != null -> TimelineStepState.DONE
                    startedAt != null -> TimelineStepState.CURRENT
                    else -> TimelineStepState.PENDING
                },
            at = finishedAt,
        ),
    )
}

// The geo scheme takes latitude first, and a query makes the application drop a pin
// on the point instead of only centering there.
internal fun directionsUri(
    latitude: Double,
    longitude: Double,
): String = "geo:$latitude,$longitude?q=$latitude,$longitude"

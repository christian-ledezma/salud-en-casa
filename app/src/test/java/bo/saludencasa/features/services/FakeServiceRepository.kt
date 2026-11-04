package bo.saludencasa.features.services

import bo.saludencasa.features.services.domain.model.AdvanceAttentionResult
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionPlace
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.AttentionSummary
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.MyAttentionsResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigDecimal
import java.time.Instant

class FakeServiceRepository : IServiceRepository {
    var result: AttentionResult = AttentionResult.NotFound
    var attentions: List<AttentionSummary> = emptyList()
    var attentionsError: ServiceError? = null
    var advanceError: ServiceError? = null
    var holdAdvance: CompletableDeferred<Unit>? = null
    val advances = mutableListOf<AttentionStep>()
    val signals = MutableSharedFlow<Unit>()

    override suspend fun getAttention(requestId: String): AttentionResult = result

    override suspend fun getMyAttentions(
        offset: Int,
        limit: Int,
    ): MyAttentionsResult =
        attentionsError?.let(MyAttentionsResult::Failure)
            ?: MyAttentionsResult.Loaded(attentions.drop(offset).take(limit))

    // The engine moves the state and stamps its date, so the reread finds them.
    override suspend fun advance(
        requestId: String,
        step: AttentionStep,
    ): AdvanceAttentionResult {
        advances += step
        holdAdvance?.await()
        advanceError?.let { return AdvanceAttentionResult.Failure(it) }
        val current = (result as? AttentionResult.Loaded)?.attention
        if (current != null) {
            val at = Instant.parse("2026-11-04T14:00:00Z")
            result =
                AttentionResult.Loaded(
                    when (step) {
                        AttentionStep.MarkArrival -> current.copy(status = step.targetStatus, startedAt = at)
                        AttentionStep.MarkCompleted -> current.copy(status = step.targetStatus, finishedAt = at)
                    },
                )
        }
        return AdvanceAttentionResult.Advanced
    }

    override fun attentionSignals(requestId: String): Flow<Unit> = signals
}

fun attentionSummary(
    requestId: String = "r1",
    status: ServiceStatus = ServiceStatus.ASSIGNED,
    scheduledAt: Instant? = null,
): AttentionSummary =
    AttentionSummary(
        requestId = requestId,
        status = status,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        patientName = "María Quispe",
        createdAt = Instant.parse("2026-10-29T12:00:00Z"),
        scheduledAt = scheduledAt,
    )

fun attention(
    requestId: String = "r1",
    status: ServiceStatus = ServiceStatus.ASSIGNED,
    iAmThePatient: Boolean = true,
    counterpartId: String = "p1",
    place: AttentionPlace = attentionPlace(),
): Attention =
    Attention(
        serviceId = "s1",
        requestId = requestId,
        status = status,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        counterpart =
            Counterpart(
                id = counterpartId,
                name = "María Quispe",
                photoUrl = null,
            ),
        iAmThePatient = iAmThePatient,
        createdAt = Instant.parse("2026-10-29T12:00:00Z"),
        scheduledAt = null,
        startedAt = null,
        finishedAt = null,
        description = "Curación de una herida en la pierna derecha.",
        place = place,
    )

fun attentionPlace(
    addressText: String? = "Av. Arce 2631",
    latitude: Double? = -16.5103,
    longitude: Double? = -68.1218,
): AttentionPlace =
    AttentionPlace(
        city = "La Paz",
        addressText = addressText,
        reference = addressText?.let { "Tercer piso" },
        latitude = latitude,
        longitude = longitude,
    )

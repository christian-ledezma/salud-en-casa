package bo.saludencasa.features.services

import bo.saludencasa.features.services.domain.model.ActiveAttentionResult
import bo.saludencasa.features.services.domain.model.AdvanceAttentionResult
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionCancellation
import bo.saludencasa.features.services.domain.model.AttentionPlace
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.AttentionSummary
import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.CancelAttentionResult
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.MyAttentionsResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import bo.saludencasa.features.services.domain.vo.CancellationReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigDecimal
import java.time.Instant

class FakeServiceRepository : IServiceRepository {
    var result: AttentionResult = AttentionResult.NotFound
    var attentions: List<AttentionSummary> = emptyList()
    var attentionsError: ServiceError? = null
    var activeAttention: ActiveAttentionResult = ActiveAttentionResult.None
    val sidesRead = mutableListOf<AttentionSide>()
    var advanceError: ServiceError? = null
    var holdAdvance: CompletableDeferred<Unit>? = null
    val advances = mutableListOf<AttentionStep>()
    var cancelError: ServiceError? = null
    var holdCancel: CompletableDeferred<Unit>? = null
    val cancels = mutableListOf<String>()
    val signals = MutableSharedFlow<Unit>()

    override suspend fun getAttention(requestId: String): AttentionResult = result

    override suspend fun getMyAttentions(
        side: AttentionSide,
        offset: Int,
        limit: Int,
    ): MyAttentionsResult {
        sidesRead += side
        return attentionsError?.let(MyAttentionsResult::Failure)
            ?: MyAttentionsResult.Loaded(attentions.drop(offset).take(limit))
    }

    override suspend fun getActiveAttention(side: AttentionSide): ActiveAttentionResult {
        sidesRead += side
        return activeAttention
    }

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

    override suspend fun cancel(
        requestId: String,
        reason: CancellationReason,
    ): CancelAttentionResult {
        cancels += reason.value
        holdCancel?.await()
        cancelError?.let { return CancelAttentionResult.Failure(CancelAttentionError.Service(it)) }
        val current = (result as? AttentionResult.Loaded)?.attention
        if (current != null) {
            result =
                AttentionResult.Loaded(
                    current.copy(
                        status = ServiceStatus.CANCELLED,
                        conversationOpen = false,
                        cancellation =
                            AttentionCancellation(
                                reason.value,
                                byMe = true,
                                at = Instant.parse("2026-11-04T14:00:00Z"),
                            ),
                    ),
                )
        }
        return CancelAttentionResult.Cancelled
    }

    override fun attentionSignals(requestId: String): Flow<Unit> = signals
}

fun attentionSummary(
    requestId: String = "r1",
    status: ServiceStatus = ServiceStatus.ASSIGNED,
    scheduledAt: Instant? = null,
    startedAt: Instant? = null,
): AttentionSummary =
    AttentionSummary(
        requestId = requestId,
        status = status,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        counterpartName = "María Quispe",
        createdAt = Instant.parse("2026-10-29T12:00:00Z"),
        scheduledAt = scheduledAt,
        startedAt = startedAt,
    )

fun attention(
    requestId: String = "r1",
    status: ServiceStatus = ServiceStatus.ASSIGNED,
    iAmThePatient: Boolean = true,
    counterpartId: String = "p1",
    place: AttentionPlace = attentionPlace(),
    conversationOpen: Boolean = status != ServiceStatus.CANCELLED,
    conversationWritableUntil: Instant? = null,
    cancellation: AttentionCancellation? = null,
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
        conversationOpen = conversationOpen,
        conversationWritableUntil = conversationWritableUntil,
        cancellation = cancellation,
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

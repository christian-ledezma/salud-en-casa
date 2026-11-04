package bo.saludencasa.features.services

import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionSummary
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.MyAttentionsResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import java.math.BigDecimal
import java.time.Instant

class FakeServiceRepository : IServiceRepository {
    var result: AttentionResult = AttentionResult.NotFound
    var attentions: List<AttentionSummary> = emptyList()
    var attentionsError: ServiceError? = null

    override suspend fun getAttention(requestId: String): AttentionResult = result

    override suspend fun getMyAttentions(
        offset: Int,
        limit: Int,
    ): MyAttentionsResult =
        attentionsError?.let(MyAttentionsResult::Failure)
            ?: MyAttentionsResult.Loaded(attentions.drop(offset).take(limit))
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
    )

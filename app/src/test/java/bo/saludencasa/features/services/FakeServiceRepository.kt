package bo.saludencasa.features.services

import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import java.math.BigDecimal
import java.time.Instant

class FakeServiceRepository : IServiceRepository {
    var result: AttentionResult = AttentionResult.NotFound

    override suspend fun getAttention(requestId: String): AttentionResult = result
}

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

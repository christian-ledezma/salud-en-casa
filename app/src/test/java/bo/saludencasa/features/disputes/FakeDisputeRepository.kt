package bo.saludencasa.features.disputes

import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.model.DisputeParty
import bo.saludencasa.features.disputes.domain.model.DisputeResolution
import bo.saludencasa.features.disputes.domain.model.DisputeResult
import bo.saludencasa.features.disputes.domain.model.DisputesResult
import bo.saludencasa.features.disputes.domain.model.PaymentDispute
import bo.saludencasa.features.disputes.domain.model.ResolveDisputeResult
import bo.saludencasa.features.disputes.domain.repository.IDisputeRepository
import bo.saludencasa.features.services.domain.model.PaymentMethod
import kotlinx.coroutines.CompletableDeferred
import java.math.BigDecimal
import java.time.Instant

class FakeDisputeRepository : IDisputeRepository {
    var disputes: List<PaymentDispute> = emptyList()
    var listError: DisputeError? = null
    var readError: DisputeError? = null
    var resolveError: DisputeError? = null
    var holdResolve: CompletableDeferred<Unit>? = null
    val resolutions = mutableListOf<Pair<String, DisputeResolution>>()

    override suspend fun getDisputes(
        offset: Int,
        limit: Int,
    ): DisputesResult =
        listError?.let(DisputesResult::Failure) ?: DisputesResult.Loaded(disputes.drop(offset).take(limit))

    override suspend fun getDispute(paymentId: String): DisputeResult {
        readError?.let { return DisputeResult.Failure(it) }
        return disputes.firstOrNull { it.paymentId == paymentId }?.let(DisputeResult::Loaded) ?: DisputeResult.NotFound
    }

    // A resolved payment leaves the queue, as payment_dispute_queue filters DISPUTED.
    override suspend fun resolve(
        paymentId: String,
        resolution: DisputeResolution,
    ): ResolveDisputeResult {
        resolutions += paymentId to resolution
        holdResolve?.await()
        resolveError?.let { return ResolveDisputeResult.Failure(it) }
        disputes = disputes.filterNot { it.paymentId == paymentId }
        return ResolveDisputeResult.Resolved
    }
}

fun paymentDispute(
    paymentId: String = "p1",
    disputedAt: Instant = Instant.parse("2026-11-23T14:15:00Z"),
): PaymentDispute =
    PaymentDispute(
        paymentId = paymentId,
        requestId = "r-$paymentId",
        serviceTypeName = "Aplicación de inyectables",
        amountBob = BigDecimal("45.00"),
        method = PaymentMethod.DIRECT_QR,
        finishedAt = Instant.parse("2026-11-23T13:38:00Z"),
        patientConfirmedAt = Instant.parse("2026-11-23T13:40:00Z"),
        disputedAt = disputedAt,
        patient = DisputeParty("Ana Quispe", "ana.quispe@ejemplo.com"),
        professional = DisputeParty("María Flores", "maria.flores@ejemplo.com"),
    )

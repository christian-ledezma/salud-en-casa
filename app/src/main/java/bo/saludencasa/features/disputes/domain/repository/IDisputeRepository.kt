package bo.saludencasa.features.disputes.domain.repository

import bo.saludencasa.features.disputes.domain.model.DisputeResolution
import bo.saludencasa.features.disputes.domain.model.DisputeResult
import bo.saludencasa.features.disputes.domain.model.DisputesResult
import bo.saludencasa.features.disputes.domain.model.ResolveDisputeResult

interface IDisputeRepository {
    // RF-11.7: oldest first, so the longest wait is resolved first.
    suspend fun getDisputes(
        offset: Int,
        limit: Int,
    ): DisputesResult

    suspend fun getDispute(paymentId: String): DisputeResult

    // The engine seals who and when.
    suspend fun resolve(
        paymentId: String,
        resolution: DisputeResolution,
    ): ResolveDisputeResult
}

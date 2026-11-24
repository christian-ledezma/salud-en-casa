package bo.saludencasa.features.disputes.data.repository

import bo.saludencasa.features.disputes.data.datasource.SupabaseDisputeDataSource
import bo.saludencasa.features.disputes.data.mapper.toDisputeError
import bo.saludencasa.features.disputes.data.mapper.toPaymentDispute
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.model.DisputeResolution
import bo.saludencasa.features.disputes.domain.model.DisputeResult
import bo.saludencasa.features.disputes.domain.model.DisputesResult
import bo.saludencasa.features.disputes.domain.model.ResolveDisputeResult
import bo.saludencasa.features.disputes.domain.repository.IDisputeRepository
import kotlinx.coroutines.CancellationException

class DisputeRepository(
    private val dataSource: SupabaseDisputeDataSource,
) : IDisputeRepository {
    override suspend fun getDisputes(
        offset: Int,
        limit: Int,
    ): DisputesResult {
        if (dataSource.currentUserId() == null) {
            return DisputesResult.Failure(DisputeError.NotSignedIn)
        }

        return try {
            val disputes =
                dataSource.fetchDisputes(offset.toLong(), limit.toLong()).map { dto ->
                    dto.toPaymentDispute() ?: return DisputesResult.Failure(DisputeError.Unexpected)
                }
            DisputesResult.Loaded(disputes)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            DisputesResult.Failure(failure.toDisputeError())
        }
    }

    override suspend fun getDispute(paymentId: String): DisputeResult {
        if (dataSource.currentUserId() == null) {
            return DisputeResult.Failure(DisputeError.NotSignedIn)
        }

        return try {
            val dto = dataSource.fetchDispute(paymentId) ?: return DisputeResult.NotFound
            dto.toPaymentDispute()?.let(DisputeResult::Loaded) ?: DisputeResult.Failure(DisputeError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            DisputeResult.Failure(failure.toDisputeError())
        }
    }

    override suspend fun resolve(
        paymentId: String,
        resolution: DisputeResolution,
    ): ResolveDisputeResult {
        if (dataSource.currentUserId() == null) {
            return ResolveDisputeResult.Failure(DisputeError.NotSignedIn)
        }
        val status =
            when (resolution) {
                DisputeResolution.PAID -> RESOLVED_PAID
                DisputeResolution.NOT_PAID -> RESOLVED_NOT_PAID
            }

        return try {
            if (dataSource.resolve(paymentId, status) == 0) {
                ResolveDisputeResult.Failure(DisputeError.AlreadyResolved)
            } else {
                ResolveDisputeResult.Resolved
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ResolveDisputeResult.Failure(failure.toDisputeError())
        }
    }

    private companion object {
        const val RESOLVED_PAID = "RESOLVED_PAID"
        const val RESOLVED_NOT_PAID = "RESOLVED_NOT_PAID"
    }
}

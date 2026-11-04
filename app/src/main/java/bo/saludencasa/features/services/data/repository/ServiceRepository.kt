package bo.saludencasa.features.services.data.repository

import bo.saludencasa.features.services.data.datasource.SupabaseServiceDataSource
import bo.saludencasa.features.services.data.mapper.toAttention
import bo.saludencasa.features.services.data.mapper.toAttentionSummary
import bo.saludencasa.features.services.data.mapper.toServiceError
import bo.saludencasa.features.services.domain.model.ActiveAttentionResult
import bo.saludencasa.features.services.domain.model.AdvanceAttentionResult
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.MyAttentionsResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ServiceRepository(
    private val dataSource: SupabaseServiceDataSource,
) : IServiceRepository {
    override suspend fun getAttention(requestId: String): AttentionResult {
        if (dataSource.currentUserId() == null) {
            return AttentionResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            val dto = dataSource.fetchAttention(requestId) ?: return AttentionResult.NotFound
            dto.toAttention()?.let(AttentionResult::Loaded)
                ?: AttentionResult.Failure(ServiceError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AttentionResult.Failure(failure.toServiceError())
        }
    }

    override suspend fun getMyAttentions(
        side: AttentionSide,
        offset: Int,
        limit: Int,
    ): MyAttentionsResult {
        if (dataSource.currentUserId() == null) {
            return MyAttentionsResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            val attentions =
                dataSource.fetchMyAttentions(side.name, offset, limit).map { dto ->
                    dto.toAttentionSummary() ?: return MyAttentionsResult.Failure(ServiceError.Unexpected)
                }
            MyAttentionsResult.Loaded(attentions)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            MyAttentionsResult.Failure(failure.toServiceError())
        }
    }

    override suspend fun getActiveAttention(side: AttentionSide): ActiveAttentionResult {
        if (dataSource.currentUserId() == null) {
            return ActiveAttentionResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            val dto = dataSource.fetchActiveAttention(side.name) ?: return ActiveAttentionResult.None
            dto.toAttentionSummary()?.let(ActiveAttentionResult::Found)
                ?: ActiveAttentionResult.Failure(ServiceError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ActiveAttentionResult.Failure(failure.toServiceError())
        }
    }

    override suspend fun advance(
        requestId: String,
        step: AttentionStep,
    ): AdvanceAttentionResult {
        if (dataSource.currentUserId() == null) {
            return AdvanceAttentionResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            if (dataSource.updateStatus(requestId, step.targetStatus.name) == 0) {
                AdvanceAttentionResult.Failure(ServiceError.Unexpected)
            } else {
                AdvanceAttentionResult.Advanced
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AdvanceAttentionResult.Failure(failure.toServiceError())
        }
    }

    override fun attentionSignals(requestId: String): Flow<Unit> {
        if (dataSource.currentUserId() == null) return emptyFlow()
        return dataSource.signals(requestId)
    }
}
